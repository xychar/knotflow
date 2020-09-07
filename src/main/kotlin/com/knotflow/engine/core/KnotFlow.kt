@file:Suppress("unused")

package com.knotflow.engine.core

/**
 * Define a workflow interface.
 *
 * Only used to scan workflows in the classpath.
 *
 * A workflow can extend from other multiple interfaces.
 * The super interfaces can be other workflow interfaces,
 * input interfaces, output interfaces, or cleaner interfaces.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Workflow


/**
 * The function is a stateful step.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkflowStep
