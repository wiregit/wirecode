package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class CategoryDownloadPanel extends JPanel {

	
	
    @Resource
    private Color oddColor;
    @Resource
    private Color oddForeground;
    @Resource
    private Color evenColor;
    @Resource
    private Color evenForeground;
	private Highlighter evenTableHighlighter;
	private Highlighter oddTableHighlighter;
	private JPanel tablePanel = new JPanel(new VerticalLayout());

	private List<DownloadTable> tables = new ArrayList<DownloadTable>();
	private List<JPanel> titles = new ArrayList<JPanel>();
	
	private EventList<DownloadItem> list;
	
	public static CategoryDownloadPanel createCategoryDownloadPanel(EventList<DownloadItem> list){
	    CategoryDownloadPanel panel = new CategoryDownloadPanel(list);
	    panel.addListListener();
	    return panel;
	}

	/**
	 * Create the panel
	 */
	private CategoryDownloadPanel(EventList<DownloadItem> list) {
	    GuiUtils.assignResources(this);
	    this.list = list;
	  //HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
        evenTableHighlighter = new CompoundHighlighter(new ColorHighlighter(HighlightPredicate.EVEN, evenColor, evenForeground, evenColor, evenForeground),
                new ColorHighlighter(HighlightPredicate.ODD, oddColor, oddForeground, oddColor, oddForeground));
        //oddHighlighter reverses color scheme
		oddTableHighlighter = new CompoundHighlighter(new ColorHighlighter(HighlightPredicate.EVEN, oddColor, oddForeground, oddColor, oddForeground) ,
		        new ColorHighlighter(HighlightPredicate.ODD, evenColor, evenForeground, evenColor, evenForeground));
		setLayout(new BorderLayout());
		add(new JScrollPane(tablePanel));
		 		
		addTable(list, I18n.tr("Finished"), DownloadState.DONE,
				DownloadState.FINISHING);
		addTable(list, I18n.tr("Active"),
				DownloadState.DOWNLOADING, DownloadState.CONNECTING);
		addTable(list, I18n.tr("Inactive"), DownloadState.PAUSED,
				DownloadState.REMOTE_QUEUED, DownloadState.LOCAL_QUEUED);
		addTable(list, I18n.tr("Unsuccessful"),
				DownloadState.ERROR, DownloadState.STALLED);
		
		updateStriping();
	}
	
	private void addListListener(){
	    list.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                if (isVisible()) {
                    for (int i = 0; i < titles.size() && i < tables.size(); i++) {
                        boolean isVisible = tables.get(i).getRowCount() > 0;
                        titles.get(i).setVisible(isVisible);
                    }
                    // TODO: should update striping when necessary. this
                    // currently fires when any download item is updated
                    updateStriping();
                }
            }
        });
	}

	/**
	 * Creates a DownloadTable and adds it to a collapsible pane with the title 
	 * 
	 * @param model the model of the table
	 * @param title the title of the panel displaying this table
	 * @param state
	 */
	private void addTable(EventList<DownloadItem> list, String title,
			DownloadState... states) {
		final JXCollapsiblePane collapsePane = new JXCollapsiblePane();
		collapsePane.setLayout(new BorderLayout());
		EventList<DownloadItem> filterList = new FilterList<DownloadItem>(list, new DownloadStateMatcher(states));
		
		final DownloadTable table = new DownloadTable(filterList);
		tables.add(table);
		table.getSelectionModel().addListSelectionListener(new MultiTableSelectionListener());
	
		collapsePane.add(table, BorderLayout.CENTER);

		
		final JPanel titlePanel = new JPanel(new VerticalLayout());
		JLabel titleLabel = new JLabel(title);
		//Set title labels to collapse their tables
		titleLabel.addMouseListener(getClickListener(collapsePane
				.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION)));
		titlePanel.add(titleLabel);
		titlePanel.add(new JSeparator());
		titles.add(titlePanel);
		tablePanel.add(titlePanel);
		tablePanel.add(collapsePane);		
	}

	/**
	 * Converts actionListener to mouse listener with hand cursor on mouseover.
	 */
	private MouseListener getClickListener(final ActionListener actionListener) {
		return new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				JComponent comp = (JComponent) e.getComponent();
				comp.getTopLevelAncestor().setCursor(
						Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				JComponent comp = (JComponent) e.getComponent();
				comp.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				actionListener.actionPerformed(new ActionEvent(
						e.getComponent(), 0, null));
			}
		};
	}
	
    private class MultiTableSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) // Ignore.
				return;

			if (e.getFirstIndex() != -1) { // Something is selected!
				ListSelectionModel sourceSelectionModel = (ListSelectionModel) e.getSource();

				for (JTable table : tables) {
					if (table.getSelectionModel() != sourceSelectionModel) {
						table.clearSelection();
					}
				}
			}
		}
	}
    

    private void updateStriping() {
		int length = 0;
		
		for (DownloadTable table : tables) {
						
			if (table.getRowCount() > 0 && table.isVisible()) {
				
				if (length % 2 == 0) {
					table.setHighlighters(evenTableHighlighter);
				} else {
					table.setHighlighters(oddTableHighlighter);
				}
				

                table.addMenuRowHighlighter();
				
				length += table.getRowCount();
			}
			
		}
	}

}
