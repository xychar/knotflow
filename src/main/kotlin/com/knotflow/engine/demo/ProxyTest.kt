package com.knotflow.engine.demo

import com.knotflow.engine.core.Workflow
import com.knotflow.engine.core.WorkflowStep
import net.bytebuddy.ByteBuddy
import net.bytebuddy.NamingStrategy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.InvocationHandler

@Workflow
interface WorkflowExample1 {
    @WorkflowStep
    fun init() {
        println("Method ${::init.name} executed")
    }

    @WorkflowStep
    fun input(): String {
        println("User Input:")
        return readLine().orEmpty().trim().toUpperCase()
    }

    @WorkflowStep
    fun optionA() {
        println("Method ${::optionA.name} executed")
    }

    @WorkflowStep
    fun optionB() {
        println("Method ${::optionB.name} executed")
    }

    @WorkflowStep
    fun example1() {
        init()

        val data = input()
        if (data == "A") {
            optionA()
        } else {
            optionB()
        }
    }
}

interface HandlerSetter {
    var handler: InvocationHandler?
}

fun main() {
    val workflowClazz = WorkflowExample1::class.java
    val methodsClazz = Class.forName("${workflowClazz.name}\$DefaultImpls")

    val handler = InvocationHandler { proxy, method, args ->
        var paramTypes = listOf(workflowClazz) + listOf(*method.parameterTypes)
        val implMethod = methodsClazz.getMethod(method.name, *paramTypes.toTypedArray())

        println("Entering method: ${implMethod.name}")

        var params = listOf(proxy) + listOf(*args)
        val result = implMethod.invoke(null, *params.toTypedArray())

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

    val dynamicType = buddy
        .subclass(Any::class.java)
        .implement(workflowClazz)
        .defineField("handler", InvocationHandler::class.java, Visibility.PUBLIC)
        .implement(HandlerSetter::class.java)
        .intercept(FieldAccessor.ofField("handler"))
        .method(ElementMatchers.isDeclaredBy(workflowClazz))
        .intercept(InvocationHandlerAdapter.toField("handler"))
        .make()
        .load(workflowClazz.classLoader)
        .loaded

    val t1 = workflowClazz.cast(dynamicType.getConstructor().newInstance())

    val hs = t1 as HandlerSetter
    hs.handler = handler

    println(t1.example1())
}