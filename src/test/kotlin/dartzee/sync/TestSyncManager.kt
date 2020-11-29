package dartzee.sync

import dartzee.core.util.getSqlDateNow
import dartzee.db.SyncAuditEntity
import dartzee.helper.*
import dartzee.logging.CODE_SQL_EXCEPTION
import dartzee.logging.CODE_SYNC_ERROR
import dartzee.logging.KEY_GAME_IDS
import dartzee.logging.Severity
import dartzee.utils.Database
import dartzee.utils.InjectedThings
import dartzee.utils.InjectedThings.mainDatabase
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.File
import java.net.SocketException
import java.sql.Timestamp
import kotlin.ConcurrentModificationException

class TestSyncManager: AbstractTest()
{
    override fun beforeEachTest()
    {
        super.beforeEachTest()
        File(TEST_DB_DIRECTORY).mkdirs()
    }

    override fun afterEachTest()
    {
        super.afterEachTest()
        File(TEST_DB_DIRECTORY).deleteRecursively()
    }

    @Test
    fun `Should show an error and log a warning if there is a connection error`()
    {
        val exception = SocketException("Failed to connect.")
        val store = mockk<IRemoteDatabaseStore>()
        every { store.fetchDatabase(any()) } throws exception

        val t = SyncManager(store).doSync(REMOTE_NAME)
        t.join()

        dialogFactory.errorsShown.shouldContainExactly("A connection error occurred. Check your internet connection and try again.")
        val log = verifyLog(CODE_SYNC_ERROR, Severity.WARN)
        log.message shouldBe "Caught network error during sync: $exception"
    }

    @Test
    fun `Should abort sync if merger validation fails`()
    {
        val remoteDb = mockk<Database>()
        every { remoteDb.testConnection() } returns false

        val store = InMemoryRemoteDatabaseStore("Goomba" to remoteDb)

        val t = SyncManager(store).doSync("Goomba")
        t.join()

        dialogFactory.errorsShown.shouldContainExactly("An error occurred connecting to the remote database.")
    }

    @Test
    fun `Should abort if a SQL error occurs during database merge`()
    {
        usingDbWithTestFile { remoteDb ->
            insertPlayer(database = mainDatabase)
            remoteDb.dropTable("Player")

            val store = InMemoryRemoteDatabaseStore(REMOTE_NAME to remoteDb)

            val t = SyncManager(store).doSync(REMOTE_NAME)
            t.join()

            dialogFactory.errorsShown.shouldContainExactly("An unexpected error occurred - no data has been changed.")

            val log = verifyLog(CODE_SQL_EXCEPTION, Severity.ERROR)
            log.message shouldContain "Caught SQLException for statement"
            log.errorObject?.message shouldContain "Table/View 'PLAYER' does not exist"

            databasesSwapped() shouldBe false
        }
    }

    @Test
    fun `Should throw an error if games from the original database have not made it through the merge`()
    {
        usingDbWithTestFile { remoteDb ->
            val g = insertGame(dtLastUpdate = Timestamp(1000), database = mainDatabase)
            SyncAuditEntity.insertSyncAudit(mainDatabase, REMOTE_NAME)

            val store = InMemoryRemoteDatabaseStore(REMOTE_NAME to remoteDb)

            val t = SyncManager(store).doSync(REMOTE_NAME)
            t.join()

            dialogFactory.errorsShown.shouldContainExactly("Sync resulted in missing data. \n\nResults have been discarded.")

            val log = verifyLog(CODE_SYNC_ERROR, Severity.ERROR)
            log.message shouldContain "1 game(s) missing from resulting database after merge"
            log.keyValuePairs[KEY_GAME_IDS] shouldBe setOf(g.rowId)

            databasesSwapped() shouldBe false
        }
    }

    @Test
    fun `Should show an error and log a warning if remote db has been modified since it was pulled`()
    {
        usingDbWithTestFile { remoteDb ->
            val exception = ConcurrentModificationException("Oh no")
            val store = mockk<IRemoteDatabaseStore>()
            every { store.fetchDatabase(any()) } returns FetchDatabaseResult(remoteDb, getSqlDateNow())
            every { store.pushDatabase(any(), any(), any()) } throws exception

            val t = SyncManager(store).doSync(REMOTE_NAME)
            t.join()

            dialogFactory.errorsShown.shouldContainExactly("Another sync has been performed since this one started. \n" +
                    "\n" +
                    "Results have been discarded.")

            val log = verifyLog(CODE_SYNC_ERROR, Severity.WARN)
            log.message shouldBe "$exception"

            databasesSwapped() shouldBe false
        }
    }

    @Test
    fun `Should successfully sync data between remote and local db, pushing up the result and swapping in locally`()
    {
        usingDbWithTestFile { remoteDb ->
            insertGame(database = mainDatabase)
            insertGame(database = remoteDb)

            val store = InMemoryRemoteDatabaseStore(REMOTE_NAME to remoteDb)
            val t = SyncManager(store).doSync(REMOTE_NAME)
            t.join()

            val resultingRemote = store.fetchDatabase(REMOTE_NAME).database
            getCountFromTable("Game", resultingRemote) shouldBe 2

            val summary = "\n\nGames pushed: 1\nGames pulled: 1"
            val expectedInfoText = "Sync completed successfully!$summary"
            dialogFactory.infosShown.shouldContainExactly(expectedInfoText)

            databasesSwapped() shouldBe true
        }
    }

    private fun usingDbWithTestFile(testBlock: (inMemoryDatabase: Database) -> Unit)
    {
        usingInMemoryDatabase(withSchema = true) { remoteDb ->
            val f = File("${remoteDb.getDirectoryStr()}/SomeFile.txt")
            f.createNewFile()

            testBlock(remoteDb)
        }
    }

    private fun databasesSwapped() = File("${InjectedThings.databaseDirectory}/Darts/SomeFile.txt").exists()
}