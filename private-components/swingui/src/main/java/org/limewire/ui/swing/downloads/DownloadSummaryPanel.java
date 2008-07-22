package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.table.DownloadTableModel;
import org.limewire.ui.swing.util.FontUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import org.limewire.ui.swing.util.I18n;



public class DownloadSummaryPanel extends JPanel {

	private JTable table;

	
	private JLabel titleLabel;
	private EventList<DownloadItem> itemList;

	/**
	 * Create the panel
	 */
	public DownloadSummaryPanel(final EventList<DownloadItem> itemList) {
	    this.itemList = itemList;
		setLayout(new BorderLayout());
		
		titleLabel = new JLabel();
        FontUtils.changeStyle(titleLabel, Font.BOLD);
		add(titleLabel, BorderLayout.NORTH);
			

		table = new JTable(new DownloadTableModel(itemList));
		table.setShowHorizontalLines(false);
		//update title when number of downloads changes and hide or show panel as necessary
		itemList.addListEventListener(new ListEventListener<DownloadItem>(){

			@Override
			public void listChanged(ListEvent<DownloadItem> listChanges) {
				updateTitle();
				adjustVisibility();  
			}
			
		});
		//TODO: sorting and filtering
		
//		TableRowSorter<DownloadTableModel> sorter = new TableRowSorter<DownloadTableModel>(
//				model);
//		sorter.setRowFilter(new DownloadStateRowFilter(DownloadState.DOWNLOADING));
		table.setDefaultRenderer(DownloadItem.class, new DownloadStatusPanelRenderer());		
		add(table, BorderLayout.CENTER);
		
		updateTitle();
		adjustVisibility();  
	}
    
	//hide panel if there are no downloads.  show it if there are
    private void adjustVisibility() {
        setVisible(itemList.size()>0);
    }

    //Overridden to add listener to all components in this container so that mouse clicks will work anywhere
    @Override
    public void addMouseListener(MouseListener listener){
        super.addMouseListener(listener);
        for(Component comp : getComponents()){
            comp.addMouseListener(listener);
        }
    }

	private void updateTitle(){
		titleLabel.setText(I18n.tr("Downloads ({0})", itemList.size()));
	}
	
	private class DownloadStatusPanelRenderer extends JPanel implements
			TableCellRenderer {
		private JLabel nameLabel = new JLabel();

		private JLabel percentLabel = new JLabel();

		public DownloadStatusPanelRenderer() {
			super(new GridBagLayout());

	        FontUtils.changeFontSize(nameLabel, -1);
	        FontUtils.changeFontSize(percentLabel, -1);
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
			if (item.getPercentComplete() >= 100) {
				return null;
			} else {
				nameLabel.setText(item.getTitle());
				percentLabel.setText(item.getPercentComplete() + "%");
				return this;
			}
		}
	}
	
	

}
