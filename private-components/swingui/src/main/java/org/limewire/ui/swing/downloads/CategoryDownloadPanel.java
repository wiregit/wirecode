package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.FancyDownloadTable;
import org.limewire.ui.swing.table.MouseableTable.MenuHighlightPredicate;
import org.limewire.ui.swing.table.MouseableTable.TableColors;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class CategoryDownloadPanel extends JPanel {


	private Highlighter evenTableHighlighter;
	private Highlighter oddTableHighlighter;
	private JPanel tablePanel = new JPanel(new VerticalLayout());

	private List<DownloadTable> tables = new ArrayList<DownloadTable>();
	private List<JPanel> titles = new ArrayList<JPanel>();
	
	private EventList<DownloadItem> list;
	
	private TableColors colors;
	
	public static CategoryDownloadPanel createCategoryDownloadPanel(EventList<DownloadItem> list){
	    CategoryDownloadPanel panel = new CategoryDownloadPanel(list);
	    panel.addListListener();
	    return panel;
	}

	/**
	 * Create the panel
	 */
	private CategoryDownloadPanel(EventList<DownloadItem> list) {
	    this.list = list;
	    
	    colors = new TableColors();
	    
	  //HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
        evenTableHighlighter = new CompoundHighlighter(
               new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor, colors.evenForeground, colors.selectionColor, colors.selectionForeground),
               new ColorHighlighter(HighlightPredicate.ODD, colors.oddColor, colors.oddForeground, colors.selectionColor, colors.selectionForeground) );
        //oddHighlighter reverses color scheme
        oddTableHighlighter = new CompoundHighlighter(
                new ColorHighlighter(HighlightPredicate.ODD,colors.evenColor, colors.evenForeground, colors.selectionColor, colors.selectionForeground), 
                new ColorHighlighter(HighlightPredicate.EVEN, colors.oddColor, colors.oddForeground, colors.selectionColor, colors.selectionForeground));
      
		setLayout(new BorderLayout());
        JScrollPane pane = new JScrollPane(tablePanel);
        pane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		add(pane);
		 		
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
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
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
		EventList<DownloadItem> filterList = GlazedListsFactory.filterList(list, new DownloadStateMatcher(states));
		
		final DownloadTable table = new FancyDownloadTable(filterList);
		tables.add(table);
		table.addMouseListener(new MultiTableMouseListener());
	
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
	
    /**
     * deselects other tables when a table is selected
     */
    private class MultiTableMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                JTable selectedTable = (JTable) e.getComponent();
                for (JTable table : tables) {
                    if (table != selectedTable) {
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
				
                 table.addHighlighter(new ColorHighlighter(new MenuHighlightPredicate(table),
                        colors.menuRowColor, colors.menuRowForeground, colors.menuRowColor,
                        colors.menuRowForeground));

                length += table.getRowCount();
			}
			
		}
	}

}
