package org.limewire.ui.swing.search;

import java.util.Locale;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.mainframe.MainPanel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class SearchHandlerImpl implements SearchHandler {
    
    private final SearchHandler p2pLinkSearch;
    private final SearchHandler textSearch;
    private final MainPanel mainPanel;
    
    @Inject
    public SearchHandlerImpl(@Named("p2p://") SearchHandler p2pLinkSearch,
                        @Named("text") SearchHandler textSearch,
                        MainPanel mainPanel) {
        this.p2pLinkSearch = p2pLinkSearch;
        this.textSearch = textSearch;
        this.mainPanel = mainPanel;
    }
    
    @Override
    public boolean doSearch(SearchInfo info) {
        if(info.getSearchCategory() == SearchCategory.PROGRAM && !LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            mainPanel.showTemporaryPanel(new ProgramsNotAllowedPanel());
            return false;
        } else {                    
            String q = info.getSearchQuery();
            if(q != null && q.toLowerCase(Locale.US).startsWith("p2p://")) {
                return p2pLinkSearch.doSearch(info);
            } else {
                return textSearch.doSearch(info);
            }
        }
    }

}
