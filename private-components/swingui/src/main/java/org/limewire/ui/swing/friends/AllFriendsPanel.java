package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.BrowsePanelFactory;
import org.limewire.ui.swing.search.SearchResultsPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AllFriendsPanel extends JPanel {
    
    private final Provider<BrowseSearchFactory> browseSearchFactory;

    private final BrowsePanelFactory browsePanelFactory;

    @Inject
    public AllFriendsPanel(Provider<BrowseSearchFactory> browseSearchFactory, BrowsePanelFactory browsePanelFactory){
        super(new BorderLayout());
        this.browseSearchFactory = browseSearchFactory;
        this.browsePanelFactory = browsePanelFactory;
        initialize();
    }
    
    private void initialize(){
        BrowseSearch search = browseSearchFactory.get().createAllFriendsBrowseSearch();
        final SearchResultsPanel panel = browsePanelFactory.createBrowsePanel(search);
        add(panel);
        
        panel.getModel().start(new SearchListener(){
            @Override
            public void handleSearchResult(Search search, final SearchResult searchResult) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        panel.getModel().addSearchResult(searchResult);
                    }
                });
            }

            @Override
            public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {}

            @Override
            public void searchStarted(Search search) {}

            @Override
            public void searchStopped(Search search) {}}
        );     
        
        panel.setBrowseTitle(I18n.tr("All Friends"));

    }
}
