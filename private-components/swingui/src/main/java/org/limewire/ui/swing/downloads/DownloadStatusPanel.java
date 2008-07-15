package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.table.DownloadTableModel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.limegroup.gnutella.gui.I18n;



public class DownloadStatusPanel extends JPanel {

	private JTable table;

	
	private JLabel titleLabel;

	/**
	 * Create the panel
	 */
	public DownloadStatusPanel(EventList<DownloadItem> itemList) {
		super();
		setBorder(new LineBorder(Color.BLACK));
		setLayout(new BorderLayout());

		titleLabel = new JLabel();
		add(titleLabel, BorderLayout.NORTH);
		
	

		table = new JTable(new DownloadTableModel(itemList));
		//update title when number of downloads changes
		itemList.addListEventListener(new ListEventListener<DownloadItem>(){

			@Override
			public void listChanged(ListEvent<DownloadItem> listChanges) {
				updateTitle();
			}
			
		});
		//TODO: sorting and filtering
		
//		TableRowSorter<DownloadTableModel> sorter = new TableRowSorter<DownloadTableModel>(
//				model);
//		sorter.setRowFilter(new DownloadStateRowFilter(DownloadState.DOWNLOADING));
		table.setDefaultRenderer(DownloadItem.class, new DownloadStatusPanelRenderer());		
		add(table, BorderLayout.CENTER);
		
		updateTitle();
	}

	private void updateTitle(){
		titleLabel.setText(I18n.tr("Downloads ({0})", table.getRowCount()));
	}
	
	private class DownloadStatusPanelRenderer extends JPanel implements
			TableCellRenderer {
		private JLabel nameLabel = new JLabel();

		private JLabel percentLabel = new JLabel();

		public DownloadStatusPanelRenderer() {
			super(new GridBagLayout());
			setOpaque(false);
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.anchor = GridBagConstraints.WEST;
			nameLabel.setAlignmentY(JLabel.LEFT_ALIGNMENT);
			add(nameLabel, gbc);

			gbc.gridx = 1;
			gbc.weightx = 0;
			gbc.anchor = GridBagConstraints.EAST;
			percentLabel.setAlignmentY(JLabel.RIGHT_ALIGNMENT);
			add(percentLabel, gbc);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			DownloadItem item = (DownloadItem) value;
			if (item.getPercent() >= 100) {
				return null;
			} else {
				nameLabel.setText(item.getTitle());
				percentLabel.setText(item.getPercent() + "%");
				return this;
			}
		}
	}
	
	

}
