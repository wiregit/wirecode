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
        String panelTitle = "Results for \"" + info.getTitle() + "\"";
        navigator.addNavigablePanel(NavItem.LIMEWIRE, panelTitle, new SearchResultsPanel(info));
        navigator.selectNavigablePanel(NavItem.LIMEWIRE, panelTitle);
    }

}
