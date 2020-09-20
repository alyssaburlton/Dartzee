package dartzee.game.state

import dartzee.`object`.Dart
import dartzee.core.util.getSqlDateNow
import dartzee.db.BulkInserter
import dartzee.db.DartEntity
import dartzee.db.ParticipantEntity

abstract class AbstractPlayerState<S: AbstractPlayerState<S>>
{
    private val listeners = mutableListOf<PlayerStateListener<S>>()

    abstract val pt: ParticipantEntity
    abstract val completedRounds: MutableList<List<Dart>>
    abstract val currentRound: MutableList<Dart>

    abstract fun getScoreSoFar(): Int

    fun addListener(listener: PlayerStateListener<S>)
    {
        listeners.add(listener)
    }

    /**
     * Helpers
     */
    fun currentRoundNumber() = completedRounds.size + 1

    fun getAllDartsFlattened() = completedRounds.flatten() + currentRound

    fun isHuman() = !pt.isAi()

    /**
     * Modifiers
     */
    fun dartThrown(dart: Dart)
    {
        dart.participantId = pt.rowId
        currentRound.add(dart)

        fireStateChanged()
    }

    fun resetRound()
    {
        currentRound.clear()
        fireStateChanged()
    }

    fun commitRound()
    {
        val entities = currentRound.mapIndexed { ix, drt ->
            DartEntity.factory(drt, pt.playerId, pt.rowId, currentRoundNumber(), ix + 1)
        }

        BulkInserter.insert(entities)

        addCompletedRound(currentRound.toList())
        currentRound.clear()

        fireStateChanged()
    }

    fun addCompletedRound(darts: List<Dart>)
    {
        darts.forEach { it.participantId = pt.rowId }
        this.completedRounds.add(darts.toList())

        fireStateChanged()
    }

    fun setParticipantFinishPosition(finishingPosition: Int)
    {
        pt.finishingPosition = finishingPosition
        pt.saveToDatabase()

        fireStateChanged()
    }

    fun participantFinished(finishingPosition: Int, finalScore: Int)
    {
        pt.finishingPosition = finishingPosition
        pt.finalScore = finalScore
        pt.dtFinished = getSqlDateNow()
        pt.saveToDatabase()

        fireStateChanged()
    }

    protected fun fireStateChanged()
    {
        listeners.forEach { it.stateChanged(this as S) }
    }
}



