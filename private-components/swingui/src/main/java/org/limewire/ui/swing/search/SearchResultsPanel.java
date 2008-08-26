package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.SearchScrollPane;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import javax.swing.BorderFactory;

/**
 * This class displays search results in a panel.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultsPanel extends JPanel {
    
    /**
     * This is the subpanel that appears in the upper-left corner
     * of each search results tab.  It displays the numbers of results
     * found for each file type.
     */
    private final SearchTabItems searchTab;
    
    /**
     * This is the subpanel that displays the actual search results.
     */
    private final ResultsContainer resultsContainer;
    
    /**
     * This is the subpanel that appears in the upper-right corner
     * of each search results tab.
     */
    private final SortAndFilterPanel sortAndFilterPanel;
    
    /** The sponsored results. */
    private final SponsoredResultsPanel sponsoredResultsPanel;
    
    /** The scroll pane embedding the search results & sponsored results. */
    private JScrollPane scrollPane;

    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchInfo searchInfo,
            @Assisted EventList<VisualSearchResult> visualSearchResults,
            @Assisted Search search,
            ResultsContainerFactory containerFactory,
            SponsoredResultsPanel sponsoredResultsPanel) {
        
        setBackground(Color.LIGHT_GRAY);
        
        this.sponsoredResultsPanel = sponsoredResultsPanel;
        sponsoredResultsPanel.setVisible(false);
        this.sortAndFilterPanel = new SortAndFilterPanel();
        this.scrollPane = new SearchScrollPane();
        EventList<VisualSearchResult> list =
            sortAndFilterPanel.getSortedAndFilteredList(visualSearchResults);
        
        this.resultsContainer = containerFactory.create(list, search);
        sortAndFilterPanel.addModeListener(new ModeListener() {
            @Override
            public void setMode(Mode mode) {
                resultsContainer.setMode(mode);
                syncColumnHeader();
            }
        });
        
        SearchTabItems.SearchTabListener listener =
            new SearchTabItems.SearchTabListener() {
            @Override
            public void categorySelected(SearchCategory searchCategory) {
                resultsContainer.showCategory(searchCategory);
                syncColumnHeader();
            }
        };
        
        this.searchTab =
            new SearchTabItems(searchInfo.getSearchCategory(), listener);
        this.searchTab.setEventList(visualSearchResults);

        for (Map.Entry<SearchCategory, Action> entry : searchTab.getResultCountActions()) {
            resultsContainer.synchronizeResultCount(
                entry.getKey(), entry.getValue());
        }
        
        layoutComponents();

        //setBorder(BorderFactory.createTitledBorder(
        //    BorderFactory.createLineBorder(Color.RED, 1), "SearchResultsPanel"));
    }
    
    public void addSponsoredResults(List<SponsoredResult> sponsoredResults){
        for(SponsoredResult result : sponsoredResults){
            sponsoredResultsPanel.addEntry(result);
        }
        
        if(!sponsoredResultsPanel.isVisible()) {
            sponsoredResultsPanel.setVisible(true);
            syncColumnHeader();
        }
    }
    
    private void syncColumnHeader() {
        Component resultHeader = resultsContainer.getScrollPaneHeader();
        if (resultHeader == null) {
            // If no headers, use nothing special.
            scrollPane.setColumnHeaderView(null);
            sponsoredResultsPanel.setTitleVisible(true);
        } else if (!sponsoredResultsPanel.isVisible()) {
            // If sponsored results aren't visible, just use the actual header.
            scrollPane.setColumnHeaderView(resultHeader);
        } else {
            // Otherwise, create a combined panel that has both sponsored results & header.
            JXPanel headerPanel = new JXPanel();
            // Make sure this syncs with the layout for the results & sponsored results!
            headerPanel.setLayout(new MigLayout("hidemode 3, gap 0!, insets 0 0 0 0", "[]", "[grow][]"));
            headerPanel.add(resultHeader, "grow, push, alignx left, aligny top");
            headerPanel.add(sponsoredResultsPanel.createTitleLabel(), "aligny top, alignx right");
            scrollPane.setColumnHeaderView(headerPanel);
            sponsoredResultsPanel.setTitleVisible(false);
        }
    }
    
    private void layoutComponents() {
        MigLayout layout = new MigLayout(
                "insets 0 0 0 0, gap 0!",
                "[grow][grow]",
                "[][grow]");
        
        setLayout(layout);
        //add(searchTab.getSearchTab(), "push, growy");
        add(searchTab, "push, growy");
        add(sortAndFilterPanel, "wrap, align right");
        add(scrollPane, "span, grow");
        
        JXPanel bottom = new JXPanel() {
            @Override
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
                    int direction) {
                return 20;
            }
            
            @Override
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
                    int direction) {
                return 20;
            }
        };
        bottom.setScrollableTracksViewportHeight(false);
        bottom.setLayout(new MigLayout("hidemode 3, gap 0!, insets 0 0 0 0", "[]", "[grow][]"));
        bottom.add(resultsContainer, "grow, push, alignx left, aligny top");
        bottom.add(sponsoredResultsPanel, "aligny top, alignx right");
        scrollPane.setViewportView(bottom);
        syncColumnHeader();
    }
}
