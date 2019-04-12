package burlton.dartzee.code.db

import burlton.core.code.util.AbstractClient
import burlton.core.code.util.Debug
import burlton.core.code.util.StringUtil
import burlton.dartzee.code.utils.DatabaseUtil
import burlton.dartzee.code.utils.SqlErrorConstants
import burlton.desktopcore.code.util.DateStatics
import burlton.desktopcore.code.util.getSqlDateNow
import java.sql.*
import java.util.*
import java.util.regex.Pattern

abstract class AbstractEntity<E : AbstractEntity<E>> : SqlErrorConstants
{
    //DB Fields
    var rowId: String = ""
    var dtCreation = getSqlDateNow()
    var dtLastUpdate = DateStatics.END_OF_TIME

    //other variables
    var retrievedFromDb = false

    /**
     * Abstract fns
     */
    abstract fun getTableName(): String
    abstract fun getCreateTableSqlSpecific(): String

    @Throws(SQLException::class)
    abstract fun populateFromResultSet(entity: E, rs: ResultSet)

    @Throws(SQLException::class)
    abstract fun writeValuesToStatement(statement: PreparedStatement, startIndex: Int, emptyStatement: String): String

    /**
     * Default implementations
     */
    open fun getColumnsAllowedToBeUnset() = mutableListOf<String>()
    open fun addListsOfColumnsForIndexes(indexes: MutableList<MutableList<String>>) {}

    /**
     * Helpers
     */
    private fun getColumnCount(): Int
    {
        val columns = getColumnsForSelectStatement()
        return StringUtil.countOccurences(columns, ",") + 1
    }

    private fun getCreateTableColumnSql() = "RowId VARCHAR(36) PRIMARY KEY, DtCreation Timestamp NOT NULL, DtLastUpdate Timestamp NOT NULL, ${getCreateTableSqlSpecific()}"

    fun getColumns(): MutableList<String>
    {
        val columnCreateSql = getCreateTableColumnSql()
        val cols = StringUtil.getListFromDelims(columnCreateSql, ",")

        return cols.map{getColumnNameFromCreateSql(it)}.toMutableList()
    }

    fun getTableNameUpperCase() = getTableName().toUpperCase()

    fun factoryFromResultSet(rs: ResultSet): E
    {
        val ret = factory()
        ret!!.rowId = rs.getString("RowId")
        ret.dtCreation = rs.getTimestamp("DtCreation")
        ret.dtLastUpdate = rs.getTimestamp("DtLastUpdate")

        populateFromResultSet(ret, rs)

        return ret
    }

    private fun factory(): E?
    {
        try
        {
            return javaClass.newInstance() as E
        }
        catch (iae: IllegalAccessException)
        {
            Debug.stackTrace(iae)
            return null
        }
        catch (iae: InstantiationException)
        {
            Debug.stackTrace(iae)
            return null
        }
    }


    fun columnCanBeUnset(columnName: String) = getColumnsAllowedToBeUnset().contains(columnName)

    open fun assignRowId(): String
    {
        rowId = UUID.randomUUID().toString()
        return rowId
    }

    fun retrieveEntity(whereSql: String): E?
    {
        val entities = retrieveEntities(whereSql)
        if (entities.size > 1)
        {
            Debug.stackTrace("Retrieved ${entities.size} rows from ${getTableName()}. Expected 1. WhereSQL [$whereSql]")
        }

        return if (entities.isEmpty())
        {
            null
        }
        else entities.first()
    }

    @JvmOverloads
    fun retrieveEntities(whereSql: String = "", alias: String = ""): MutableList<E>
    {
        var queryWithFrom = "FROM ${getTableName()} $alias"
        if (!whereSql.isEmpty())
        {
            queryWithFrom += " WHERE $whereSql"
        }

        return retrieveEntitiesWithFrom(queryWithFrom, alias)
    }

    fun retrieveEntitiesWithFrom(whereSqlWithFrom: String, alias: String): MutableList<E>
    {
        val query = "SELECT " + getColumnsForSelectStatement(alias) + " " + whereSqlWithFrom

        val ret = mutableListOf<E>()

        try
        {
            DatabaseUtil.executeQuery(query).use { rs ->
                while (rs.next())
                {
                    val entity = factoryFromResultSet(rs)
                    entity.retrievedFromDb = true
                    ret.add(entity)
                }
            }
        }
        catch (sqle: SQLException)
        {
            Debug.logSqlException(query, sqle)
        }

        return ret
    }

    @JvmOverloads
    fun retrieveForId(rowId: String, stackTraceIfNotFound: Boolean = true): E?
    {
        val entities = retrieveEntities("RowId = '$rowId'")
        if (entities.isEmpty())
        {
            if (stackTraceIfNotFound)
            {
                Debug.stackTrace("Failed to find ${getTableName()} for ID [$rowId]")
            }

            return null
        }

        if (entities.size > 1)
        {
            Debug.stackTrace("Found ${entities.size} ${getTableName()} rows for ID [$rowId]")
        }

        return entities[0]
    }

    fun deleteFromDatabase(): Boolean
    {
        val sql = "DELETE FROM ${getTableName()} WHERE RowId = '$rowId'"
        return DatabaseUtil.executeUpdate(sql)
    }

    @JvmOverloads
    fun saveToDatabase(dtLastUpdate: Timestamp = getSqlDateNow())
    {
        this.dtLastUpdate = dtLastUpdate

        if (retrievedFromDb)
        {
            updateDatabaseRow()
        }
        else
        {
            insertIntoDatabase()
        }
    }

    private fun updateDatabaseRow()
    {
        var updateQuery = buildUpdateQuery()

        val conn = DatabaseUtil.borrowConnection()
        try
        {
            conn.prepareStatement(updateQuery).use { psUpdate ->
                updateQuery = writeTimestamp(psUpdate, 1, dtCreation, updateQuery)
                updateQuery = writeTimestamp(psUpdate, 2, dtLastUpdate, updateQuery)
                updateQuery = writeValuesToStatement(psUpdate, 3, updateQuery)
                updateQuery = writeString(psUpdate, getColumnCount(), rowId, updateQuery)

                Debug.appendSql(updateQuery, AbstractClient.traceWriteSql)

                psUpdate.executeUpdate()

                val updateCount = psUpdate.updateCount
                if (updateCount == 0)
                {
                    Debug.stackTrace("0 rows updated: $updateQuery")
                }
            }
        }
        catch (sqle: SQLException)
        {
            Debug.logSqlException(updateQuery, sqle)
        }
        finally
        {
            DatabaseUtil.returnConnection(conn)
        }
    }

    private fun buildUpdateQuery(): String
    {
        //Some fun String manipulation
        var columns = getColumnsForSelectStatement()
        columns = columns.replaceFirst("RowId, ", "")
        columns = columns.replace(",", "=?,")
        columns += "=?"

        return "UPDATE ${getTableName()} SET $columns WHERE RowId=?"
    }

    private fun insertIntoDatabase()
    {
        var insertQuery = buildInsertQuery()

        val conn = DatabaseUtil.borrowConnection()
        try
        {
            conn.prepareStatement(insertQuery).use { psInsert ->
                insertQuery = writeValuesToInsertStatement(insertQuery, psInsert)

                Debug.appendSql(insertQuery, AbstractClient.traceWriteSql)

                psInsert.executeUpdate()

                //Set this so we can call save() again on the same object and get the right behaviour
                retrievedFromDb = true
            }
        }
        catch (sqle: SQLException)
        {
            Debug.logSqlException(insertQuery, sqle)
        }
        finally
        {
            DatabaseUtil.returnConnection(conn)
        }
    }

    fun writeValuesToInsertStatement(emptyStatement: String, psInsert: PreparedStatement, entityNumber: Int = 0): String
    {
        val adjustment = entityNumber * getColumnCount()

        var insertQuery = emptyStatement
        insertQuery = writeString(psInsert, 1 + adjustment, rowId, insertQuery)
        insertQuery = writeTimestamp(psInsert, 2 + adjustment, dtCreation, insertQuery)
        insertQuery = writeTimestamp(psInsert, 3 + adjustment, dtLastUpdate, insertQuery)
        insertQuery = writeValuesToStatement(psInsert, 4 + adjustment, insertQuery)

        return insertQuery
    }

    private fun buildInsertQuery() = "INSERT INTO ${getTableName()} VALUES ${getInsertBlockForStatement()}"

    fun getInsertBlockForStatement() = "(${getColumns().joinToString{"?"}})"

    open fun createTable(): Boolean
    {
        val createdTable = DatabaseUtil.createTableIfNotExists(getTableName(), getCreateTableColumnSql())
        if (createdTable)
        {
            createIndexes()
        }

        return createdTable
    }

    fun createIndexes()
    {
        //Also create the indexes
        val indexes = mutableListOf<MutableList<String>>()
        addListsOfColumnsForIndexes(indexes)

        indexes.forEach{
            createIndex(it)
        }
    }

    private fun createIndex(columns: MutableList<String>)
    {
        val columnList = StringUtil.toDelims(columns, ",")
        val indexName = columnList.replace(",", "_")

        val statement = "CREATE INDEX $indexName ON ${getTableName()}($columnList)"
        val success = DatabaseUtil.executeUpdate(statement)
        if (!success)
        {
            Debug.append("Failed to create index $indexName on ${getTableName()}")
        }
    }

    fun addIntColumn(columnName: String): Boolean
    {
        return addColumn(columnName, "INT", "-1")
    }

    fun addStringColumn(columnName: String, length: Int): Boolean
    {
        return addColumn(columnName, "VARCHAR($length)", "''")
    }

    private fun addColumn(columnName: String, dataType: String, defaultValue: String): Boolean
    {
        if (columnExists(columnName))
        {
            Debug.append("Not adding column $columnName to ${getTableName()} as it already exists")
            return false
        }

        val sql = "ALTER TABLE ${getTableName()} ADD COLUMN $columnName $dataType NOT NULL DEFAULT $defaultValue"
        val addedColumn = DatabaseUtil.executeUpdate(sql)
        if (!addedColumn)
        {
            return false
        }

        //We've added the column, now attempt to drop the default.
        val defaultSql = "ALTER TABLE ${getTableName()} ALTER COLUMN $columnName DEFAULT NULL"
        return DatabaseUtil.executeUpdate(defaultSql)
    }

    private fun columnExists(columnName: String): Boolean
    {
        val columnNameUpperCase = columnName.toUpperCase()
        val tableName = getTableNameUpperCase()

        val sb = StringBuilder()
        sb.append("SELECT COUNT(1) ")
        sb.append("FROM sys.systables t, sys.syscolumns c ")
        sb.append("WHERE c.ReferenceId = t.TableId ")
        sb.append("AND t.TableName = '")
        sb.append(tableName)
        sb.append("' AND c.ColumnName = '")
        sb.append(columnNameUpperCase)
        sb.append("'")

        val count = DatabaseUtil.executeQueryAggregate(sb)
        return count > 0
    }

    protected fun getColumnsForSelectStatement(alias: String = ""): String
    {
        val sb = StringBuilder()

        val cols = getColumns()
        for (i in cols.indices)
        {
            if (i > 0)
            {
                sb.append(", ")
            }

            var column = cols[i]
            if (!alias.isEmpty())
            {
                column = "$alias.$column"
            }

            sb.append(column)
        }

        return sb.toString()
    }

    private fun getColumnNameFromCreateSql(col: String): String
    {
        var colSanitised = col
        colSanitised = colSanitised.trim()
        colSanitised = colSanitised.replace("(", "")
        colSanitised = colSanitised.replace(")", "")

        return colSanitised.split(" ")[0]
    }

    fun getField(fieldName: String): Any?
    {
        println("${getTableName()} - $fieldName")
        val getter = javaClass.getMethod("get$fieldName")
        return getter.invoke(this)
    }
    fun setField(fieldName: String, value: Any?)
    {
        val getMethod = javaClass.getMethod("get$fieldName")
        val setMethod = javaClass.getDeclaredMethod("set$fieldName", getMethod.returnType)

        setMethod.invoke(this, value)
    }
    fun getFieldType(fieldName: String): Class<*>
    {
        val getter = javaClass.getMethod("get$fieldName")
        return getter.returnType
    }

    /**
     * Write to statement methods
     */
    @Throws(SQLException::class)
    fun writeLong(ps: PreparedStatement, ix: Int, value: Long, statementStr: String): String
    {
        ps.setLong(ix, value)
        return swapInValue(statementStr, value)
    }

    @Throws(SQLException::class)
    fun writeInt(ps: PreparedStatement, ix: Int, value: Int, statementStr: String): String
    {
        ps.setInt(ix, value)
        return swapInValue(statementStr, value)
    }

    @Throws(SQLException::class)
    fun writeString(ps: PreparedStatement, ix: Int, value: String, statementStr: String): String
    {
        ps.setString(ix, value)
        return swapInValue(statementStr, "'$value'")
    }

    @Throws(SQLException::class)
    fun writeTimestamp(ps: PreparedStatement, ix: Int, value: Timestamp, statementStr: String): String
    {
        ps.setTimestamp(ix, value)
        return swapInValue(statementStr, "'$value'")
    }

    @Throws(SQLException::class)
    fun writeBlob(ps: PreparedStatement, ix: Int, value: Blob, statementStr: String): String
    {
        ps.setBlob(ix, value)
        val blobStr = "Blob (dataLength: " + value.length() + ")"
        return swapInValue(statementStr, blobStr)
    }

    @Throws(SQLException::class)
    fun writeBoolean(ps: PreparedStatement, ix: Int, value: Boolean, statementStr: String): String
    {
        ps.setBoolean(ix, value)
        return swapInValue(statementStr, value)
    }

    private fun swapInValue(statementStr: String, value: Any): String
    {
        return statementStr.replaceFirst(Pattern.quote("?").toRegex(), "" + value)
    }
}
