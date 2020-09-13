@file:Suppress("unused")

package com.codeflow.example

@Workflow
interface WorkflowBase1 {
    @Step
    fun hello(t1: String) {
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

        hello("ab")
        return data
    }
}

fun main() {
    runSpringApp {
        val controller = WorkflowController.buildFrom(WorkflowExample1::class)
        val workflowInstance = controller.newInstance<WorkflowExample1>()

        val stepStateMapper = getBean(StepStateMapper::class.java)
        stepStateMapper.createTable()

        val state1 = StepState(
            sessionId = "s_001",
            stepName = "step01",
            stepKey = "---",
        )

        val state2 = state1.copy(
            state = "Done",
        )

        if (stepStateMapper.getStepStateBy(state1) == null) {
            stepStateMapper.addStepState(state2)
        }

        val t1 = workflowInstance.workflow
        println(t1.example1())
    }
}
