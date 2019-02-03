package burlton.dartzee.code.ai;

import burlton.core.code.util.Debug;
import burlton.dartzee.code.db.GameEntityKt;
import burlton.dartzee.code.object.Dart;
import burlton.dartzee.code.screen.Dartboard;
import burlton.dartzee.code.utils.X01UtilKt;

/**
 * Simulate a single game of X01 for an AI
 */
public class DartsSimulationX01 extends AbstractDartsSimulation
{
	private static final int X01 = 501;
	
	//Transient things
	protected int startingScore = -1;
	protected int currentScore = -1;
	
	public DartsSimulationX01(Dartboard dartboard, AbstractDartsModel model)
	{
		super(dartboard, model);
	}
	
	@Override
	public String getGameParams()
	{
		return "" + X01;
	}
	
	@Override
	public int getGameType()
	{
		return GameEntityKt.GAME_TYPE_X01;
	}
	
	@Override
	public int getTotalScore()
	{
		int totalRounds = currentRound - 1;
		return ((totalRounds - 1) * 3) + dartsThrown.size();
	}
	
	@Override
	public boolean shouldPlayCurrentRound()
	{
		return currentScore > 0;
	}
	
	@Override
	protected void resetVariables()
	{
		super.resetVariables();
		startingScore = X01;
		currentScore = X01;
	}
	
	@Override
	public void startRound()
	{
		//Starting a new round. Need to keep track of what we started on so we can reset if we bust.
		startingScore = currentScore;
		resetRound();
		
		model.throwX01Dart(currentScore, dartboard);
	}
	
	private void finishedRound()
	{
		hmRoundNumberToDarts.put(currentRound, dartsThrown);
		
		//If we've bust, then reset the current score back
		if (X01UtilKt.isBust(currentScore, dartsThrown.lastElement()))
		{
			currentScore = startingScore;
		}
		
		Debug.appendBanner("Round " + currentRound, logging);
		Debug.append("StartingScore [" + startingScore + "]", logging);
		Debug.append("Darts [" + dartsThrown + "]", logging);
		Debug.append("CurrentScore [" + currentScore + "]", logging);
		
		currentRound++;
	}
	
	@Override
	public void dartThrown(Dart dart)
	{
		dartsThrown.add(dart);
		dart.setStartingScore(currentScore);
		
		int dartTotal = dart.getTotal();
		currentScore = currentScore - dartTotal;
		
		if (currentScore <= 1
		  || dartsThrown.size() == 3
		  || X01UtilKt.shouldStopForMercyRule(model, startingScore, currentScore))
		{
			finishedRound();
		}
		else
		{
			model.throwX01Dart(currentScore, dartboard);
		}
	}
}
