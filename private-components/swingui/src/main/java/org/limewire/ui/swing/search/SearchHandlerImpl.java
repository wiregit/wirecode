package org.limewire.ui.swing.search;

import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;

public class SearchHandlerImpl implements SearchHandler {
    
    private final Navigator navigator;
    
    public SearchHandlerImpl(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void doSearch(SearchInfo info) {
        String panelTitle = info.getTitle();
        NavItem item = navigator.addNavigablePanel(NavCategory.SEARCH, panelTitle, new SearchResultsPanel(info), true);
        item.select();
    }

}
