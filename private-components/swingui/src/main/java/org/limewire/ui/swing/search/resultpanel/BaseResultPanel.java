package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Scrollable;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.ModeListener;
import org.limewire.ui.swing.search.ModeListener.Mode;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;
import java.awt.Window;
import java.util.Calendar;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import org.limewire.ui.swing.ConfigurableTable;

public class BaseResultPanel extends JXPanel {
    
    private ActionColumnTableCellEditor actionEditor =
        new ActionColumnTableCellEditor();
    private ActionColumnTableCellEditor actionRenderer =
        new ActionColumnTableCellEditor();
    private final CardLayout layout = new CardLayout();
    private final EventList<VisualSearchResult> baseEventList;
    private ConfigurableTable<VisualSearchResult> resultsList;
    private ConfigurableTable<VisualSearchResult> resultsTable;
    private final Search search;
    private final SearchResultDownloader searchResultDownloader;
    
    private Scrollable visibileComponent;
    
    BaseResultPanel(String title,
            EventList<VisualSearchResult> eventList,
            ResultsTableFormat<VisualSearchResult> tableFormat,
            SearchResultDownloader searchResultDownloader,
            Search search) {
        this.baseEventList = eventList;
        this.searchResultDownloader = searchResultDownloader;
        this.search = search;
        
        setLayout(layout);
                
        configureList(eventList);
        configureTable(eventList, tableFormat);
        
        // TODO: RMV I think these JScrollPanes are fighting with the ones
        // TODO: RMV that Sam added outside that include Sponsored Results.
        add(new JScrollPane(resultsList), ModeListener.Mode.LIST.name());
        add(new JScrollPane(resultsTable), ModeListener.Mode.TABLE.name());
        setMode(ModeListener.Mode.LIST);
    }
    
    private void configureList(final EventList<VisualSearchResult> eventList) {
        // We're using a JTable with one column instead of JList
        // because that will allow us to display buttons with rollover icons.
        resultsList = new ConfigurableTable<VisualSearchResult>(false) {
            // TODO: RMV Want the code below to fix loss of row selection
            // TODO: RMV after sorting or filtering?
            /*
            private VisualSearchResult selectedVSR;

            public void setSelected(VisualSearchResult selectedVSR) {
                this.selectedVSR = selectedVSR;
            }

            public void restoreSelection() {
                if (selectedVSR == null) return;
                
                TableModel tableModel = getModel();
                int rowCount = tableModel.getRowCount();
                for (int row = 0; row < rowCount; row++) {
                    Object obj = tableModel.getValueAt(row, 0);
                    if (obj == selectedVSR) {
                        System.out.println(
                            "BaseResultPanel: restored selection of row " + row);
                        setRowSelectionInterval(row, row);
                        break;
                    }
                }
            }
            */
        };

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
            new ListViewTableCellEditor(actionRenderer);
        resultsList.setDefaultRenderer(VisualSearchResult.class, renderer);

        ListViewTableCellEditor editor =
            new ListViewTableCellEditor(actionEditor);
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        int columnIndex = 0;
        resultsList.setColumnWidth(columnIndex,
            tableFormat.getInitialColumnWidth(columnIndex));

        resultsList.setRowHeight(ListViewTableCellEditor.HEIGHT);

        //EventSelectionModel<VisualSearchResult> selectionModel =
        //    new EventSelectionModel<VisualSearchResult>(eventList);
        // The next line breaks everything!
        //resultsList.setSelectionModel(selectionModel);
        
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
                    Window window = (Window) component.getTopLevelAncestor();
                    SearchResultMenu menu = new SearchResultMenu(window, vsr);
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

        resultsTable.setDefaultRenderer(
            Calendar.class, new CalendarTableCellRenderer());
        resultsTable.setDefaultRenderer(
            Component.class, new ComponentTableCellRenderer());

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
        int columnCount = resultsTable.getColumnCount();
        // We have to loop backwards because making a column invisible
        // changes the index of the columns after it.
        for (int i = columnCount - 1; i > lastVisibleColumnIndex; i--) {
            resultsTable.setColumnVisible(i, false);
        }

        resultsTable.setRowHeight(26);
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
                TableModel tm = resultsList.getModel();
                VisualSearchResult item =
                    (VisualSearchResult) tm.getValueAt(row, 0);
                
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

    public Component getScrollPaneHeader() {
        if(visibileComponent == resultsTable) {
            return resultsTable.getTableHeader();
        } else {
            return null;
        }
    }
}
