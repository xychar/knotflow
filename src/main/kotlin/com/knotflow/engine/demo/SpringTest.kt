@file:Suppress("unused")

package com.knotflow.engine.demo

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.jvm.javaMethod

@Component
class TestService1() {
    init {
        println("TestService1.constructor")
    }

    fun hello1() {
        println(::hello1.javaMethod?.name)
    }
}

@Component
class TestService2() {
    init {
        println("TestService2.constructor")
    }

    fun hello2() {
        println(::hello2.javaMethod?.name)
    }
}

fun main() {
    val context = AnnotationConfigApplicationContext()

    val packagesToScan = arrayOf(
        "com.knotflow.engine.core",
        "com.knotflow.engine.demo",
    )

    context.scan(*packagesToScan)
    context.refresh()
    context.start()

    val ts2 = context.getBean(TestService2::class.java)
    val ts1 = context.getBean(TestService1::class.java)

    ts2.hello2()
    ts1.hello1()

    context.stop()
    context.close()
}