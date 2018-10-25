package code.screen.game;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import code.db.ParticipantEntity;
import code.object.Dart;
import code.utils.DartsColour;
import code.utils.X01Util;
import object.HandyArrayList;

/**
 * Shows running stats for X01 games - three-dart average, checkout % etc.
 */
public final class GameStatisticsPanelX01 extends GameStatisticsPanel
{
	private HandyArrayList<String> playerNamesOrdered = new HandyArrayList<>();
	
	@Override
	protected void buildTableModel()
	{
		DefaultTableModel tm = new DefaultTableModel();
		tm.addColumn("");
		
		for (ParticipantEntity pt : participants)
		{
			String playerName = pt.getPlayerName();
			playerNamesOrdered.addUnique(playerName);
			tm.addColumn(playerName);
		}
		
		int rowSize = playerNamesOrdered.size() + 1;
		
		//3-dart average
		Object[] threeDartAvgs = new Object[rowSize];
		threeDartAvgs[0] = "3-dart avg";
		for (int i=0; i<playerNamesOrdered.size(); i++)
		{
			String playerName = playerNamesOrdered.get(i);
			ArrayList<HandyArrayList<Dart>> rounds = hmPlayerToDarts.get(playerName);
			HandyArrayList<Dart> darts = HandyArrayList.flattenBatches(rounds);
			
			double avg = X01Util.calculateThreeDartAverage(darts, 140);
			int p1 = (int)(100 * avg);
			avg = (double)p1/100;
			
			threeDartAvgs[i+1] = avg;
			
		}
		
		tm.addRow(getScoreRow(i -> i.max().getAsInt(), "Highest Score"));
		tm.addRow(threeDartAvgs);
		tm.addRow(getScoreRow(i -> i.min().getAsInt(), "Lowest Score"));
		tm.addRow(getMissesRow());
		
		tm.addRow(new Object[getRowWidth()]);
		
		tm.addRow(getScoresBetween(180, 181, "180"));
		tm.addRow(getScoresBetween(140, 180, "140 - 179"));
		tm.addRow(getScoresBetween(100, 140, "100 - 139"));
		tm.addRow(getScoresBetween(80, 100, "80 - 99"));
		tm.addRow(getScoresBetween(60, 80, "60 - 79"));
		tm.addRow(getScoresBetween(40, 60, "40 - 59"));
		tm.addRow(getScoresBetween(20, 40, "20 - 39"));
		tm.addRow(getScoresBetween(0, 20, "0 - 19"));
		
		tm.addRow(new Object[getRowWidth()]);
		
		tm.addRow(getCheckoutPercentRow());
		
		table.setRowHeight(20);
		table.setModel(tm);
		table.disableSorting();
		
		table.setColumnWidths("120");
		
		//Rendering
		for (int i=0; i<getRowWidth(); i++)
		{
			table.getColumn(i).setCellRenderer(new ScorerRenderer());
		}
	}
	
	private Object[] getCheckoutPercentRow()
	{
		Object[] row = new Object[getRowWidth()];
		row[0] = "Checkout %";
		
		for (int i=0; i<playerNamesOrdered.size(); i++)
		{
			String playerName = playerNamesOrdered.get(i);
			HandyArrayList<Dart> darts = getFlattenedDarts(playerName);
			
			HandyArrayList<Dart> potentialFinishers = darts.createFilteredCopy(d -> X01Util.isCheckoutDart(d));
			HandyArrayList<Dart> actualFinishes = potentialFinishers.createFilteredCopy(d -> d.isDouble() && (d.getTotal() == d.getStartingScore()));
			
			if (actualFinishes.isEmpty())
			{
				row[i+1] = "N/A";
			}
			else
			{
				int p1 = 10000 * actualFinishes.size() / potentialFinishers.size();
				double percent = (double)p1/100;
				
				row[i+1] = percent;
			}
			
			
		}
		
		return row;
	}
	
	private Object[] getScoresBetween(int min, int max, String desc)
	{
		Object[] row = new Object[getRowWidth()];
		row[0] = desc;
		
		for (int i=0; i<playerNamesOrdered.size(); i++)
		{
			String playerName = playerNamesOrdered.get(i);
			HandyArrayList<HandyArrayList<Dart>> rounds = getScoringRounds(playerName);
			
			HandyArrayList<HandyArrayList<Dart>> bigRounds = rounds.createFilteredCopy(r -> X01Util.sumScore(r) >= min && X01Util.sumScore(r) < max);
			
			row[i+1] = bigRounds.size();
		}
		
		return row;
	}
	
	private Object[] getScoreRow(Function<IntStream, Integer> f, String desc)
	{
		Object[] row = new Object[getRowWidth()];
		row[0] = desc;
		
		for (int i=0; i<playerNamesOrdered.size(); i++)
		{
			String playerName = playerNamesOrdered.get(i);
			ArrayList<HandyArrayList<Dart>> rounds = getScoringRounds(playerName);
			
			IntStream roundsAsTotal = rounds.stream().mapToInt(rnd -> X01Util.sumScore(rnd));
			row[i+1] = f.apply(roundsAsTotal);
		}
		
		return row;
	}
	
	private Object[] getMissesRow()
	{
		Object[] row = new Object[getRowWidth()];
		row[0] = "Miss %";
		
		for (int i=0; i<playerNamesOrdered.size(); i++)
		{
			String playerName = playerNamesOrdered.get(i);
			HandyArrayList<Dart> scoringDarts = getScoringDarts(playerName);
			HandyArrayList<Dart> misses = scoringDarts.createFilteredCopy(d -> d.getMultiplier() == 0);
			
			int p1 = 10000 * misses.size() / scoringDarts.size();
			double percent = (double)p1/100;
			
			row[i+1] = percent;
		}
		
		return row;
	}
	
	private HandyArrayList<HandyArrayList<Dart>> getScoringRounds(String playerName)
	{
		HandyArrayList<HandyArrayList<Dart>> rounds = hmPlayerToDarts.get(playerName);
		return rounds.createFilteredCopy(r -> r.lastElement().getStartingScore() > 140);
	}
	
	private HandyArrayList<Dart> getFlattenedDarts(String playerName)
	{
		ArrayList<HandyArrayList<Dart>> rounds = hmPlayerToDarts.get(playerName);
		return HandyArrayList.flattenBatches(rounds);
	}
	
	private HandyArrayList<Dart> getScoringDarts(String playerName)
	{
		HandyArrayList<Dart> darts = getFlattenedDarts(playerName);
		return X01Util.getScoringDarts(darts, 140);
	}
	
	private int getRowWidth()
	{
		return playerNamesOrdered.size() + 1;
	}
	
	
	private static class ScorerRenderer extends DefaultTableCellRenderer
	{
        @Override
        public Component getTableCellRendererComponent(JTable table, Object
            value, boolean isSelected, boolean hasFocus, int row, int column) 
        {
    		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    		setHorizontalAlignment(SwingConstants.CENTER);
    		
    		if (column == 0)
    		{
    			setFont(new Font("Trebuchet MS", Font.BOLD, 15));
    		}
    		else
    		{
    			setFont(new Font("Trebuchet MS", Font.PLAIN, 15));
    		}
    		
    		setColours(table, row, column);
    		return this;
        }
        
        private void setColours(JTable table, int row, int column)
        {
        	if (column == 0)
        	{
        		//Do nothing
        		setForeground(null);
        		setBackground(Color.WHITE);
        		return;
        	}
        	
        	TableModel tm = table.getModel();
        	if (row >= 0
        	  && row <= 2)
        	{
        		//double score = getDoubleAt(tm, row, column);
        		//Color fg = DartsColour.getScorerForegroundColour(score);
            	//Color bg = DartsColour.getScorerBackgroundColour(score);
            	//setForeground(fg);
            	//setBackground(bg);
        		
        		int pos = getPositionForColour(tm, row, column, true);
        		DartsColour.setFgAndBgColoursForPosition(this, pos);
        	}
        	else if (row == 3)
        	{
        		int pos = getPositionForColour(tm, row, column, false);
        		DartsColour.setFgAndBgColoursForPosition(this, pos);
        	}
        	else if (row == 14)
        	{
        		int pos = getPositionForColour(tm, row, column, true);
        		DartsColour.setFgAndBgColoursForPosition(this, pos);
        	}
        	else if (row >= 5
        	  && row <= 12)
        	{
        		int sum = getHistogramSum(tm, column);
        		
        		double thisValue = getDoubleAt(tm, row, column);
        		float percent = (float)thisValue / sum;
        		
        		Color bg = Color.getHSBColor((float)0.5, percent, 1);
        		
        		setForeground(null);
        		setBackground(bg);
        	}
        	else
        	{
        		setForeground(null);
        		setBackground(Color.WHITE);
        	}
        }
        
        private double getDoubleAt(TableModel tm, int row, int col)
        {
        	Number thisValue = (Number)tm.getValueAt(row, col);
    		return thisValue.doubleValue();
        }
        
        private int getPositionForColour(TableModel tm, int row, int col, boolean highestWins)
        {
        	if (tm.getValueAt(row, col) instanceof String)
        	{
        		return -1;
        	}
        	
        	double myScore = getDoubleAt(tm, row, col);
        	
        	int myPosition = 1;
        	for (int i=1; i<tm.getColumnCount(); i++)
        	{
        		if (i == col)
        		{
        			continue;
        		}
        		
        		double theirScore = getDoubleAt(tm, row, i);
        		
        		//Compare positivity to the boolean
        		int result = Double.compare(theirScore, myScore);
        		if ((result > 0) == highestWins
        		  && result != 0)
        		{
        			myPosition++;
        		}
        	}
        	
        	return myPosition;
        }
        
        private int getHistogramSum(TableModel tm, int col)
        {
        	int sum = 0;
        	for (int i=5; i<= 12; i++)
        	{
        		sum += (int)tm.getValueAt(i, col);
        	}

        	return sum;
        }
    }
}
