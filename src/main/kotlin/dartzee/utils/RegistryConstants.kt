package dartzee.utils

import dartzee.utils.DartsColour.DARTBOARD_BLACK_STR
import dartzee.utils.DartsColour.DARTBOARD_GREEN_STR
import dartzee.utils.DartsColour.DARTBOARD_RED_STR
import dartzee.utils.DartsColour.DARTBOARD_WHITE_STR

//Node names
const val NODE_PREFERENCES = "DartsPrefs"

//Variable names
val PREFERENCES_STRING_ODD_SINGLE_COLOUR = "oddsing;$DARTBOARD_WHITE_STR"
val PREFERENCES_STRING_ODD_DOUBLE_COLOUR = "odddoub;$DARTBOARD_GREEN_STR"
val PREFERENCES_STRING_ODD_TREBLE_COLOUR = "oddtreb;$DARTBOARD_GREEN_STR"
val PREFERENCES_STRING_EVEN_SINGLE_COLOUR = "evensing;$DARTBOARD_BLACK_STR"
val PREFERENCES_STRING_EVEN_DOUBLE_COLOUR = "evendoub;$DARTBOARD_RED_STR"
val PREFERENCES_STRING_EVEN_TREBLE_COLOUR = "eventreb;$DARTBOARD_RED_STR"

const val PREFERENCES_BOOLEAN_AI_AUTO_CONTINUE = "aiauto;true"
const val PREFERENCES_BOOLEAN_CHECK_FOR_UPDATES = "chkupd;true"
const val PREFERENCES_BOOLEAN_SHOW_ANIMATIONS = "anim;true"

const val PREFERENCES_INT_AI_SPEED = "aispd;1000"
const val PREFERENCES_INT_LEADERBOARD_SIZE = "ldbrdsz;50"

const val PREFERENCES_DOUBLE_HUE_FACTOR = "huefactor;0.8"
const val PREFERENCES_DOUBLE_FG_BRIGHTNESS = "fgbri;0.5"
const val PREFERENCES_DOUBLE_BG_BRIGHTNESS = "bgbri;1"