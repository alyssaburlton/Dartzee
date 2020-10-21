package dartzee.utils

import dartzee.core.util.DialogUtil
import dartzee.core.util.FileUtil
import dartzee.db.*
import dartzee.logging.CODE_STARTING_BACKUP
import dartzee.logging.CODE_STARTING_RESTORE
import dartzee.logging.KEY_DB_VERSION
import dartzee.screen.ScreenCache
import dartzee.utils.InjectedThings.logger
import dartzee.utils.InjectedThings.mainDatabase
import org.apache.derby.jdbc.EmbeddedDriver
import java.io.File
import java.sql.DriverManager
import javax.swing.JOptionPane
import kotlin.system.exitProcess

const val TOTAL_ROUND_SCORE_SQL_STR = "(drtFirst.StartingScore - drtLast.StartingScore) + (drtLast.score * drtLast.multiplier)"

/**
 * Database helpers specific to Dartzee, e.g. first time initialisation
 */
object DartsDatabaseUtil
{
    const val DATABASE_VERSION = 15
    const val DATABASE_NAME = "jdbc:derby:Databases/Darts;create=true"

    private val DATABASE_FILE_PATH_TEMP = DATABASE_FILE_PATH + "_copying"

    fun getAllEntities(database: Database = mainDatabase): List<AbstractEntity<*>>
    {
        return listOf(PlayerEntity(database),
                DartEntity(database),
                GameEntity(database),
                ParticipantEntity(database),
                PlayerImageEntity(database),
                DartsMatchEntity(database),
                AchievementEntity(database),
                DartzeeRuleEntity(database),
                DartzeeTemplateEntity(database),
                DartzeeRoundResultEntity(database),
                X01FinishEntity(database),
                PendingLogsEntity(database))
    }

    fun getAllEntitiesIncludingVersion(database: Database = mainDatabase) =
        getAllEntities(database) + VersionEntity(database)

    fun initialiseDatabase()
    {
        DriverManager.registerDriver(EmbeddedDriver())

        DialogUtil.showLoadingDialog("Checking database status...")

        mainDatabase.doDuplicateInstanceCheck()

        //Pool the db connections now. Initialise with 5 to begin with?
        mainDatabase.initialiseConnectionPool(5)

        val version = mainDatabase.getDatabaseVersion()

        logger.addToContext(KEY_DB_VERSION, version)

        DialogUtil.dismissLoadingDialog()

        val migrator = DatabaseMigrator(DatabaseMigrations.getConversionsMap())
        migrateDatabase(migrator)
    }

    fun migrateDatabase(migrator: DatabaseMigrator)
    {
        val result = migrator.migrateToLatest(mainDatabase, "Your")
        if (result == MigrationResult.TOO_OLD)
        {
            exitProcess(1)
        }

        logger.addToContext(KEY_DB_VERSION, DATABASE_VERSION)
    }

    /**
     * Backup / Restore
     */
    fun backupCurrentDatabase()
    {
        val dbFolder = File(DATABASE_FILE_PATH)

        logger.info(CODE_STARTING_BACKUP, "About to start DB backup")

        val file = FileUtil.chooseDirectory(ScreenCache.mainScreen)
                ?: //Cancelled
                return

        val destinationPath = file.absolutePath + "\\Databases"
        val success = dbFolder.copyRecursively(File(destinationPath))
        if (!success)
        {
            DialogUtil.showError("There was a problem creating the backup.")
        }

        DialogUtil.showInfo("Database successfully backed up to $destinationPath")
    }

    fun restoreDatabase()
    {
        logger.info(CODE_STARTING_RESTORE, "About to start DB restore")

        if (!checkAllGamesAreClosed())
        {
            return
        }

        val directoryFrom = selectAndValidateNewDatabase()
                ?: //Cancelled, or invalid
                return

        //Confirm at this point
        val confirmationQ = "Successfully conected to target database. " + "\n\nAre you sure you want to restore this database? All current data will be lost."
        val option = DialogUtil.showQuestion(confirmationQ, false)
        if (option == JOptionPane.NO_OPTION)
        {
            return
        }

        //Copy the files to a temporary file path in the application directory - Databases_copying.
        val success = directoryFrom.copyRecursively(File(DATABASE_FILE_PATH_TEMP), true)
        if (!success)
        {
            DialogUtil.showError("Restore failed - failed to copy the new database files.")
            return
        }

        //Issue a shutdown command to derby so we no longer have a handle on the old files
        val shutdown = mainDatabase.shutdownDerby()
        if (!shutdown)
        {
            DialogUtil.showError("Failed to shut down current database connection, unable to restore new database.")
            return
        }

        //Now switch it in
        val error = FileUtil.swapInFile(DATABASE_FILE_PATH, DATABASE_FILE_PATH_TEMP)
        if (error != null)
        {
            DialogUtil.showError("Failed to restore database. Error: $error")
            return
        }

        DialogUtil.showInfo("Database successfully restored. Application will now exit.")
        exitProcess(0)
    }

    private fun selectAndValidateNewDatabase(): File?
    {
        DialogUtil.showInfo("Select the 'Databases' folder you want to restore from.")
        val directoryFrom = FileUtil.chooseDirectory(ScreenCache.mainScreen)
                ?: //Cancelled
                return null

        //Check it's named right
        val name = directoryFrom.name
        if (name != "Databases")
        {
            DialogUtil.showError("Selected path is not valid - you must select a folder named 'Databases'")
            return null
        }

        //Test we can connect
        val otherDatabase = Database(filePath = directoryFrom.absolutePath)
        val testSuccess = otherDatabase.testConnection()
        if (!testSuccess)
        {
            DialogUtil.showError("Testing conection failed for the selected database. Cannot restore from this location.")
            return null
        }

        return directoryFrom
    }

    private fun checkAllGamesAreClosed(): Boolean
    {
        val openScreens = ScreenCache.getDartsGameScreens()
        if (openScreens.isNotEmpty())
        {
            DialogUtil.showError("You must close all open games before continuing.")
            return false
        }

        return true
    }
}
