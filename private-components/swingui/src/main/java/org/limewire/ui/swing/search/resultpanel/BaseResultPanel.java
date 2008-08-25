package org.limewire.ui.swing.search.resultpanel;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.Scrollable;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.ModeListener;
import org.limewire.ui.swing.search.ModeListener.Mode;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;
import org.limewire.ui.swing.ConfigurableTable;

public class BaseResultPanel extends JXPanel {
    
    private final CardLayout layout = new CardLayout();
    private final EventList<VisualSearchResult> baseEventList;
    private ConfigurableTable resultsTable;
    private JList resultsList;
    private final Search search;
    private final SearchResultDownloader searchResultDownloader;
    
    private Scrollable visibileComponent;
    
    BaseResultPanel(String title,
            EventList<VisualSearchResult> eventList,
            AdvancedTableFormat<VisualSearchResult> tableFormat,
            SearchResultDownloader searchResultDownloader, Search search) {
        this.baseEventList = eventList;
        this.searchResultDownloader = searchResultDownloader;
        this.search = search;
        
        setLayout(layout);
                
        EventListModel<VisualSearchResult> eventListModel =
            new EventListModel<VisualSearchResult>(eventList);

        configureList(eventListModel, eventList);
        configureTable(eventList, tableFormat);
        
        // TODO: RMV I think these JScrollPanes are fighting with the ones
        // TODO: RMV that Sam added outside that include Sponsored Results.
        add(new JScrollPane(resultsList), ModeListener.Mode.LIST.name());
        add(new JScrollPane(resultsTable), ModeListener.Mode.TABLE.name());
        setMode(ModeListener.Mode.LIST);
    }
    
    private void configureList(
        EventListModel<VisualSearchResult> eventListModel,
        EventList<VisualSearchResult> eventList) {

        resultsList = new JXList(eventListModel);
        resultsList.setCellRenderer(new SearchResultListCellRenderer());
        resultsList.setFixedCellHeight(50);
        resultsList.setSelectionModel(
            new EventSelectionModel<VisualSearchResult>(eventList));
        resultsList.setSelectionMode(
            ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        resultsList.addMouseListener(new ResultDownloader());
    }

    private void configureTable(EventList<VisualSearchResult> eventList,
        AdvancedTableFormat<VisualSearchResult> tableFormat) {
        resultsTable = new ConfigurableTable<VisualSearchResult>(){
            // hack - gets rid of java.lang.IllegalStateException: A reverse mapping
            // function must be specified to support this List operation
            @Override
            public void setValueAt(Object aValue, int row, int column) {
            }
        };

        resultsTable.setEventList(eventList);
        resultsTable.setTableFormat(tableFormat);

        ActionColumnTableCellEditor editor = new ActionColumnTableCellEditor();
        ActionColumnTableCellEditor renderer = new ActionColumnTableCellEditor();
        resultsTable.setDefaultRenderer(VisualSearchResult.class, renderer);
        resultsTable.setDefaultEditor(VisualSearchResult.class, editor);

        // Find the column that will disply the action buttons.
        /*
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            Class type = tableFormat.getColumnClass(i);
            if (type == VisualSearchResult.class) {
                System.out.println("VisualSearchResult column is " + i);
                TableColumn column =
                    resultsTable.getColumnModel().getColumn(i);
                column.setCellEditor(editor);
            }
        }
        */
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

    public Component getScrollPaneHeader() {
        if(visibileComponent == resultsTable) {
            return resultsTable.getTableHeader();
        } else {
            return null;
        }
    }
}
