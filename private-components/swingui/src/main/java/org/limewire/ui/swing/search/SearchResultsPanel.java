package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.sponsored.SponsoredResult.LinkTarget;

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

    /**
     * NavTree used by SponsoredResultsPanel
     * 
     */
    private NavTree navTree;
    
    /**
     * StorePanel used by SponsoredResultsPanel
     * 
     */
    private StorePanel storePanel;

    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchInfo searchInfo,
            @Assisted EventList<VisualSearchResult> visualSearchResults,
            @Assisted Search search,
            NavTree navTree,
            StorePanel storePanel,
            ResultsContainerFactory containerFactory) {
        
        this.navTree = navTree;
        this.storePanel = storePanel;
        
        setBackground(Color.LIGHT_GRAY);
        
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
        for(Map.Entry<SearchCategory, Action> entry : searchTab.getResultCountActions()) {
            resultsContainer.synchronizeResultCount(entry.getKey(), entry.getValue());
        }
        
        layoutComponents();
        
        sortAndFilterPanel.addModeListener(resultsContainer);
    }

    private JComponent createSponsoredResultsPanel() {
        SponsoredResultsPanel srp = new SponsoredResultsPanel(navTree, storePanel);
        
        srp.addEntry(new SponsoredResult("Internal Ad", "a ad a daflad fajla\naldjfla awejl sdaf", 
                "store.limewire.com", "http://www.store.limewire.com/store/app/pages/help/Help/", 
                LinkTarget.STORE));
        
        srp.addEntry(new SponsoredResult("External Ad", "a ad a daflad fajla\naldjfla awejl sdaf", 
                "google.com", "http://google.com", 
                LinkTarget.EXTERNAL));
        
        JScrollPane sp = new JScrollPane(srp);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(sp);
        return p;
    }
    
    private void layoutComponents() {
        MigLayout layout = new MigLayout(
                "insets 0 0 0 0, gap 0!",
                "[grow][grow][125::]",
                "[][grow]");
        
        setLayout(layout);
        add(searchTab.getSearchTab(), "push, growy");
        add(sortAndFilterPanel, "span, wrap, align right");
        add(resultsContainer, "span 2, grow, push");
        add(createSponsoredResultsPanel(), "grow, align right");
    }
}
