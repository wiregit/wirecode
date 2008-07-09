package org.limewire.ui.swing.search;

import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavItem;

public class SearchHandlerImpl implements SearchHandler {
    
    private final Navigator navigator;
    
    public SearchHandlerImpl(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void doSearch(SearchInfo info) {
        String panelTitle = info.getTitle();
        navigator.addNavigablePanel(NavItem.SEARCH, panelTitle, new SearchResultsPanel(info), true);
        navigator.selectNavigablePanel(NavItem.SEARCH, panelTitle);
    }

}
