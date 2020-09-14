package com.codeflow.example

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.mybatis.spring.SqlSessionTemplate
import org.mybatis.spring.annotation.MapperScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role
import org.springframework.core.env.Environment
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
class ExceptionConfig {
    @Bean
    fun exceptionTranslation(): PersistenceExceptionTranslationPostProcessor {
        return PersistenceExceptionTranslationPostProcessor()
    }
}

@Configuration
@EnableTransactionManagement
@MapperScan(annotationClass = Mapper::class)
@ComponentScan(basePackageClasses = [AppConfig::class])
@PropertySource("classpath:database.properties")
class AppConfig {
    @Autowired
    lateinit var env: Environment

    @Bean
    @Throws(Exception::class)
    fun sqlSessionFactory(): SqlSessionFactory? {
        val factoryBean = SqlSessionFactoryBean()
        factoryBean.setDataSource(dataSource())
        return factoryBean.getObject()
    }

    @Bean
    fun transactionManager(): DataSourceTransactionManager? {
        return DataSourceTransactionManager(dataSource())
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
    @Throws(java.lang.Exception::class)
    fun sqlSession(): SqlSessionTemplate? {
        return SqlSessionTemplate(sqlSessionFactory())
    }
}

inline fun runSpringApp(body: ApplicationContext.() -> Unit) {
    val context = AnnotationConfigApplicationContext()
    context.register(AppConfig::class.java)

    context.refresh()
    context.start()

    try {
        context.run(body)
    } finally {
        context.stop()
        context.close()
    }
}
