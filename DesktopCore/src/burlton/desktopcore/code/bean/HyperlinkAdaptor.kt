package burlton.desktopcore.code.bean

import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class HyperlinkAdaptor(private val listener: HyperlinkListener) : MouseAdapter()
{
    private val listenerWindow = listener as Component

    init
    {
        if (listener !is Component)
        {
            throw Exception("Creating HyperlinkAdaptor with non-component: $listener")
        }
    }

    override fun mouseClicked(arg0: MouseEvent?) = listener.linkClicked(arg0)

    override fun mouseMoved(arg0: MouseEvent?)
    {
        if (listener.isOverHyperlink(arg0))
        {
            listenerWindow.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
        else
        {
            listenerWindow.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        }
    }

    override fun mouseExited(arg0: MouseEvent?)
    {
        listenerWindow.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    }
}
