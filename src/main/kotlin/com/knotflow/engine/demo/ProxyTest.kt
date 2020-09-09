@file:Suppress("unused")

package com.knotflow.engine.demo

import com.knotflow.engine.core.Step
import com.knotflow.engine.core.Workflow
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.TransactionIsolation
import me.liuwj.ktorm.schema.*
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

object DbSchema {
    val t_flow = """
        create table if not exists t_flow(
            id int not null,
            name varchar(128) not null,
            location varchar(128) not null,
            primary key(id, name)
        );
        """.trimIndent()

    val t_task = """
        create table if not exists t_task(
            id int not null,
            name varchar(128) not null,
            job varchar(128) not null,
            manager_id int null,
            hire_date date not null,
            salary bigint not null,
            department_id int not null,
            primary key(id, name)
        );
        """.trimIndent()
}

object Departments : Table<Nothing>("t_department") {
    val id = int("id").primaryKey()
    val name = varchar("name").primaryKey()
    val location = varchar("location")
}

object Employees : Table<Nothing>("t_employee") {
    val id = int("id").primaryKey()
    val name = varchar("name").primaryKey()
    val job = varchar("job")
    val managerId = int("manager_id")
    val hireDate = date("hire_date")
    val salary = long("salary")
    val departmentId = int("department_id")
}

fun main() {
    val dbf = File("test1.db").canonicalPath
    val db = Database.connect("jdbc:sqlite:$dbf", "org.sqlite.JDBC")

    db.useTransaction(TransactionIsolation.SERIALIZABLE) {
        it.connection.createStatement().use {
            it.execute(DbSchema.t_flow)
            it.execute(DbSchema.t_task)
        }
    }

    db.useTransaction(TransactionIsolation.SERIALIZABLE) {
        it.connection.createStatement().use {
            it.execute(DbSchema.t_flow)
            it.execute(DbSchema.t_task)
        }
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