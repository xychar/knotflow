@file:Suppress("unused")

package com.knotflow.engine.demo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.*
import org.springframework.core.env.Environment
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.*
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource
import kotlin.reflect.jvm.javaMethod

@Component
class TestService1 {
    init {
        println("TestService1.constructor")
    }

    fun hello1() {
        println(::hello1.javaMethod?.name)
    }
}

@Component
class TestService2 {
    init {
        println("TestService2.constructor")
    }

    fun hello2() {
        println(::hello2.javaMethod?.name)
    }
}

@Configuration
//@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableTransactionManagement
@PropertySource("classpath:database.properties")
class PersistenceJPAConfig {
    @Autowired
    lateinit var env: Environment

    @Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val entityManagerFactoryBean = LocalContainerEntityManagerFactoryBean()
        entityManagerFactoryBean.dataSource = dataSource()
        entityManagerFactoryBean.setPackagesToScan("com.knotflow.engine.demo")

        val vendorAdapter = HibernateJpaVendorAdapter()
        entityManagerFactoryBean.jpaVendorAdapter = vendorAdapter
        entityManagerFactoryBean.setJpaProperties(additionalProperties())

        return entityManagerFactoryBean
    }

    fun additionalProperties(): Properties {
        val props = Properties()

        arrayOf(
            "hibernate.hbm2ddl.auto",
            "hibernate.show_sql",
            "hibernate.dialect",
            "hibernate.cache.use_second_level_cache",
            "hibernate.cache.use_query_cache",
        ).forEach {
            props.setProperty(it, env.getProperty(it))
        }

        return props
    }

    @Bean
    fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName"))
        dataSource.url = env.getProperty("jdbc.url")
        dataSource.username = env.getProperty("jdbc.user")
        dataSource.password = env.getProperty("jdbc.pass")

        return dataSource
    }

    @Bean
    fun transactionManager(emf: EntityManagerFactory?): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = emf
        return transactionManager
    }
}

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
class ConfigurationTest {
    @Bean
    fun exceptionTranslation(): PersistenceExceptionTranslationPostProcessor {
        return PersistenceExceptionTranslationPostProcessor()
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