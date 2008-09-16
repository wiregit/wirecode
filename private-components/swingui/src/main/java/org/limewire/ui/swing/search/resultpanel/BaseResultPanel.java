package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.ConfigurableTable;
import org.limewire.ui.swing.StringTableCellRenderer;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.search.ModeListener;
import org.limewire.ui.swing.search.ModeListener.Mode;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public abstract class BaseResultPanel extends JXPanel {
    public static final int TABLE_ROW_HEIGHT = 26;
    
    private ActionColumnTableCellEditor actionEditor =
        new ActionColumnTableCellEditor();
    private ActionColumnTableCellEditor actionRenderer =
        new ActionColumnTableCellEditor();
    private final CardLayout layout = new CardLayout();
    private final EventList<VisualSearchResult> baseEventList;
    private ConfigurableTable<VisualSearchResult> resultsList;
    private ConfigurableTable<VisualSearchResult> resultsTable;
    private NavigableTree navTree;
    private final Search search;
    private final SearchResultDownloader searchResultDownloader;
    
    private Scrollable visibileComponent;
    
    BaseResultPanel(String title,
            EventList<VisualSearchResult> eventList,
            ResultsTableFormat<VisualSearchResult> tableFormat,
            SearchResultDownloader searchResultDownloader,
            Search search,
            NavigableTree navTree) {
        this.baseEventList = eventList;
        this.searchResultDownloader = searchResultDownloader;
        this.search = search;
        this.navTree = navTree;
        
        setLayout(layout);
                
        configureList(eventList);
        configureTable(eventList, tableFormat);
 
        add(resultsList, ModeListener.Mode.LIST.name());
        add(resultsTable, ModeListener.Mode.TABLE.name());
        setMode(ModeListener.Mode.LIST);
    }
    
    private void configureList(final EventList<VisualSearchResult> eventList) {
        // We're using a MouseableTable with one column instead of JList
        // because that will allow us to display buttons with rollover icons.
        resultsList = new ConfigurableTable<VisualSearchResult>(false);

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

        // TODO: RMV Need to use Guice to get an instance.
        ListViewTableCellEditor renderer =
            new ListViewTableCellEditor(actionRenderer, navTree);
        resultsList.setDefaultRenderer(VisualSearchResult.class, renderer);

        // TODO: RMV Need to use Guice to get an instance.
        ListViewTableCellEditor editor =
            new ListViewTableCellEditor(actionEditor, navTree);
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        int columnIndex = 0;
        resultsList.setColumnWidth(columnIndex,
            tableFormat.getInitialColumnWidth(columnIndex));

        resultsList.setRowHeight(ListViewTableCellEditor.HEIGHT);

        //EventSelectionModel<VisualSearchResult> selectionModel =
        //    new EventSelectionModel<VisualSearchResult>(eventList);
        // TODO: RMV The next line breaks everything!
        //resultsList.setSelectionModel(selectionModel);
        
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
                        BaseResultPanel.this, navTree, vsr, row);
                    menu.show(component, e.getX(), e.getY());
                }
            }
        });
    }

    private void configureTable(EventList<VisualSearchResult> eventList,
        final ResultsTableFormat<VisualSearchResult> tableFormat) {
        resultsTable = new ConfigurableTable<VisualSearchResult>(true);

        resultsTable.setEventList(eventList);
        resultsTable.setTableFormat(tableFormat);

        CalendarTableCellRenderer calendarRenderer =
            new CalendarTableCellRenderer();
        ComponentTableCellRenderer componentRenderer =
            new ComponentTableCellRenderer();
        StringTableCellRenderer stringRenderer =
            new StringTableCellRenderer();

        // TODO: RMV Don't know why this approach of registering renderers
        // TODO: RMV by class didn't work.
        /*
        resultsTable.setDefaultRenderer(Integer.class, stcr);
        resultsTable.setDefaultRenderer(Long.class, stcr);
        resultsTable.setDefaultRenderer(String.class, stcr);
        resultsTable.setDefaultRenderer(
            Calendar.class, new CalendarTableCellRenderer());
        resultsTable.setDefaultRenderer(
            Component.class, new ComponentTableCellRenderer());
        */

        // TODO: RMV But this way works.
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
            }
        }

        resultsTable.setDefaultRenderer(
            VisualSearchResult.class, actionRenderer);
        resultsTable.setDefaultEditor(
            VisualSearchResult.class, actionEditor);

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
            DownloadItem di = searchResultDownloader.addDownload(
                search, vsr.getCoreSearchResults());
            di.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("state".equals(evt.getPropertyName())) {
                        DownloadState state = (DownloadState) evt.getNewValue();
                        switch (state) {
                            case CANCELLED:
                            case ERROR:
                                vsr.setDownloadState(
                                    BasicDownloadState.NOT_STARTED);
                                break;
                            case DONE:
                                vsr.setDownloadState(
                                    BasicDownloadState.DOWNLOADED);
                                break;
                        }

                        // Trigger a re-render of the corresponding list row.
                        AbstractTableModel tm =
                            (AbstractTableModel) resultsList.getModel();
                        
                        // TODO: Why doesn't this cause the row to be repainted?
                        //System.out.println(
                        //    "BaseResultPanel: firing row " + row + " update");
                        tm.fireTableRowsUpdated(row, row);
                    }
                }
            });
             
            vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
        } catch (SaveLocationException sle) {
            // TODO: Do something!
            sle.printStackTrace();
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
}