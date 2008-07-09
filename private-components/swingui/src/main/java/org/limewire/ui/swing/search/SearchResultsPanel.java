package org.limewire.ui.swing.search;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.core.api.search.SearchResult;

public class SearchResultsPanel extends JPanel {

    private final SearchInfo searchInfo;

    public SearchResultsPanel(SearchInfo searchInfo) {
        this.searchInfo = searchInfo;
        add(new JLabel(searchInfo.getTitle()));
    }

    public void addSearchResult(SearchResult searchResult) {
        // TODO Auto-generated method stub
        
    }

}
