package dartzee.main

import dartzee.`object`.DartsClient
import dartzee.core.util.CoreRegistry.INSTANCE_STRING_DEVICE_ID
import dartzee.core.util.CoreRegistry.INSTANCE_STRING_USER_NAME
import dartzee.core.util.CoreRegistry.instance
import dartzee.core.util.Debug
import dartzee.core.util.DebugUncaughtExceptionHandler
import dartzee.core.util.DialogUtil
import dartzee.core.util.MessageDialogFactory
import dartzee.logging.*
import dartzee.screen.ScreenCache
import dartzee.utils.DARTS_VERSION_NUMBER
import dartzee.utils.DartsDebugExtension
import dartzee.utils.InjectedThings.logger
import java.util.*
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    DartsClient.parseProgramArguments(args)

    if (!DartsClient.trueLaunch && !DartsClient.isAppleOs())
    {
        Runtime.getRuntime().exec("cmd /c start javaw -Xms256m -Xmx512m -jar Dartzee.jar trueLaunch")
        exitProcess(0)
    }

    Debug.initialise(ScreenCache.debugConsole)
    setLoggingContextFields()

    DialogUtil.init(MessageDialogFactory())

    setLookAndFeel()

    Debug.debugExtension = DartsDebugExtension()
    Debug.logToSystemOut = true

    val mainScreen = ScreenCache.mainScreen
    Thread.setDefaultUncaughtExceptionHandler(DebugUncaughtExceptionHandler())

    DartsClient.logArgumentState()

    DartsClient.checkForUpdatesIfRequired()

    mainScreen.isVisible = true
    mainScreen.init()
}

private fun setLoggingContextFields()
{
    logger.addToContext(KEY_USERNAME, getUsername())
    logger.addToContext(KEY_APP_VERSION, DARTS_VERSION_NUMBER)
    logger.addToContext(KEY_OPERATING_SYSTEM, DartsClient.operatingSystem)
    logger.addToContext(KEY_DEVICE_ID, getDeviceId())
    logger.addToContext(KEY_DEV_MODE, DartsClient.devMode)
}

private fun getDeviceId() = instance.get(INSTANCE_STRING_DEVICE_ID, null) ?: setDeviceId()
private fun setDeviceId(): String
{
    val deviceId = UUID.randomUUID().toString()
    instance.put(INSTANCE_STRING_DEVICE_ID, deviceId)
    return deviceId
}

private fun getUsername() = instance.get(INSTANCE_STRING_USER_NAME, null) ?: setUsername()
private fun setUsername(): String
{
    logger.info(CODE_USERNAME_UNSET, "No username found, prompting for one now")

    var username: String? = null
    while (username == null || username.isEmpty())
    {
        username = JOptionPane.showInputDialog(null, "Please enter your name (for debugging purposes).\nThis will only be asked for once.", "Enter your name")
    }

    logger.info(CODE_USERNAME_SET, "$username has set their username", KEY_USERNAME to username)
    instance.put(INSTANCE_STRING_USER_NAME, username)
    return username
}

private fun setLookAndFeel()
{
    if (DartsClient.isAppleOs())
    {
        setLookAndFeel("javax.swing.plaf.metal")
    }
    else
    {
        setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
    }
}

private fun setLookAndFeel(laf: String)
{
    try
    {
        UIManager.setLookAndFeel(laf)
    }
    catch (e: Throwable)
    {
        logger.error(CODE_LOOK_AND_FEEL_ERROR, "Failed to load laf $laf", e)
        DialogUtil.showError("Failed to load Look & Feel 'Nimbus'.")
    }

    logger.info(CODE_LOOK_AND_FEEL_SET, "Set look and feel to $laf")
}
