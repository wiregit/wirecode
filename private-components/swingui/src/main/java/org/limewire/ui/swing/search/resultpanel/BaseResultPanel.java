package org.limewire.ui.swing.search.resultpanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.Scrollable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.ActionColumnTableCellEditor;
import org.limewire.ui.swing.search.ModeListener;
import org.limewire.ui.swing.search.ModeListener.Mode;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;

public class BaseResultPanel extends JXPanel implements Scrollable {
    
    private static final int ACTION_COLUMN = 4;
    
    private final EventList<VisualSearchResult> baseEventList;
    private final JList resultsList;
    private final JXTable resultsTable;
    private final JScrollPane scrollPane = new JScrollPane();
    private final Search search;
    private final SearchResultDownloader searchResultDownloader;
    private final TableComparatorChooser tcc;
    
    BaseResultPanel(String title,
            EventList<VisualSearchResult> eventList,
            SearchResultDownloader searchResultDownloader, Search search) {
        this.baseEventList = eventList;
        this.searchResultDownloader = searchResultDownloader;
        this.search = search;
        
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        
        // TODO: RMV The latest screen mockups do not include this title!
        /*
        JLabel titleLabel = new JLabel(title);
        FontUtils.changeSize(titleLabel, 5);
        FontUtils.changeStyle(titleLabel, Font.BOLD);
        add(titleLabel, BorderLayout.NORTH);
        */
                
        EventListModel<VisualSearchResult> eventListModel =
            new EventListModel<VisualSearchResult>(eventList);
        
        resultsList = new JList(eventListModel);
        // TODO: RMV Write this renderer!
        //resultsList.setCellRenderer(new SearchResultListCellRenderer());
        resultsList.setSelectionModel(new EventSelectionModel<VisualSearchResult>(eventList));
        resultsList.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        resultsList.addMouseListener(new ResultDownloader());
        
        SortedList<VisualSearchResult> sortedResults =
            new SortedList<VisualSearchResult>(
                eventList, new ResultComparator());
        EventTableModel<VisualSearchResult> tableModel =
            new EventTableModel<VisualSearchResult>(
                sortedResults, new ResultTableFormat());
        resultsTable = new JXTable(tableModel);
        
        // This enables rollover icons on the buttons to work.
        // Note that this approach ... calling editCellAt
        // based on mouse movements ... could be an issue
        // if other cells in the table are truely editable.
        resultsTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = resultsTable.columnAtPoint(e.getPoint());
                if (col != ACTION_COLUMN) return;
                int row = resultsTable.rowAtPoint(e.getPoint());
                resultsTable.editCellAt(row, col);
            }
        });
        
        Highlighter highlighter = HighlighterFactory.createAlternateStriping();
        resultsTable.setHighlighters(new Highlighter[] { highlighter });
        
        boolean multiColumnSort = false;
        tcc = new TableComparatorChooser<VisualSearchResult>(
            resultsTable, sortedResults, multiColumnSort);
        // Does tcc need to be used anywhere?
        
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(ACTION_COLUMN);
        ActionColumnTableCellEditor actce = new ActionColumnTableCellEditor();
        tc.setCellRenderer(actce);
        tc.setCellEditor(actce);
        
        setMode(ModeListener.Mode.LIST);
    }
    
    public EventList<VisualSearchResult> getResultsEventList() {
        return baseEventList;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return resultsList.getPreferredScrollableViewportSize();
    }

    @Override
    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction) {
        return resultsList.getScrollableBlockIncrement(
            visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return resultsList.getScrollableTracksViewportHeight();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect,
            int orientation, int direction) {
        return resultsList.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }
    
    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setMode(Mode mode) {
        Component component =
            mode == Mode.LIST ? resultsList :
            mode == Mode.TABLE ? resultsTable :
            null;
        if(component != scrollPane.getViewport().getView()) {
            scrollPane.setViewportView(component);
        }
    }
    
    private class ResultDownloader extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int index = resultsList.locationToIndex(e.getPoint());
                ListModel dlm = resultsList.getModel();
                VisualSearchResult item =
                    (VisualSearchResult) dlm.getElementAt(index);
                resultsList.ensureIndexIsVisible(index);
                
                try {
                    // TODO: Need to go through some of the rigor that 
                    // com.limegroup.gnutella.gui.download.DownloaderUtils.createDownloader
                    // went through.. checking for conflicts, etc.
                    searchResultDownloader.addDownload(
                        search, item.getCoreSearchResults());
                } catch (SaveLocationException sle) {
                    // TODO: Do something!
                    sle.printStackTrace();
                }
            }
        }
    }
}
