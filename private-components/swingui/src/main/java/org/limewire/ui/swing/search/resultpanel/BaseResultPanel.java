package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.DownloadItemPropertyListener;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.SearchViewType;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.StringTableCellRenderer;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.EventTableModel;

public abstract class BaseResultPanel extends JXPanel implements DownloadHandler {
    
    private final ListViewTableEditorRendererFactory listViewTableEditorRendererFactory;
    private final Log LOG = LogFactory.getLog(BaseResultPanel.class);
    
    private static final int TABLE_ROW_HEIGHT = 26;
    private static final int ROW_HEIGHT = 56;
    
    private final CardLayout layout = new CardLayout();
    private final EventList<VisualSearchResult> baseEventList;
    private ListViewTable resultsList;
    private ConfigurableTable<VisualSearchResult> resultsTable;
    private final Search search;
    private final ResultDownloader resultDownloader;
    //cache for RowDisplayResult which could be expensive to generate with large search result sets
    private final Map<VisualSearchResult, RowDisplayResult> vsrToRowDisplayResultMap = 
        new HashMap<VisualSearchResult, RowDisplayResult>();
    
    private Scrollable visibileComponent;
    
    BaseResultPanel(ListViewTableEditorRendererFactory listViewTableEditorRendererFactory,
            EventList<VisualSearchResult> eventList,
            ResultsTableFormat<VisualSearchResult> tableFormat,
            ResultDownloader resultDownloader,
            Search search,
            SearchInfo searchInfo, 
            RowSelectionPreserver preserver,
            Navigator navigator, RemoteHostActions remoteHostActions, PropertiesFactory<VisualSearchResult> properties, 
            ListViewRowHeightRule rowHeightRule) {
        
        this.listViewTableEditorRendererFactory = listViewTableEditorRendererFactory;
        
        this.baseEventList = eventList;
        this.resultDownloader = resultDownloader;
        this.search = search;
        
        setLayout(layout);
                
        configureList(eventList, preserver, navigator, searchInfo, remoteHostActions, properties, rowHeightRule);
        configureTable(eventList, tableFormat, navigator);
 
        add(resultsList, SearchViewType.LIST.name());
        add(resultsTable, SearchViewType.TABLE.name());
        setViewType(SearchViewType.LIST);
    }
    
    private void configureList(final EventList<VisualSearchResult> eventList, RowSelectionPreserver preserver, final Navigator navigator, 
            final SearchInfo searchInfo, final RemoteHostActions remoteHostActions, final PropertiesFactory<VisualSearchResult> properties, 
            final ListViewRowHeightRule rowHeightRule) {
        resultsList = new ListViewTable();
        resultsList.setShowGrid(true, false);
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

        ListViewTableEditorRenderer renderer = listViewTableEditorRendererFactory.create(
           new ActionColumnTableCellEditor(this), searchInfo.getQuery(), 
                    remoteHostActions, navigator, resultsList.getTableColors().selectionColor, this);
        
        TableColumnModel tcm = resultsList.getColumnModel();
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setCellRenderer(renderer);
        }

        ListViewTableEditorRenderer editor = listViewTableEditorRendererFactory.create(
                new ActionColumnTableCellEditor(this), searchInfo.getQuery(), 
                    remoteHostActions, navigator, resultsList.getTableColors().selectionColor, this);
        
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        for(int columnIndex = 0; columnIndex < tableFormat.getLastVisibleColumnIndex() + 1; columnIndex++) {        
            int initialColumnWidth = tableFormat.getInitialColumnWidth(columnIndex);
            resultsList.setColumnWidth(columnIndex, initialColumnWidth);
        }
        
//        resultsList.getColumnModel().getColumn(2).setMaxWidth(ListViewTableFormat.ACTIONS_WIDTH);
        
        resultsList.setRowHeightEnabled(true);
        //add listener to table model to set row heights based on contents of the search results
        eventList.addListEventListener(new ListEventListener<VisualSearchResult>() {
            @Override
            public void listChanged(ListEvent<VisualSearchResult> listChanges) {
                
                final EventTableModel model = (EventTableModel) resultsList.getModel();
                if (model.getRowCount() == 0) {
                    return;
                }
                
                //Push row resizing to the end of the event dispatch queue
                Runnable runner = new Runnable() {
                    @Override
                    public void run() {
                        resultsList.setIgnoreRepaints(true);
                        for(int row = 0; row < model.getRowCount(); row++) {
                            VisualSearchResult vsr = (VisualSearchResult) model.getElementAt(row);
                            RowDisplayResult result = vsrToRowDisplayResultMap.get(vsr);
                            if (result == null || result.isStale(vsr)) {
                                result = rowHeightRule.getDisplayResult(vsr, searchInfo.getQuery());
                                vsrToRowDisplayResultMap.put(vsr, result);
                            } 
                            int newRowHeight = result.getConfig().getRowHeight();
                            if (resultsList.getRowHeight(row) != newRowHeight) {
                                LOG.debugf("Row: {0} vsr: {1} config: {2}", row, vsr.getHeading(), 
                                        result.getConfig());
                                resultsList.setRowHeight(row, newRowHeight);
                            }
                        }
                        resultsList.setIgnoreRepaints(false);
                        resultsList.updateViewSizeSequence();
                        resultsList.resizeAndRepaint();
                    }
                };
                
                SwingUtilities.invokeLater(runner);
            }
        });
        resultsList.setRowHeight(ROW_HEIGHT);
        
        resultsList.addMouseListener(new ResultDownloaderAdaptor());

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
                    SearchResultMenu searchResultMenu = new SearchResultMenu(BaseResultPanel.this, vsr, row, remoteHostActions, properties);
                    searchResultMenu.show(component, e.getX(), e.getY());
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
        BackgroundExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
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
        });
    }
    
    public EventList<VisualSearchResult> getResultsEventList() {
        return baseEventList;
    }

    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setViewType(SearchViewType mode) {
        layout.show(this, mode.name());
        switch(mode) {
        case LIST: this.visibileComponent = resultsList; break;
        case TABLE: this.visibileComponent = resultsTable; break;
        default: throw new IllegalStateException("unsupported mode: " + mode);
        }
    }

    private class ResultDownloaderAdaptor extends MouseAdapter {
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
    
    private static class ListViewTable extends ConfigurableTable<VisualSearchResult> {
        private boolean ignoreRepaints;
        
        public ListViewTable() {
            super(false);
            
            setGridColor(Color.decode("#EBEBEB"));
        }

        @Override
        protected TableColors newTableColors() {
            TableColors colors = super.newTableColors();
            
            colors.evenColor = Color.WHITE;
            colors.oddColor = Color.WHITE;
            colors.getEvenHighLighter().setBackground(colors.evenColor);
            colors.getOddHighLighter().setBackground(colors.oddColor);
            return colors;
        }
        
        private void setIgnoreRepaints(boolean ignore) {
            this.ignoreRepaints = ignore;
        }
        
        @Override
        protected void updateViewSizeSequence() {
            if (ignoreRepaints) {
                return;
            }
            super.updateViewSizeSequence();
        }

        @Override
        protected void resizeAndRepaint() {
            if (ignoreRepaints) {
                return;
            }
            super.resizeAndRepaint();
        }
    }
}