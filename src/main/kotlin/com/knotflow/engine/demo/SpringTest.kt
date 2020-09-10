@file:Suppress("unused")

package com.knotflow.engine.demo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role
import org.springframework.core.env.Environment
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.Properties
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityManagerFactory
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
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

@Entity
@Table(name = "customer")
data class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int,

    @Column(name = "first_name", length = 50)
    var firstName: String,

    @Column(name = "last_name", length = 50)
    var lastName: String,

    @Column(name = "email", length = 200)
    var email: String,
)

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