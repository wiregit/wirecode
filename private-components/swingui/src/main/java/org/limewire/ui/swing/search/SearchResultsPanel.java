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

public class SearchResultsPanel extends JPanel {
    
    private final SearchTabItems searchTab;
    private final ResultsContainer resultsContainer;
    private final SortAndFilterPanel sortAndFilterPanel;

    @AssistedInject public SearchResultsPanel(@Assisted SearchInfo searchInfo,
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
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(searchTab, gbc);
        
        gbc.weighty = 1;
        add(resultsContainer, gbc);
        
        gbc.weighty = 0;
        add(sortAndFilterPanel, gbc);
    }

}
