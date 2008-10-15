package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.DownloadItemPropertyListener;
import org.limewire.ui.swing.search.FromActions;
import org.limewire.ui.swing.search.ModeListener;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.ModeListener.Mode;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.StringTableCellRenderer;

import ca.odell.glazedlists.EventList;

public abstract class BaseResultPanel extends JXPanel implements DownloadHandler {
    private static final int TABLE_ROW_HEIGHT = 26;
    
    private final CardLayout layout = new CardLayout();
    private final EventList<VisualSearchResult> baseEventList;
    private ConfigurableTable<VisualSearchResult> resultsList;
    private ConfigurableTable<VisualSearchResult> resultsTable;
    private final Search search;
    private final org.limewire.core.api.download.ResultDownloader resultDownloader;
    
    private Scrollable visibileComponent;
    
    BaseResultPanel(EventList<VisualSearchResult> eventList,
            ResultsTableFormat<VisualSearchResult> tableFormat,
            org.limewire.core.api.download.ResultDownloader resultDownloader,
            Search search,
            SearchInfo searchInfo, 
            RowSelectionPreserver preserver,
            Navigator navigator, FromActions fromActions) {
        this.baseEventList = eventList;
        this.resultDownloader = resultDownloader;
        this.search = search;
        
        setLayout(layout);
                
        configureList(eventList, preserver, navigator, searchInfo, fromActions);
        configureTable(eventList, tableFormat, navigator);
 
        add(resultsList, ModeListener.Mode.LIST.name());
        add(resultsTable, ModeListener.Mode.TABLE.name());
        setMode(ModeListener.Mode.LIST);
    }
    
    private void configureList(final EventList<VisualSearchResult> eventList, RowSelectionPreserver preserver, final Navigator navigator, 
            SearchInfo searchInfo, FromActions fromActions) {
        resultsList = new ConfigurableTable<VisualSearchResult>(false);
        resultsList.setShowGrid(false, false);
        preserver.addRowPreservationListener(resultsList);

        resultsList.setEventList(eventList);
        ListViewTableFormat tableFormat = new ListViewTableFormat();
        resultsList.setTableFormat(tableFormat);

        // Note that the same ListViewTableCellEditor instance
        // cannot be used for both the editor and the renderer
        // because the renderer receives paint requests for some cells
        // while another cell is being edited
        // and they can't share state (the list of sources).
        // The two ListViewTableCellEditor instances
        // can share the same ActionColumnTableCellEditor though.

        ListViewTableCellEditor renderer =
            new ListViewTableCellEditor(new ActionColumnTableCellEditor(this), searchInfo.getQuery(), 
                    fromActions, navigator, resultsList);
        
        TableColumnModel tcm = resultsList.getColumnModel();
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setCellRenderer(renderer);
        }

        ListViewTableCellEditor editor =
            new ListViewTableCellEditor(new ActionColumnTableCellEditor(this), searchInfo.getQuery(), 
                    fromActions, navigator, resultsList);
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        for(int columnIndex = 0; columnIndex < tableFormat.getLastVisibleColumnIndex() + 1; columnIndex++) {        
            int initialColumnWidth = tableFormat.getInitialColumnWidth(columnIndex);
            resultsList.setColumnWidth(columnIndex, initialColumnWidth);
        }
        
        resultsList.getColumnModel().getColumn(2).setMaxWidth(ListViewTableFormat.ACTIONS_WIDTH);

        resultsList.setRowHeight(ListViewTableCellEditor.HEIGHT);
        
        resultsList.addMouseListener(new ResultDownloader());

        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // If a right-click has occurred ...
                if (e.getButton() == 3) {
                    // Get the VisualSearchResult that was selected.
                    int row = resultsList.rowAtPoint(e.getPoint());
                    VisualSearchResult vsr = eventList.get(row);

                    // Display a SearchResultMenu for the VisualSearchResult.
                    JComponent component = (JComponent) e.getSource();
                    SearchResultMenu menu = new SearchResultMenu(
                        BaseResultPanel.this, navigator, vsr, row);
                    menu.show(component, e.getX(), e.getY());
                }
            }
        });
    }

    private void configureTable(EventList<VisualSearchResult> eventList,
        final ResultsTableFormat<VisualSearchResult> tableFormat, Navigator navigator) {
        resultsTable = new ConfigurableTable<VisualSearchResult>(true);

        resultsTable.setEventList(eventList);
        resultsTable.setTableFormat(tableFormat);

        CalendarTableCellRenderer calendarRenderer =
            new CalendarTableCellRenderer();
        ComponentTableCellRenderer componentRenderer =
            new ComponentTableCellRenderer();
        StringTableCellRenderer stringRenderer =
            new StringTableCellRenderer();

        TableColumnModel tcm = resultsTable.getColumnModel();
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            Class clazz = tableFormat.getColumnClass(i);
            if (clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class) {
                tc.setCellRenderer(stringRenderer);
            } else if (clazz == Calendar.class) {
                tc.setCellRenderer(calendarRenderer);
            } else if (clazz == Component.class) {
                tc.setCellRenderer(componentRenderer);
            } else if (VisualSearchResult.class.isAssignableFrom(clazz)) {
                tc.setCellRenderer(new ActionColumnTableCellEditor(this));
            }
        }

        resultsTable.setDefaultEditor(
            VisualSearchResult.class, new ActionColumnTableCellEditor(this));

        // Don't allow sorting on the "Actions" column
        int columnIndex = tableFormat.getActionColumnIndex();
        resultsTable.getColumnExt(columnIndex).setSortable(false);

        // Set default width of all visible columns.
        int lastVisibleColumnIndex = tableFormat.getLastVisibleColumnIndex();
        for (int i = 0; i <= lastVisibleColumnIndex; i++) {
            resultsTable.setColumnWidth(
                i, tableFormat.getInitialColumnWidth(i));
        }

        // Make some columns invisible by default.
        // We have to loop backwards because making a column invisible
        // changes the index of the columns after it.
        for (int i = columnCount - 1; i > lastVisibleColumnIndex; i--) {
            resultsTable.setColumnVisible(i, false);
        }

        resultsTable.setRowHeight(TABLE_ROW_HEIGHT);
    }

    public void download(final VisualSearchResult vsr, final int row) {
        try {
            // TODO: Need to go through some of the rigor that
            // com.limegroup.gnutella.gui.download.DownloaderUtils.createDownloader
            // went through.. checking for conflicts, etc.
            DownloadItem di = resultDownloader.addDownload(
                search, vsr.getCoreSearchResults());
            di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
             
            vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
        } catch (SaveLocationException sle) {
            //TODO
            throw new RuntimeException("FIX ME", sle);
        }
    }
    
    public EventList<VisualSearchResult> getResultsEventList() {
        return baseEventList;
    }

    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setMode(Mode mode) {
        layout.show(this, mode.name());
        switch(mode) {
        case LIST: this.visibileComponent = resultsList; break;
        case TABLE: this.visibileComponent = resultsTable; break;
        default: throw new IllegalStateException("unsupported mode: " + mode);
        }
    }

    private class ResultDownloader extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = resultsList.rowAtPoint(e.getPoint());
                if (row == -1) return;
                TableModel tm = resultsList.getModel();
                VisualSearchResult vsr =
                    (VisualSearchResult) tm.getValueAt(row, 0);
                download(vsr, row);
            }
        }
    }

    public Component getScrollPaneHeader() {
        return visibileComponent == resultsTable ?
            resultsTable.getTableHeader() : null;
    }

    public Scrollable getScrollable() {
        return visibileComponent;
    }
}