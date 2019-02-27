package burlton.dartzee.code.db.sanity;

import burlton.core.code.obj.HandyArrayList;
import burlton.core.code.util.StringUtil;
import burlton.dartzee.code.bean.TableModelEntity;
import burlton.dartzee.code.db.AbstractEntity;
import burlton.dartzee.code.utils.DatabaseUtil;
import burlton.desktopcore.code.bean.ScrollTable;
import burlton.desktopcore.code.util.DialogUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.List;

public abstract class AbstractSanityCheckResultEntities extends AbstractSanityCheckResult
{
	private List<? extends AbstractEntity<?>> entities;

	/**
	 * Constructor
	 */
	public AbstractSanityCheckResultEntities(List<? extends AbstractEntity<?>> entities)
	{
		this.entities = entities;
	}
	
	@Override
	protected DefaultTableModel getResultsModel()
	{
		return new TableModelEntity(entities);
	}
	
	@Override
	public Action getDeleteAction(ScrollTable t)
	{
		return new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				int[] rows = t.getSelectedModelRows();
				if (rows.length == 0)
				{
					return;
				}
				
				int ans = DialogUtil.showQuestion("Are you sure you want to delete " + rows.length + " row(s) from " + getEntityName() + "?", false);
				if (ans == JOptionPane.YES_OPTION)
				{
					boolean success = deleteSelectedRows(t, rows);
					if (!success)
					{
						DialogUtil.showError("An error occurred deleting the rows. You should re-run the sanity check and check logs.");
					}
				}
			}
		};
	}
	private boolean deleteSelectedRows(ScrollTable t, int[] selectedRows)
	{
		HandyArrayList<Long> rowIds = new HandyArrayList<>();
		for (int i=0; i<selectedRows.length; i++)
		{
			long rowId = (long)t.getValueAt(selectedRows[i], 0);
			rowIds.add(rowId);
		}
		
		HandyArrayList<HandyArrayList<Long>> batches = HandyArrayList.getBatches(rowIds, 50);
		boolean success = true;
		for (HandyArrayList<Long> batch : batches)
		{
			String idStr = StringUtil.toDelims(batch, ", ");
			String sql = "DELETE FROM " + getEntityName() + " WHERE RowId IN (" + idStr + ")";
			success = DatabaseUtil.executeUpdate(sql);
		}
		
		return success;
	}
	
	public List<? extends AbstractEntity<?>> getEntities()
	{
		return entities;		
	}	
	public String getEntityName()
	{
		return entities.get(0).getTableName();
	}
	@Override public int getCount()
	{
		return entities.size();
	}
}
