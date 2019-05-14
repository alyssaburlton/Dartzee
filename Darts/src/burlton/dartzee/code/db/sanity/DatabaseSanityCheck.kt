package burlton.dartzee.code.db.sanity

import burlton.core.code.util.Debug
import burlton.dartzee.code.screen.ScreenCache
import burlton.dartzee.code.utils.DartsDatabaseUtil
import burlton.desktopcore.code.bean.ScrollTableButton
import burlton.desktopcore.code.screen.TableModelDialog
import burlton.desktopcore.code.util.DialogUtil
import burlton.desktopcore.code.util.TableUtil.DefaultModel
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.table.DefaultTableModel

fun getAllSanityChecks(): List<AbstractSanityCheck>
{
    //Bog standard checks
    val checks = mutableListOf(
            SanityCheckFinishedParticipantsNoScore(),
            SanityCheckDuplicateDarts(),
            SanityCheckColumnsThatAllowDefaults(),
            SanityCheckUnfinishedGamesNoActiveParticipants(),
            SanityCheckDuplicateMatchOrdinals(),
            SanityCheckFinalScoreX01(),
            SanityCheckFinalScoreGolf(),
            SanityCheckFinalScoreRtc())

    //Checks that run on all entities
    DartsDatabaseUtil.getAllEntities().forEach{
        checks.add(SanityCheckUnsetOrHangingFields(it))
    }

    //Do this last in case we leave temp tables lying around from other checks
    checks.add(SanityCheckUnexpectedTables())
    return checks
}

object DatabaseSanityCheck
{
    private val sanityErrors = mutableListOf<AbstractSanityCheckResult>()

    fun runSanityCheck()
    {
        Debug.appendBanner("RUNNING SANITY CHECK")

        sanityErrors.clear()

        getAllSanityChecks().forEach{
            val results = it.runCheck()
            sanityErrors.addAll(results)
        }

        val tm = buildResultsModel()
        if (tm.rowCount > 0)
        {
            val showResults = object : AbstractAction()
            {
                override fun actionPerformed(e: ActionEvent)
                {
                    val modelRow = Integer.valueOf(e.actionCommand)

                    val result = sanityErrors[modelRow]
                    showResultsBreakdown(result)
                }
            }

            val autoFix = object : AbstractAction()
            {
                override fun actionPerformed(e: ActionEvent)
                {
                    val modelRow = Integer.valueOf(e.actionCommand)

                    val result = sanityErrors[modelRow]
                    result.autoFix()
                }
            }

            val table = ScrollTableButton(tm)
            table.setButtonColumn(2, showResults)
            table.setButtonColumn(3, autoFix)

            val dlg = TableModelDialog("Sanity Results", table)
            dlg.setColumnWidths("-1;50;150;150")
            dlg.setLocationRelativeTo(ScreenCache.getMainScreen())
            dlg.isVisible = true
        }
        else
        {
            DialogUtil.showInfo("Sanity check completed and found no issues")
        }
    }

    private fun buildResultsModel(): DefaultTableModel
    {
        val model = DefaultModel()
        model.addColumn("Description")
        model.addColumn("Count")
        model.addColumn("")
        model.addColumn("")

        for (result in sanityErrors)
        {
            val row = arrayOf(result.getDescription(), result.getCount(), "View Results >", "Auto-fix")
            model.addRow(row)
        }

        return model
    }

    private fun showResultsBreakdown(result: AbstractSanityCheckResult)
    {
        val dlg = result.getResultsDialog()
        dlg.setSize(800, 600)
        dlg.setLocationRelativeTo(ScreenCache.getMainScreen())
        dlg.isVisible = true
    }
}
