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

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


public class CategoryDownloadPanel extends JPanel {

	
	
//TODO: move these out of here
	private Color oddColor = Color.WHITE;
	private Color evenColor = Color.LIGHT_GRAY;
	private Highlighter evenHighlighter;
	private Highlighter oddHighlighter;
	private JPanel tablePanel = new JPanel(new VerticalLayout());

	private List<JXTable> tables = new ArrayList<JXTable>();
	private List<JPanel> titles = new ArrayList<JPanel>();

	/**
	 * Create the panel
	 */
	public CategoryDownloadPanel(EventList<DownloadItem> list) {
		evenHighlighter = HighlighterFactory.createAlternateStriping(oddColor, evenColor);
		oddHighlighter = HighlighterFactory.createAlternateStriping(evenColor, oddColor);
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
		
		list.addListEventListener(new ListEventListener<DownloadItem>() {
			@Override
			public void listChanged(ListEvent<DownloadItem> listChanges) {
			    //list events probably won't be on EDT
			    SwingUtils.invokeLater(new Runnable() {
					public void run() {
						for (int i = 0; i < titles.size() && i < tables.size(); i++) {
							boolean isVisible = tables.get(i).getRowCount() > 0;
							titles.get(i).setVisible(isVisible);
						}
						updateStriping();
					}

				});
			}
		});

		
		updateStriping();
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
		//TODO: kill cast and fix this
		FilterList<DownloadItem> filterList = 
			new FilterList<DownloadItem>(list, new DownloadStateMatcher(states));
		final JXTable table = new DownloadTable(filterList);
		tables.add(table);
		//TODO - list selection from glazed lists
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
		
		for (JXTable table : tables) {
						
			if (table.getRowCount() > 0 && table.isVisible()) {
				
				if (length % 2 == 0) {
					table.setHighlighters(evenHighlighter);
				} else {
					table.setHighlighters(oddHighlighter);
				}
				
				length += table.getRowCount();
			}
			
		}
	}

}
