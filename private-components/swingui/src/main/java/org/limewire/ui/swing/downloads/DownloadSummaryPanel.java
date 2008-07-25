package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTableModel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import org.limewire.ui.swing.util.I18n;



public class DownloadSummaryPanel extends JPanel {

	private JTable table;

	
	private JLabel titleLabel;
	private EventList<DownloadItem> allList;
    private EventList<DownloadItem> warningList;
    private EventList<DownloadItem> unfinishedList;

    @Resource
    private Icon warningIcon;

	/**
	 * Create the panel
	 */
	public DownloadSummaryPanel(final EventList<DownloadItem> itemList) {
	    GuiUtils.assignResources(this);

        setLayout(new BorderLayout());
	    
	    this.allList = itemList;
	    unfinishedList = new FilterList<DownloadItem>(itemList, new DownloadStateExcluder(DownloadState.DONE));
        warningList = new FilterList<DownloadItem>(itemList, new DownloadStateMatcher(DownloadState.ERROR, DownloadState.STALLED)); 
		
		titleLabel = new JLabel();
		titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        FontUtils.changeFontStyle(titleLabel, Font.BOLD);
		add(titleLabel, BorderLayout.NORTH);
			

		table = new JTable(new DownloadTableModel(unfinishedList));
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);
		
		//update title when number of downloads changes and hide or show panel as necessary
		allList.addListEventListener(new ListEventListener<DownloadItem>() {

            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
              //list events probably won't be on EDT
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateTitle();
                        adjustVisibility();
                    }
                });
            }

        });
		
		
		//TODO: sorting
		table.setDefaultRenderer(DownloadItem.class, new DownloadStatusPanelRenderer());		
		add(table, BorderLayout.CENTER);
		
		updateTitle();
		adjustVisibility();  
	}
    
	//hide panel if there are no downloads.  show it if there are
    private void adjustVisibility() {
        setVisible(allList.size()>0);
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
		titleLabel.setText(I18n.tr("Downloads ({0})", unfinishedList.size()));
        if(warningList.size()>0){
            titleLabel.setIcon(warningIcon);
        } else {
            titleLabel.setIcon(null);
        }
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
