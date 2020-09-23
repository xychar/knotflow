@file:Suppress("unused")

package com.codeflow.example

@Workflow
interface WorkflowBase1 {
    @Step
    fun hello(t1: String) {
        println("*** Method ${::hello.name} executed in WorkflowBase1")

        welcome(t1)
    }

    @Step
    fun welcome(t1: String) {
        println("*** Method ${::welcome.name} executed in WorkflowBase1")
    }
}

@Workflow
interface WorkflowExample1 : WorkflowBase1 {
    var foo: String

    @Step
    override fun hello(t1: String) {
        println("*** Method ${::hello.name} executed in WorkflowExample1")
        super.hello(t1)
    }

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
        controller.stepStateStore = getBean(StepStateStore::class.java)

        val stepInfoMapper = getBean(StepInfoMapper::class.java)
        stepInfoMapper.createTable()

        val step1 = StepInfo(
            sessionId = "s_001",
            stepName = "step01",
            stepKey = "-",
        )

        val step2 = step1.copy(state = "Done")

        if (stepInfoMapper.getStepInfoBy(step1) == null) {
            stepInfoMapper.addStepInfo(step2)
        }

        val workflowInstance = controller.newInstance<WorkflowExample1>()

        val t1 = workflowInstance.workflow
        println(t1.example1())

        val stepState = controller.stepStateStore.loadState(StepStateKey("s01", "hello", "-"))
        println(stepState)
    }
}
