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
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
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

data class FlowId(val id: String, val name: String) : Comparable<FlowId> {
    override fun compareTo(other: FlowId) = compareValuesBy(this, other, { it.id }, { it.name })
}

object Flows : Table() {
    val id = varchar("id", 50).index()
    val name = varchar("name", 200).index()
    val age = integer("age")

    override val primaryKey = PrimaryKey(id, name)
}

object Tasks : Table() {
    val id = varchar("id", 50).index()
    val name = varchar("name", 200)

    override val primaryKey = PrimaryKey(id, name)
}



class Flow(id: EntityID<FlowId>) : Entity<FlowId>(id) {
    //companion object : IntEntityClass<User>(Users)

    var name by Flows.name
    var age by Flows.age
}

//class City(id: EntityID<Int>) : IntEntity(id) {
//    companion object : IntEntityClass<City>(Cities)
//
//    var name by Cities.name
//    val users by User referrersOn Users.city
//}

fun main() {
    val dbf = File("test1.db").canonicalPath
    val db = Database.connect("jdbc:sqlite:$dbf", "org.sqlite.JDBC")

    transaction(db) {
        SchemaUtils.create(Flows, Tasks)
    }

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
        .subclass(Any::class.java)
        .implement(workflowClazz)
        .defineField("handler", InvocationHandler::class.java, Visibility.PUBLIC)
        .implement(HandlerSetter::class.java)
        .intercept(FieldAccessor.ofField("handler"))
        .method(
            ElementMatchers.isDeclaredBy(
                ElementMatchers.isSuperTypeOf(workflowClazz)
            )
        )
        .intercept(InvocationHandlerAdapter.toField("handler"))
        .make()
        .load(workflowClazz.classLoader)
        .loaded

    val t1 = workflowClazz.cast(workflowProxyClass.getConstructor().newInstance())

    val hs = t1 as HandlerSetter
    hs.handler = handler

    println(t1.example1())
}