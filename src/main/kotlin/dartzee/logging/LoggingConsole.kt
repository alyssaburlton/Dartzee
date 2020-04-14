package dartzee.logging

import dartzee.core.util.Debug
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

class LoggingConsole: JFrame(), ILogDestination
{
    private val doc = DefaultStyledDocument()
    private val scrollPane = JScrollPane()
    private val textArea = JTextPane(doc)

    init
    {
        title = "Console"
        setSize(1000, 600)
        setLocationRelativeTo(null)
        contentPane.layout = BorderLayout(0, 0)
        contentPane.add(scrollPane)
        textArea.foreground = Color.GREEN
        textArea.background = Color.BLACK
        textArea.isEditable = false
        scrollPane.setViewportView(textArea)
    }

    override fun log(record: LogRecord)
    {
        val cx = StyleContext()
        val text = record.toString()
        val style = cx.addStyle(text, null)
        if (record.loggingCode == CODE_SQL)
        {
            when
            {
                text.contains("INSERT") -> StyleConstants.setForeground(style, Color.YELLOW)
                text.contains("UPDATE") -> StyleConstants.setForeground(style, Color.YELLOW)
                text.contains("DELETE") -> StyleConstants.setForeground(style, Color.ORANGE)
                else -> StyleConstants.setForeground(style, Color.CYAN)
            }
        }

        if (record.severity == Severity.ERROR)
        {
            StyleConstants.setForeground(style, Color.RED)
        }

        try
        {
            doc.insertString(doc.length, "\n$text", style)
            record.getThrowableStr()?.let { doc.insertString(doc.length, "\n$it", style) }

            textArea.select(doc.length, doc.length)
        }
        catch (ble: BadLocationException)
        {
            Debug.stackTrace(ble, "BLE trying to append: $text")
        }
    }

    fun clear()
    {
        textArea.text = ""
    }
}