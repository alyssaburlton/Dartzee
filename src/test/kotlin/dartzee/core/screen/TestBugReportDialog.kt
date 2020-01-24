package dartzee.core.screen

import dartzee.core.helper.exceptionLogged
import dartzee.core.helper.getLogs
import dartzee.core.util.Debug
import dartzee.core.util.DebugExtension
import dartzee.helper.AbstractTestWithUsername
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TestBugReportDialog: AbstractTestWithUsername()
{
    private val ext = Debug.debugExtension

    override fun afterEachTest()
    {
        super.afterEachTest()

        Debug.debugExtension = ext
    }

    @Test
    fun `Should enforce a non-empty description`()
    {
        val dlg = BugReportDialog()

        dlg.btnOk.doClick()

        dialogFactory.errorsShown.shouldContainExactly("You must enter a description.")
    }

    @Test
    fun `Should show a message if sending the email fails`()
    {
        val ext = mockk<DebugExtension>(relaxed = true)
        every { ext.sendEmail(any(), any()) } throws Exception("Not again")

        Debug.debugExtension = ext

        val dlg = BugReportDialog()
        dlg.descriptionField.text = "Foo"
        dlg.btnOk.doClick()

        dialogFactory.infosShown.shouldContainExactly("Unable to send bug report. Please check your internet connection and try again.")
        exceptionLogged() shouldBe true
        getLogs() shouldContain "Unable to send Bug Report. Exceptions follow."
    }

    @Test
    fun `Should send a bug report when Ok is pressed`()
    {
        val ext = mockk<DebugExtension>(relaxed = true)

        Debug.positionLastEmailed = 0
        Debug.debugExtension = ext

        val dlg = BugReportDialog()
        dlg.descriptionField.text = "Description"
        dlg.textPaneReplicationSteps.text = "Some steps"
        dlg.btnOk.doClick()

        verify { ext.sendEmail("BUG REPORT: Description - TestUser", any()) }
        dialogFactory.infosShown.shouldContainExactly("Bug report submitted.")
    }
}