@file:Suppress("unused")

package com.codeflow.example

import net.bytebuddy.ByteBuddy
import net.bytebuddy.NamingStrategy
import net.bytebuddy.description.ByteCodeElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

enum class StepState {
    /**
     * The initial state of a step.
     *
     * Non-workflow-step function always return Undefined.
     */
    Undefined,

    /**
     * The step is called asynchronously, but not really started.
     */
    Scheduled,

    /**
     * The step function is executing.
     */
    Executing,

    /**
     * The step is retrying after first failure.
     */
    Retrying,

    /**
     * The external step is waiting for external events.
     */
    Waiting,

    /**
     * The step is failed, the whole workflow will be stopped.
     */
    Failed,

    /**
     * The step is finished successfully.
     */
    Done,
}

class ExecutionHandler(val controller: WorkflowController) : InvocationHandler {
    var sessionId: String = "-"

    var lifecycle: WorkflowLifecycle = WorkflowLifecycle.Initial

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        val paramTypes = arrayOf(controller.workflowClass, *method.parameterTypes)
        val implMethod = controller.workflowImplClass.getMethod(method.name, *paramTypes)

        val methodName = "${method.declaringClass.simpleName}.${implMethod.name}"
        println("Entering method: $methodName")

        val result = implMethod.invoke(null, proxy, *args)

        println("Leaving method: $methodName")
        return result
    }
}

enum class WorkflowLifecycle {
    /**
     * The initial state of a workflow.
     */
    Initial,

    /**
     * Workflow is in preparing.
     */
    Preparing,

    /**
     * Workflow is in executing.
     */
    Executing,

    /**
     * Workflow is recycling.
     *
     * A recycling workflow cannot be executed again.
     *
     * Workflow goes to this state only if recycle is called.
     */
    Recycling,
}

interface WorkflowSession<T> {
    val workflow: T

    /**
     * The instance id will be used to load or persist workflow states.
     */
    val sessionId: String

    val lifecycle: WorkflowLifecycle
}

open class WorkflowSessionBase<T> : WorkflowSession<T> {
    @JvmField
    var jvmHandler: ExecutionHandler? = null

    var handler: ExecutionHandler
        get() = this.jvmHandler!!
        set(value) {
            this.jvmHandler = value
        }

    override val workflow: T
        @Suppress("UNCHECKED_CAST")
        get() = this as T

    override val sessionId: String
        get() = handler.sessionId

    override val lifecycle: WorkflowLifecycle
        get() = handler.lifecycle
}

/**
 * Check step state for async step call.
 *
 * This interface is implemented by the engine.
 */
interface StepAccessor {
    /**
     * Check if the step is finished successfully.
     */
    fun isDone(step: KFunction<Any>, stepKey: Any): Boolean

    /**
     * Check if the step is already failed.
     */
    fun isFailed(step: KFunction<Any>, stepKey: Any): Boolean

    /**
     * Check if the step is started.
     *
     * Async call will change the step to started.
     */
    fun isStarted(step: KFunction<Any>, stepKey: Any): Boolean

    /**
     * Get the step state.
     */
    fun stepState(step: KFunction<Any>, stepKey: Any): StepState

    /**
     * Get the last error thrown by the step, stacktrace not included.
     */
    fun lastError(step: KFunction<Any>, stepKey: Any): Throwable

    /**
     * Get the start time of the step
     * @return null if not started
     */
    fun stepStartTime(step: KFunction<Any>, stepKey: Any): Instant?

    /**
     * Get the end time of the step
     * @return null if not finished
     */
    fun stepEndTime(step: KFunction<Any>, stepKey: Any): Instant?
}

data class StepStateKey(
    val instanceId: String,
    val stepName: String,
    val stepKey: String,
)

data class StepStateData(
    val stateKey: StepStateKey,
    var parameters: String? = null,
    var stepResult: String? = null,
    var stepState: String? = null,
    var lastError: String? = null,
)

interface StepStateStore {
    fun loadState(key: StepStateKey): StepStateData
    fun updateLastError(key: StepStateKey, error: String?)
    fun updateState(key: StepStateKey, state: String?)
    fun saveState(state: StepStateData)
}

class WorkflowController private constructor() {
    lateinit var workflowClass: Class<*>
    lateinit var workflowImplClass: Class<*>

    lateinit var workflowProxyClass: Class<out WorkflowSessionBase<*>>

    lateinit var stepStateStore: StepStateStore

    fun <T> newInstance(): WorkflowSessionBase<T> {
        val session = workflowProxyClass.getConstructor().newInstance()
        session.handler = ExecutionHandler(this)

        @Suppress("UNCHECKED_CAST")
        return session as WorkflowSessionBase<T>
    }

    companion object {
        private fun newByteBuddy(clazz: KClass<*>): ByteBuddy {
            return ByteBuddy().with(
                NamingStrategy.SuffixingRandom(
                    "KnotFlow",
                    NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType(
                        TypeDescription.ForLoadedType(clazz.java)
                    )
                )
            )
        }

        /**
         * Only intercept methods
         */
        private fun methodFilter(clazz: KClass<*>): ElementMatcher<ByteCodeElement> {
            return ElementMatchers.isDeclaredBy(
                ElementMatchers.isSuperTypeOf<TypeDescription>(clazz.java)
                    .and(ElementMatchers.isInterface())
            )
        }

        fun buildFrom(workflowClazz: KClass<*>): WorkflowController {
            val controller = WorkflowController()

            controller.workflowClass = workflowClazz.java

            // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
            val implClassName = workflowClazz.java.name + '$' + "DefaultImpls"
            controller.workflowImplClass = Class.forName(implClassName)

            val buddy = newByteBuddy(workflowClazz)

            controller.workflowProxyClass = buddy
                .subclass(WorkflowSessionBase::class.java)
                .implement(workflowClazz.java)
                .method(methodFilter(workflowClazz))
                .intercept(InvocationHandlerAdapter.toField("jvmHandler"))
                .make()
                .load(workflowClazz.java.classLoader)
                .loaded

            return controller
        }
    }
}