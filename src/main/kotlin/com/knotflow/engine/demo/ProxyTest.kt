@file:Suppress("unused")

package com.knotflow.engine.demo

import com.knotflow.engine.core.Step
import com.knotflow.engine.core.Workflow
import net.bytebuddy.ByteBuddy
import net.bytebuddy.NamingStrategy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import java.io.File
import java.lang.reflect.InvocationHandler


@Workflow
interface WorkflowBase1 {
    @Step
    fun hello() {
        println("Method ${::hello.name} executed")
    }
}

@Workflow
interface WorkflowExample1 : WorkflowBase1 {
    var foo: String;

    @Step
    fun init() {
        println("Method ${::init.name} executed")
    }

    @Step
    fun input(): String {
        // println("User Input:")
        // return readLine().orEmpty().trim().toUpperCase()
        println("Method ${::input.name} executed")
        return "B"
    }

    @Step
    fun optionA() {
        println("Method ${::optionA.name} executed")
    }

    @Step
    fun optionB() {
        println("Method ${::optionB.name} executed")
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

interface HandlerSetter {
    var handler: InvocationHandler?
}

open class WorkflowBase {
    @JvmField
    var handler: InvocationHandler? = null
}

fun main() {
    val dbf = File("sessions.db").canonicalPath

    val workflowClazz = WorkflowExample1::class.java
    val methodsClazz = Class.forName("${workflowClazz.name}\$DefaultImpls")

    val handler = InvocationHandler { proxy, method, args ->
        val paramTypes = arrayOf(workflowClazz, *method.parameterTypes)
        val implMethod = methodsClazz.getMethod(method.name, *paramTypes)

        println("Entering method: ${implMethod.name}")

        val params = arrayOf(proxy, *args)
        val result = implMethod.invoke(null, *params)

        println("Leaving method: ${implMethod.name}")
        result
    }

    val buddy = ByteBuddy()
        .with(
            NamingStrategy.SuffixingRandom(
                "KnotFlow",
                NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType(
                    TypeDescription.ForLoadedType(workflowClazz)
                )
            )
        )

    val workflowProxyClass = buddy
        .subclass(WorkflowBase::class.java)

//        .defineField("handler", InvocationHandler::class.java, Visibility.PUBLIC)
//
//        .implement(HandlerSetter::class.java)
//        .intercept(FieldAccessor.ofField("handler"))

        .implement(workflowClazz)
        .method(
            ElementMatchers.isDeclaredBy(
                ElementMatchers.isSuperTypeOf(workflowClazz)
            )
        )
        .intercept(InvocationHandlerAdapter.toField("handler"))
        .make()
        .load(workflowClazz.classLoader)
        .loaded

    val wfb = workflowProxyClass.newInstance()
    // val hs = wfb as HandlerSetter
    wfb.handler = handler

    val t1 = workflowClazz.cast(wfb)
    println(t1.example1())
}