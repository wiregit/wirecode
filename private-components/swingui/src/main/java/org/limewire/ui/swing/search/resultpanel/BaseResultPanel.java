package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.Scrollable;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SearchResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.SponsoredResultsPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

class BaseResultPanel extends JXPanel implements Scrollable {
    
    public enum Mode { LIST, TABLE };
    
    private final JList resultsList;
    private final JTable resultsTable;
    private final Search search;
    private final SearchResultDownloader searchResultDownloader;
    
    BaseResultPanel(String title,
            EventListModel<VisualSearchResult> listModel,
            EventSelectionModel<VisualSearchResult> selectionModel,
            SearchResultDownloader searchResultDownloader, Search search) {
        
        this.searchResultDownloader = searchResultDownloader;
        this.search = search;
        
        setLayout(new BorderLayout());
        
        // TODO: RMV The latest screen mockups do not include this title!
        /*
        JLabel titleLabel = new JLabel(title);
        FontUtils.changeSize(titleLabel, 5);
        FontUtils.changeStyle(titleLabel, Font.BOLD);
        add(titleLabel, BorderLayout.NORTH);
        */
                
        resultsList = new JList(listModel);
        resultsList.setSelectionModel(selectionModel);
        resultsList.addMouseListener(new ResultDownloader());
        
        /*
        SortedList sortedResults =
            new SortedList(listModel, new ResultComparator());
        EventTableModel tableModel =
            new EventTableModel(sortedResults, new ResultTableFormat());
        resultsTable = new JTable(tableModel);
        */
        resultsTable = null;
        
        setMode(Mode.LIST);
        
        SponsoredResultsPanel srp = createSponsoredResultsPanel();
        add(srp, BorderLayout.EAST);
    }

    private SponsoredResultsPanel createSponsoredResultsPanel() {
        SponsoredResultsPanel srp = new SponsoredResultsPanel();
        srp.addEntry("Advantage Consulting, Inc.\n" +
            "When you really can't afford to fail...\n" +
            "IT Staffing Solutions with an ADVANTAGE");
        srp.addEntry("Object Computing, Inc.\n" +
            "An OO Software Engineering Company");
        return srp;
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
    
    public void setMode(Mode mode) {
        Component component =
            mode == Mode.LIST ? resultsList :
            mode == Mode.TABLE ? resultsTable :
            null;
        add(component, BorderLayout.CENTER);
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
