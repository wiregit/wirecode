package org.limewire.ui.swing.search;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public class SearchResultsPanel extends JPanel {

    private final SearchInfo searchInfo;
    
    private final SearchTabItems searchTab;
    private final ResultsContainer resultsContainer;
    private final SortAndFilterPanel sortAndFilterPanel;

    public SearchResultsPanel(SearchInfo searchInfo, EventList<VisualSearchResult> visualSearchResults) {
        this.searchInfo = searchInfo;
        this.resultsContainer = new ResultsContainer(visualSearchResults);
        this.searchTab = new SearchTabItems(searchInfo.getSearchCategory(), new SearchTabItems.SearchTabListener() {
            @Override
            public void categorySelected(SearchCategory searchCategory) {
                resultsContainer.showCategory(searchCategory);
            }
        });
        this.sortAndFilterPanel = new SortAndFilterPanel();
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(searchTab, gbc);
        
        gbc.weighty = 1;
        add(resultsContainer, gbc);
        
        gbc.weighty = 0;
        add(sortAndFilterPanel, gbc);
        
        
    }

    public void addSearchResult(SearchResult searchResult) {
        // TODO Auto-generated method stub
        
    }

}
