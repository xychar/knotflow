@file:Suppress("unused")

package com.codeflow.example

import net.bytebuddy.ByteBuddy
import net.bytebuddy.NamingStrategy
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass

interface WorkflowSession<T> {
    val workflow: T
}

class StepHandler(val controller: WorkflowController) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        val paramTypes = arrayOf(controller.workflowClass, *method.parameterTypes)
        val implMethod = controller.workflowImplClass.getMethod(method.name, *paramTypes)

        val methodName = "${method.declaringClass.simpleName}.${implMethod.name}"
        println("Entering method: $methodName")

        val params = arrayOf(proxy, *args)
        val result = implMethod.invoke(null, *params)

        println("Leaving method: $methodName")
        return result
    }
}

open class WorkflowSessionBase<T> : WorkflowSession<T> {
    @JvmField
    var handler: StepHandler? = null

    override val workflow: T
        @Suppress("UNCHECKED_CAST")
        get() = this as T
}

class WorkflowController private constructor() {
    lateinit var workflowClass: Class<*>
    lateinit var workflowImplClass: Class<*>
    lateinit var workflowProxyClass: Class<out WorkflowSessionBase<*>>

    fun <T> newInstance(): WorkflowSessionBase<T> {
        val session = workflowProxyClass.getConstructor().newInstance()
        session.handler = StepHandler(this)

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

        fun buildFrom(workflowClazz: KClass<*>): WorkflowController {
            val controller = WorkflowController()

            controller.workflowClass = workflowClazz.java

            // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
            val implClassName = "${workflowClazz.java.name}\$DefaultImpls"
            controller.workflowImplClass = Class.forName(implClassName)

            val buddy = newByteBuddy(workflowClazz)

            controller.workflowProxyClass = buddy
                .subclass(WorkflowSessionBase::class.java)
                .implement(workflowClazz.java)
                .method(
                    ElementMatchers.isDeclaredBy(
                        ElementMatchers.isSuperTypeOf(workflowClazz.java)
                    )
                )
                .intercept(InvocationHandlerAdapter.toField("handler"))
                .make()
                .load(workflowClazz.java.classLoader)
                .loaded

            return controller
        }
    }
}