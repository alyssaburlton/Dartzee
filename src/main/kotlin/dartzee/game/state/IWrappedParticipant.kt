package dartzee.game.state

import dartzee.db.IParticipant
import dartzee.db.ParticipantEntity
import dartzee.db.TeamEntity
import dartzee.game.ParticipantName
import dartzee.game.UniqueParticipantName

/**
 * Wraps up either a Team or an individual Participant, granting access to either:
 *
 *  - The individual player of a round, e.g. to save their darts, unlock achievements and so on
 *  - The top-level "participant" (a player or team), for checking/setting finishing position, score etc
 */
sealed interface IWrappedParticipant
{
    val individuals: List<ParticipantEntity>
    val participant: IParticipant

    fun getIndividual(roundNumber: Int): ParticipantEntity
    fun getUniqueParticipantName() = UniqueParticipantName(individuals.map { it.getPlayerName() }.sorted().joinToString(" & "))
    fun getParticipantName() = ParticipantName(individuals.joinToString(" & ") { it.getPlayerName() })
}

class SingleParticipant(override val participant: ParticipantEntity): IWrappedParticipant
{
    override val individuals = listOf(participant)

    override fun getIndividual(roundNumber: Int) = participant
}

class TeamParticipant(override val participant: TeamEntity, override val individuals: List<ParticipantEntity>): IWrappedParticipant
{
    private val teamSize = individuals.size

    override fun getIndividual(roundNumber: Int) = individuals[(roundNumber - 1) % teamSize]
}