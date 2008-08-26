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
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import org.limewire.ui.swing.ConfigurableTable;

public class BaseResultPanel extends JXPanel {
    
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
            SearchResultDownloader searchResultDownloader, Search search) {
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

        //setBorder(BorderFactory.createTitledBorder(
        //    BorderFactory.createLineBorder(Color.RED, 1), "BaseResultPanel"));
    }
    
    private void configureList(EventList<VisualSearchResult> eventList) {
        // We're using a JTable with one column instead of JList
        // because that will allow us to display buttons with rollover icons.
        resultsList = new ConfigurableTable<VisualSearchResult>(false);

        resultsList.setEventList(eventList);
        resultsList.setTableFormat(new ListViewTableFormat());

        SearchResultTableCellEditor editor = new SearchResultTableCellEditor();
        resultsList.setDefaultRenderer(VisualSearchResult.class, editor);
        resultsList.setDefaultEditor(VisualSearchResult.class, editor);

        resultsList.setRowHeight(50);
        resultsList.setColumnWidth(0, 700);
    }

    private void configureTable(EventList<VisualSearchResult> eventList,
        ResultsTableFormat<VisualSearchResult> tableFormat) {
        resultsTable = new ConfigurableTable<VisualSearchResult>(true);

        resultsTable.setEventList(eventList);
        resultsTable.setTableFormat(tableFormat);

        ActionColumnTableCellEditor editor = new ActionColumnTableCellEditor();
        resultsTable.setDefaultRenderer(VisualSearchResult.class, editor);
        resultsTable.setDefaultEditor(VisualSearchResult.class, editor);

        resultsTable.setColumnWidth(
            tableFormat.getActionButtonColumnIndex(), 100);
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
