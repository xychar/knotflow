package com.knotflow.engine.demo.mybatis

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Result
import org.apache.ibatis.annotations.Results
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.mybatis.spring.SqlSessionTemplate
import org.mybatis.spring.annotation.MapperScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role
import org.springframework.core.env.Environment
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource


data class User(
    var id: String? = null,
    var name: String? = null,
)

@Mapper
interface UserMapper {
    @Results(
        id = "userResult", value = [
            Result(property = "id", column = "uid", id = true),
            Result(property = "name", column = "name"),
        ]
    )
    @Select("SELECT * FROM users WHERE uid = #{userId}")
    fun getUser(@Param("userId") userId: String): User?

    @Insert("insert or ignore into users(uid, name) values(#{userId}, #{userName})")
    fun addUser(@Param("userId") userId: String, @Param("userName") userName: String)
}

@Configuration
@EnableTransactionManagement
@MapperScan("com.knotflow.engine.demo.mybatis")
@PropertySource("classpath:database.properties")
class MyBatisConfig {
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

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
class ConfigurationTest {
    @Bean
    fun exceptionTranslation(): PersistenceExceptionTranslationPostProcessor {
        return PersistenceExceptionTranslationPostProcessor()
    }
}

@Service
@Transactional
class FooServiceImpl(
    private val userMapper: UserMapper,
    private val sqlSession: SqlSession,
) {
    fun prepareData(userId: String) {
        sqlSession.connection.apply {
            createStatement().use {
                it.execute(
                    """
                    CREATE TABLE if not exists users (
                        uid     TEXT,
                        name	TEXT,
                        PRIMARY KEY(uid)
                    );
                    """.trimIndent()
                )
            }
        }

        userMapper.addUser(userId, "Hallow")
    }

    fun doSomeStuff(userId: String?): User? {
        return userMapper.getUser(userId!!)
    }
}

fun main() {
    val context = AnnotationConfigApplicationContext()

    val packagesToScan = arrayOf(
        "com.knotflow.engine.core",
        "com.knotflow.engine.demo.mybatis",
    )

    context.scan(*packagesToScan)
    context.refresh()
    context.start()

    val foo = context.getBean(FooServiceImpl::class.java)
    foo.prepareData("u123")
    println(foo.doSomeStuff("u123"))

    context.stop()
    context.close()
}