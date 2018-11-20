package code.screen.game;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Beans;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import code.db.DartsMatchEntity;
import code.db.GameEntity;
import code.db.ParticipantEntity;
import code.db.PlayerEntity;
import object.HandyArrayList;
import object.SuperHashMap;

/**
 * The first tab displayed for any match. Provides a summary of the players' overall scores with (hopefully) nice graphs and stuff
 */
public class MatchSummaryPanelMk2 extends PanelWithScorers<MatchScorer>
								  implements ActionListener
{
	private SuperHashMap<Long, MatchScorer> hmPlayerIdToScorer = new SuperHashMap<>();
	private HandyArrayList<ParticipantEntity> participants = new HandyArrayList<>();
	private DartsMatchEntity match = null;
	
	public MatchSummaryPanelMk2() 
	{
		super();
		
		refreshPanel.add(btnRefresh);
		btnRefresh.addActionListener(this);
		btnRefresh.setPreferredSize(new Dimension(80, 80));
		btnRefresh.setIcon(new ImageIcon(DartsGamePanel.class.getResource("/buttons/Refresh.png")));
		btnRefresh.setToolTipText("Refresh stats");
	}
	
	private GameStatisticsPanel statsPanel;
	private final JPanel refreshPanel = new JPanel();
	private final JButton btnRefresh = new JButton();
	
	public void init(DartsMatchEntity match)
	{
		this.match = match;
		
		statsPanel = factoryStatsPanel();
		statsPanel.setGameParams(match.getGameParams());
		
		if (statsPanel != null)
		{
			panelCenter.add(statsPanel, BorderLayout.CENTER);
			panelCenter.add(refreshPanel, BorderLayout.SOUTH);
		}
		
		HandyArrayList<PlayerEntity> players = match.getPlayers();
		
		int totalPlayers = players.size();
		initScorers(totalPlayers);
		
		for (PlayerEntity player : players)
		{
			long playerId = player.getRowId();
			MatchScorer scorer = assignScorer(player, hmPlayerIdToScorer, playerId, "");
			scorer.setMatch(match);
		}
	}
	
	
	
	public void addParticipant(long gameId, ParticipantEntity participant)
	{
		long playerId = participant.getPlayerId();
		MatchScorer scorer = hmPlayerIdToScorer.get(playerId);
		
		Object[] row = {gameId, participant, participant, participant};
		scorer.addRow(row);
		
		participants.add(participant);
	}
	public void updateTotalScores()
	{
		ArrayList<MatchScorer> scorers = hmPlayerIdToScorer.getValuesAsVector();
		for (MatchScorer scorer : scorers)
		{
			scorer.updateResult();
		}
		
		updateStats();
	}
	public void updateStats()
	{
		if (statsPanel != null)
		{
			statsPanel.showStats(participants);
		}
	}

	@Override
	public MatchScorer factoryScorer()
	{
		return new MatchScorer();
	}
	
	private GameStatisticsPanel factoryStatsPanel()
	{
		int type = match.getGameType();
		if (type == GameEntity.GAME_TYPE_X01)
		{
			return new MatchStatisticsPanelX01();
		}
		else if (type == GameEntity.GAME_TYPE_GOLF)
		{
			return new MatchStatisticsPanelGolf();
		}
		else if (type == GameEntity.GAME_TYPE_ROUND_THE_CLOCK)
		{
			return new MatchStatisticsPanelRoundTheClock();
		}
		
		if (Beans.isDesignTime())
		{
			return new MatchStatisticsPanelX01();
		}
		
		return null;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		updateStats();
	}
}
