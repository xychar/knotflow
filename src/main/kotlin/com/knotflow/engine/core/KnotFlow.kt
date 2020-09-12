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
 * The properties of the class are from the workflow settings.
 *
 * The input interface will be implemented by engine and registered as a service.
 *
 * An input interface can be the super class or injected as a property.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkflowInput

/**
 * The properties of the class will be persisted with the workflow instance.
 *
 * The output interface will be implemented by engine and registered as a service.
 *
 * An output interface can be the super class or injected as a property.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WorkflowOutput

/**
 * The function is a stateful step.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Step
