package org.limewire.ui.swing.search.resultpanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Calendar;

import javax.swing.JLabel;
import javax.swing.Scrollable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.DisposalListener;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.RemoteHostWidget.RemoteWidgetType;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.AllTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.AudioTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ClassicDoubleClickHandler;
import org.limewire.ui.swing.search.resultpanel.classic.DocumentTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.FromTableCellRenderer;
import org.limewire.ui.swing.search.resultpanel.classic.ImageTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.OtherTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ProgramTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ResultEnterAction;
import org.limewire.ui.swing.search.resultpanel.classic.TorrentTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.VideoTableFormat;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRendererFactory;
import org.limewire.ui.swing.table.MultilineTooltipRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.EventListJXTableSorting;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Base class containing the search results tables for a single category.  
 * BaseResultPanel contains both the List view and Table view components.  The
 * current view is selected by calling the <code>setViewType()</code> method.
 * the display category is selected by calling the <code>showCategory()</code>
 * method.
 */
public class BaseResultPanel extends JXPanel {
    
    private static final int TABLE_ROW_HEIGHT = 23;

    private static final Log LOG = LogFactory.getLog(BaseResultPanel.class);

    /** Table component for the Table view. */
    private final ResultsTable<VisualSearchResult> resultsTable;
    /** The category the table is currently configured for. */
    private SearchCategory tableConfiguredFor;
    
    /** The currently filtered SearchCategory. */
    private SearchCategory currentCategory;
    
    /** Data model containing search results. */
    private final SearchResultsModel searchResultsModel;
    
    private final ResultsTableFormatFactory tableFormatFactory;
    private final RemoteHostWidgetFactory fromWidgetfactory;
    private final Provider<IconLabelRendererFactory> iconLabelRendererFactory;
    private final DownloadHandler downloadHandler;
    private final Provider<TimeRenderer> timeRenderer;
    private final Provider<FileSizeRenderer> fileSizeRenderer;
    private final Provider<CalendarRenderer> calendarRenderer;
    private final Provider<QualityRenderer> qualityRenderer;
    private final DefaultLimeTableCellRenderer defaultTableCellRenderer;
    
    private DefaultEventSelectionModel<VisualSearchResult> listSelectionModel;    
    private EventListJXTableSorting resultsTableSorting; 
    private DefaultEventSelectionModel<VisualSearchResult> selectionModel;
    private ColorHighlighter resultsColorHighlighter;
    private Scrollable visibleComponent;
    private final SearchResultMenuFactory menuFactory;

    /**
     * Constructs a BaseResultPanel with the specified components.
     */
    @Inject
    public BaseResultPanel(
            @Assisted SearchResultsModel searchResultsModel,
            ResultsTableFormatFactory tableFormatFactory,
            RemoteHostWidgetFactory fromWidgetFactory,
            SearchResultMenuFactory menuFactory,
            Provider<IconLabelRendererFactory> iconLabelRendererFactory,
            Provider<TimeRenderer> timeRenderer,
            Provider<FileSizeRenderer> fileSizeRenderer, 
            Provider<CalendarRenderer> calendarRenderer,
            LibraryMediator libraryMediator,
            Provider<QualityRenderer> qualityRenderer, 
            DefaultLimeTableCellRenderer defaultTableCellRenderer,
            DownloadMediator downloadMediator) {
        
        this.searchResultsModel = searchResultsModel;
        this.tableFormatFactory = tableFormatFactory;
        this.fromWidgetfactory = fromWidgetFactory;
        this.iconLabelRendererFactory = iconLabelRendererFactory;
        this.downloadHandler = new DownloadHandlerImpl(searchResultsModel, libraryMediator, downloadMediator);
        this.timeRenderer = timeRenderer;
        this.fileSizeRenderer = fileSizeRenderer;
        this.calendarRenderer = calendarRenderer;
        this.qualityRenderer = qualityRenderer;
        this.defaultTableCellRenderer = defaultTableCellRenderer;
        this.menuFactory = menuFactory;
        
        // Create tables.
        this.resultsTable = createTable();
        
        searchResultsModel.addDisposalListener(new ResultModelDisposalListener());
        
        setLayout(new BorderLayout());
    }
    
    /**
     * Creates a new Table view table.
     */
    private ResultsTable<VisualSearchResult> createTable() {
        ResultsTable<VisualSearchResult> table = new ResultsTable<VisualSearchResult>();
        
        // Set table fields that do not change with search category.
        table.setPopupHandler(new SearchPopupHandler(downloadHandler, table, menuFactory));
        table.setDoubleClickHandler(new ClassicDoubleClickHandler(table, downloadHandler));
        table.setRowHeight(TABLE_ROW_HEIGHT);
        
        return table;
    }
    
    /**
     * Configures the Table view to display results for the selected category.
     */
    private void configureTable() {
        LOG.debugf("Configuring table view for {0}, configured already for {1}", currentCategory, tableConfiguredFor);
        
        tableConfiguredFor = currentCategory;
        
        // Uninstall components with references to previous list.
        if (resultsTableSorting != null) {
            resultsTableSorting.uninstall();
        }
        if (resultsColorHighlighter != null) {
            resultsTable.removeHighlighter(resultsColorHighlighter);
        }

        // Get results list and table format for selected category.
        SearchCategory selectedCategory = searchResultsModel.getSelectedCategory();
        EventList<VisualSearchResult> eventList = searchResultsModel.getFilteredSearchResults();
        ResultsTableFormat<VisualSearchResult> tableFormat = tableFormatFactory.createTableFormat(selectedCategory);

        // Create sorted list and set table model.
        SortedList<VisualSearchResult> sortedList = GlazedListsFactory.sortedList(eventList, null);
        EventList<VisualSearchResult> downstreamList = sortedList;
        resultsTable.setEventListFormat(downstreamList, tableFormat, true);
        
        //link the jxtable column headers to the sorted list
        resultsTableSorting = EventListJXTableSorting.install(resultsTable, sortedList, tableFormat);   
        
        //create and install new EventSelectionModel and enter key action        
        if (selectionModel != null) {
            selectionModel.dispose();
        }
        selectionModel = new DefaultEventSelectionModel<VisualSearchResult>(downstreamList);
        resultsTable.setSelectionModel(selectionModel);
        resultsTable.setEnterKeyAction(new ResultEnterAction(selectionModel.getSelected(), downloadHandler));
            
        setupCellRenderers(tableFormat);
        
        // Apply column settings for table format.
        resultsTable.applySavedColumnSettings();

        TableColors tableColors = new TableColors();
        resultsColorHighlighter = new ColorHighlighter(new DownloadedHighlightPredicate(downstreamList), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        resultsTable.addHighlighter(resultsColorHighlighter);
    }

    /**
     * Initializes cell renderers in the Table view column model based on 
     * column types provided by the specified table format. 
     */
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        SearchCategory selectedCategory = searchResultsModel.getSelectedCategory();
        
        TableCellRenderer nameRenderer = iconLabelRendererFactory.get().createIconRenderer(selectedCategory == SearchCategory.ALL);
        
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Class clazz = tableFormat.getColumnClass(i);
            if (clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class) {
                setCellRenderer(i, defaultTableCellRenderer);
                setCellEditor(i, null);
            } else if (clazz == Calendar.class) {
                setCellRenderer(i, calendarRenderer.get());
                setCellEditor(i, null);
            } else if (i == tableFormat.getNameColumn()) {
                setCellRenderer(i, nameRenderer);
                setCellEditor(i, null);
            } else if (VisualSearchResult.class.isAssignableFrom(clazz)) {
                setCellRenderer(i, new FromTableCellRenderer(fromWidgetfactory.create(RemoteWidgetType.TABLE)));
                setCellEditor(i, new FromTableCellRenderer(fromWidgetfactory.create(RemoteWidgetType.TABLE)));
            }
        }
        
        // Set specific column renderers for selected category.
        switch (selectedCategory) {
        case ALL:
            setCellRenderer(AllTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            break;
        case AUDIO:
            setHeaderRenderer(AudioTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
            setCellRenderer(AudioTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            setCellRenderer(AudioTableFormat.LENGTH_INDEX, timeRenderer.get());
            setCellRenderer(AudioTableFormat.QUALITY_INDEX, qualityRenderer.get());
            break;
        case VIDEO:
            setHeaderRenderer(VideoTableFormat.LENGTH_INDEX, new TableCellHeaderRenderer(JLabel.TRAILING));
            setCellRenderer(VideoTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            setCellRenderer(VideoTableFormat.LENGTH_INDEX, timeRenderer.get());
            setCellRenderer(VideoTableFormat.QUALITY_INDEX, qualityRenderer.get());
            break;
        case DOCUMENT:
            setCellRenderer(DocumentTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            break;
        case IMAGE:
            setCellRenderer(ImageTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            break;
        case PROGRAM:
            setCellRenderer(ProgramTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            break;
        case OTHER:
            setCellRenderer(OtherTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            break;
        case TORRENT:
            MultilineTooltipRenderer renderer = new MultilineTooltipRenderer();
            setCellRenderer(TorrentTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            setCellRenderer(TorrentTableFormat.FILES_INDEX, renderer);
            setCellRenderer(TorrentTableFormat.TRACKERS_INDEX, renderer);
        default:
            break;
        }
    }

    /**
     * Assigns the specified cell renderer to the specified column in the 
     * Table view column model.   
     */
    protected void setCellRenderer(int column, TableCellRenderer cellRenderer) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellRenderer(cellRenderer);
    }
    
    /**
     * Assigns the specified cell editor to the specified column in the 
     * Table view column model.   
     */
    protected void setCellEditor(int column, TableCellEditor editor) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellEditor(editor);
    }

    /**
     * Assigns the specified header renderer to the specified column in the 
     * Table view column model.   
     */
    protected void setHeaderRenderer(int column, TableCellRenderer headerRenderer) {
        TableColumnModel tcm = resultsTable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setHeaderRenderer(headerRenderer);
    }

    /**
     * Displays search results for the specified search category.
     */
    public void showCategory(SearchCategory searchCategory) {
        if(currentCategory != searchCategory) {
            currentCategory = searchCategory;
            
            // Select category to update sorted list.
            searchResultsModel.setSelectedCategory(searchCategory);
            
            if(tableConfiguredFor != null || visibleComponent == resultsTable) {
                configureTable();
            }
        } else {
            LOG.debugf("Resetting current category {0}!", currentCategory);
        }
    }

    // FIXME: get rid of this
    public void setViewType() {
        if(visibleComponent != null) {
            remove((Component)visibleComponent);
        }
        if(currentCategory != null && tableConfiguredFor != currentCategory) {
            configureTable();
        }
        this.visibleComponent = resultsTable;
        add((Component)visibleComponent);
    }

    /**
     * Returns the header component for the scroll pane.  The method returns
     * null if no header is displayed.
     */
    public Component getScrollPaneHeader() {
        return visibleComponent == resultsTable ?
            resultsTable.getTableHeader() : null;
    }

    /**
     * Returns the results view component currently being displayed. 
     */
    public Scrollable getScrollable() {
        return visibleComponent;
    }
    
    /**Disposes of the selection model when the result model is disposed*/
    private class ResultModelDisposalListener implements DisposalListener {
        @Override
        public void objectDisposed(Disposable source) {
            if (listSelectionModel != null){
                listSelectionModel.dispose();
            }
            if(selectionModel != null){
                selectionModel.dispose();
            }
        }        
    }
    
    /**
	 * Paints the foreground of a table row. 
	 */
    private static class DownloadedHighlightPredicate implements HighlightPredicate {
        private EventList<VisualSearchResult> eventList;
        public DownloadedHighlightPredicate (EventList<VisualSearchResult> sortedList) {
            this.eventList = sortedList;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            VisualSearchResult result = eventList.get(adapter.row);
            return result.isSpam();
        }       
    }
}
