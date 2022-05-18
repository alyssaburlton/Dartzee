package dartzee.db

import dartzee.core.util.StringUtil
import dartzee.core.util.isEndOfTime
import java.sql.Timestamp

interface IParticipant
{
    var ordinal: Int
    var finishingPosition: Int
    var finalScore: Int
    var dtFinished: Timestamp

    fun saveToDatabase()

    fun isActive() = isEndOfTime(dtFinished)
    fun getFinishingPositionDesc(): String = StringUtil.convertOrdinalToText(finishingPosition)

    fun saveFinishingPosition(game: GameEntity, position: Int)
    {
        this.finishingPosition = position
        this.saveToDatabase()
    }
}