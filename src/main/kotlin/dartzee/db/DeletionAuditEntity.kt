package dartzee.db

import dartzee.utils.Database
import dartzee.utils.InjectedThings.mainDatabase

/**
 * Represents a row that's been deleted.
 * Used by the sync to ensure deleted rows stay deleted
 */
class DeletionAuditEntity(database: Database = mainDatabase): AbstractEntity<DeletionAuditEntity>(database)
{
    /**
     * DB fields
     */
    var entityName: EntityName = EntityName.DeletionAudit
    var entityId = ""

    override fun getTableName() = EntityName.DeletionAudit

    override fun getCreateTableSqlSpecific(): String
    {
        return ("EntityName VARCHAR(255) NOT NULL, "
                + "EntityId VARCHAR(36) NOT NULL")
    }

    companion object
    {
        fun factoryAndSave(entityName: EntityName, entityId: String): DeletionAuditEntity
        {
            val entity = DeletionAuditEntity()
            entity.assignRowId()
            entity.entityName = entityName
            entity.entityId = entityId
            entity.saveToDatabase()
            return entity
        }
    }
}