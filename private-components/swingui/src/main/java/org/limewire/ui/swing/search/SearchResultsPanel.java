package org.limewire.ui.swing.search;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

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
        this.resultsContainer = containerFactory.create(sortAndFilterPanel.getSortedAndFilteredList(visualSearchResults),search);
        this.searchTab = new SearchTabItems(searchInfo.getSearchCategory(), new SearchTabItems.SearchTabListener() {
            @Override
            public void categorySelected(SearchCategory searchCategory) {
                resultsContainer.showCategory(searchCategory);
            }
        });
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        add(searchTab, gbc);
        
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(sortAndFilterPanel, gbc);
        
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(resultsContainer, gbc);
    }
}
