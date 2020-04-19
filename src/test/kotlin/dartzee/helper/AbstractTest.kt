package dartzee.helper

import dartzee.CURRENT_TIME
import dartzee.`object`.DartsClient
import dartzee.core.helper.TestDebugExtension
import dartzee.core.helper.TestMessageDialogFactory
import dartzee.core.helper.checkedForExceptions
import dartzee.core.helper.exceptionLogged
import dartzee.core.util.Debug
import dartzee.core.util.DialogUtil
import dartzee.core.util.TestDebug
import dartzee.db.LocalIdGenerator
import dartzee.logging.*
import dartzee.utils.DartsDatabaseUtil
import dartzee.utils.InjectedThings
import io.kotlintest.shouldBe
import io.mockk.clearAllMocks
import org.apache.derby.jdbc.EmbeddedDriver
import org.junit.After
import org.junit.Before
import java.sql.DriverManager
import java.time.Clock
import java.time.ZoneId
import javax.swing.UIManager
import kotlin.test.assertNotNull

private const val DATABASE_NAME_TEST = "jdbc:derby:memory:Darts;create=true"
private var doneOneTimeSetup = false
private val logDestination = FakeLogDestination()
val logger = Logger(listOf(logDestination, LogDestinationSystemOut()))

abstract class AbstractTest
{
    private var doneClassSetup = false
    protected val dialogFactory = TestMessageDialogFactory()

    @Before
    fun oneTimeSetup()
    {
        if (!doneOneTimeSetup)
        {
            doOneTimeSetup()
            doneOneTimeSetup = true
        }

        if (!doneClassSetup)
        {
            doClassSetup()
            doneClassSetup = true
        }

        beforeEachTest()
    }

    private fun doOneTimeSetup()
    {
        Debug.initialise(TestDebug.SimpleDebugOutput())
        Debug.logToSystemOut = true

        Debug.debugExtension = TestDebugExtension()
        DialogUtil.init(dialogFactory)

        InjectedThings.logger = logger
        InjectedThings.dartzeeCalculator = FakeDartzeeCalculator()
        InjectedThings.verificationDartboardSize = 50
        InjectedThings.clock = Clock.fixed(CURRENT_TIME, ZoneId.of("UTC"))

        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
        DartsClient.derbyDbName = DATABASE_NAME_TEST
        DriverManager.registerDriver(EmbeddedDriver())
        DartsDatabaseUtil.initialiseDatabase()
    }

    open fun doClassSetup()
    {
        Debug.initialise(TestDebug.SimpleDebugOutput())
        Debug.logToSystemOut = true
        DialogUtil.init(dialogFactory)
    }

    open fun beforeEachTest()
    {
        Debug.lastErrorMillis = -1
        Debug.initialise(TestDebug.SimpleDebugOutput())
        dialogFactory.reset()
        clearLogs()
        clearAllMocks()

        LocalIdGenerator.hmLastAssignedIdByTableName.clear()
        DartsDatabaseUtil.getAllEntities().forEach { wipeTable(it.getTableName()) }

        logger.loggingContext.clear()
    }

    @After
    open fun afterEachTest()
    {
        if (!checkedForExceptions)
        {
            exceptionLogged() shouldBe false
            errorLogged() shouldBe false
        }

        checkedForExceptions = false
    }

    fun getLastLog() = getLogRecords().last()

    fun verifyLog(code: LoggingCode, severity: Severity = Severity.INFO): LogRecord
    {
        val record = getLogRecords().findLast { it.loggingCode == code && it.severity == severity }
        assertNotNull(record)

        if (severity == Severity.ERROR)
        {
            checkedForExceptions = true
        }

        return record
    }

    fun verifyNoLogs(code: LoggingCode)
    {
        getLogRecords().any { it.loggingCode == code } shouldBe false
    }

    fun errorLogged(): Boolean
    {
        checkedForExceptions = true
        return getLogRecords().any { it.severity == Severity.ERROR }
    }

    fun getLogRecords(): List<LogRecord>
    {
        logger.waitUntilLoggingFinished()
        return logDestination.logRecords.toList()
    }
    fun clearLogs()
    {
        logger.waitUntilLoggingFinished()
        logDestination.clear()
    }
}