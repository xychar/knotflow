@file:Suppress("unused")

package com.knotflow.engine.demo

import com.knotflow.engine.core.Step
import com.knotflow.engine.core.Workflow
import net.bytebuddy.ByteBuddy
import net.bytebuddy.NamingStrategy
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass


@Workflow
interface WorkflowBase1 {
    @Step
    fun hello() {
        println("*** Method ${::hello.name} executed")
    }
}

@Workflow
interface WorkflowExample1 : WorkflowBase1 {
    var foo: String

    @Step
    fun init() {
        println("*** Method ${::init.name} executed")
    }

    @Step
    fun input(): String {
//        println("User Input:")
//        return readLine().orEmpty().trim().toUpperCase()
        println("*** Method ${::input.name} executed")
        return "A"
    }

    @Step
    fun optionA() {
        println("*** Method ${::optionA.name} executed")
    }

    @Step
    fun optionB() {
        println("*** Method ${::optionB.name} executed")
    }

    @Step
    fun example1(): String {
        init()

        val data = input()
        if (data == "A") {
            optionA()
        } else {
            optionB()
        }

        hello()
        return data
    }
}

interface WorkflowSession<T> {
    val workflow: T
}

open class WorkflowSessionBase<T> : WorkflowSession<T> {
    override val workflow: T
        @Suppress("UNCHECKED_CAST")
        get() = this as T
}

class WorkflowFactory private constructor() : InvocationHandler {
    lateinit var workflowClass: Class<*>
    lateinit var workflowImplClass: Class<*>
    lateinit var workflowProxyClass: Class<out WorkflowSessionBase<*>>

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        val paramTypes = arrayOf(workflowClass, *method.parameterTypes)
        val implMethod = workflowImplClass.getMethod(method.name, *paramTypes)

        // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
        val methodName = "${method.declaringClass.simpleName}.${implMethod.name}"
        println("Entering method: $methodName")

        val params = arrayOf(proxy, *args)
        val result = implMethod.invoke(null, *params)

        println("Leaving method: $methodName")
        return result
    }

    fun newInstance(): WorkflowSessionBase<*> {
        return workflowProxyClass.newInstance()
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

        fun of(workflowClazz: KClass<*>): WorkflowFactory {
            val factory = WorkflowFactory()
            factory.workflowClass = workflowClazz.java

            val implClassName = "${workflowClazz.java.name}\$DefaultImpls"
            factory.workflowImplClass = Class.forName(implClassName)

            val buddy = newByteBuddy(workflowClazz)
            factory.workflowProxyClass = buddy
                .subclass(WorkflowSessionBase::class.java)
                .implement(workflowClazz.java)
                .method(
                    ElementMatchers.isDeclaredBy(
                        ElementMatchers.isSuperTypeOf(workflowClazz.java)
                    )
                )
                .intercept(InvocationHandlerAdapter.of(factory))
                .make()
                .load(workflowClazz.java.classLoader)
                .loaded

            return factory
        }
    }
}

fun main() {
    val factory = WorkflowFactory.of(WorkflowExample1::class)
    val wfb = factory.newInstance()

    val t1 = wfb as WorkflowExample1
    println(t1.example1())
}