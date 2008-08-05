package org.limewire.ui.swing.search;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;

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

    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchInfo searchInfo,
            @Assisted EventList<VisualSearchResult> visualSearchResults,
            @Assisted Search search,
            ResultsContainerFactory containerFactory) {
        
        this.sortAndFilterPanel = new SortAndFilterPanel();
        EventList<VisualSearchResult> list =
            sortAndFilterPanel.getSortedAndFilteredList(visualSearchResults);
        
        this.resultsContainer = containerFactory.create(list, search);
        sortAndFilterPanel.addModeListener(resultsContainer);
        
        SearchTabItems.SearchTabListener listener =
            new SearchTabItems.SearchTabListener() {
            @Override
            public void categorySelected(SearchCategory searchCategory) {
                resultsContainer.showCategory(searchCategory);
            }
        };
        
        this.searchTab =
            new SearchTabItems(searchInfo.getSearchCategory(), listener);
        
        layoutComponents();
        
        sortAndFilterPanel.addModeListener(resultsContainer);
    }

    private JComponent createSponsoredResultsPanel() {
        SponsoredResultsPanel srp = new SponsoredResultsPanel();
        srp.addEntry("Advantage Consulting, Inc.\n" +
            "When you really can't afford to fail...\n" +
            "IT Staffing Solutions with an ADVANTAGE");
        srp.addEntry("Object Computing, Inc.\n" +
            "An OO Software Engineering Company");
        JScrollPane sp = new JScrollPane(srp);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(sp);
        return p;
    }
    
    private void layoutComponents() {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(searchTab, BorderLayout.CENTER);
        northPanel.add(sortAndFilterPanel, BorderLayout.EAST);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(resultsContainer, BorderLayout.CENTER);
        centerPanel.add(createSponsoredResultsPanel(), BorderLayout.EAST);
        
        setLayout(new BorderLayout());
        add(northPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }
}
