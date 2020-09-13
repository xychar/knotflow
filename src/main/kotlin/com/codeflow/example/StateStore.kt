@file:Suppress("unused")

package com.codeflow.example

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.InsertProvider
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Result
import org.apache.ibatis.annotations.ResultMap
import org.apache.ibatis.annotations.Results
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.SelectProvider
import org.apache.ibatis.annotations.Update
import org.apache.ibatis.annotations.UpdateProvider
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider
import org.mybatis.dynamic.sql.util.SqlProviderAdapter

data class StepState(
    var sessionId: String? = null,
    var stepName: String? = null,
    var stepKey: String? = null,
    var state: String? = null,
)

@Mapper
interface StepStateMapper {
    @Update(
        "CREATE TABLE IF NOT EXISTS t_step_state(",
        "  session_id varchar(50) NOT NULL,",
        "  step_name varchar(200) NOT NULL,",
        "  step_key varchar(200) NOT NULL,",
        "  state varchar(20) NOT NULL,",
        "  PRIMARY KEY(session_id, step_name, step_key)",
        ")",
    )
    fun createTable()

    @Results(
        id = "stepState", value = [
            Result(property = "sessionId", column = "session_id", id = true),
            Result(property = "stepName", column = "step_name", id = true),
            Result(property = "stepKey", column = "step_key"),
            Result(property = "state", column = "state"),
        ]
    )
    @Select(
        "SELECT * FROM t_step_state WHERE session_id = #{sessionId}",
        " and step_name = #{stepName} and step_key = #{stepKey}",
    )
    fun getStepState(
        @Param("sessionId") sessionId: String,
        @Param("stepName") stepName: String,
        @Param("stepKey") stepKey: String,
    ): StepState?

    @Select(
        "SELECT * FROM t_step_state WHERE session_id = #{sessionId}",
        " and step_name = #{stepName} and step_key = #{stepKey}",
    )
    @ResultMap("stepState")
    fun getStepStateBy(example: StepState): StepState?

    @Insert(
        "INSERT INTO t_step_state(session_id, step_name, step_key, state)",
        " values(#{sessionId}, #{stepName}, #{stepKey}, #{state})",
    )
    fun addStepState(stepState: StepState)

    @InsertProvider(type = SqlProviderAdapter::class, method = "insert")
    fun insert(insertStatement: InsertStatementProvider<StepState?>?): Int

    @UpdateProvider(type = SqlProviderAdapter::class, method = "update")
    fun update(updateStatement: UpdateStatementProvider?): Int

    @ResultMap("stepState")
    @SelectProvider(type = SqlProviderAdapter::class, method = "select")
    fun selectMany(selectStatement: SelectStatementProvider?): List<StepState?>?

    @ResultMap("stepState")
    @SelectProvider(type = SqlProviderAdapter::class, method = "select")
    fun selectOne(selectStatement: SelectStatementProvider?): StepState?
}
