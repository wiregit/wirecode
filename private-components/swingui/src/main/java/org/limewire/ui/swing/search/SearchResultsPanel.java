package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.SearchScrollPane;

import ca.odell.glazedlists.EventList;

import ca.odell.glazedlists.SortedList;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.HashMap;
import org.limewire.core.api.search.ResultType;
import org.limewire.ui.swing.components.FancyTab;

/**
 * This class displays search results in a panel.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultsPanel extends JPanel {
    
    private static Map<ResultType, SearchCategory> resultTypeToSearchCategoryMap =
        new HashMap<ResultType, SearchCategory>();

    static {
        resultTypeToSearchCategoryMap.put(
            ResultType.AUDIO, SearchCategory.AUDIO);
        resultTypeToSearchCategoryMap.put(
            ResultType.VIDEO, SearchCategory.VIDEO);
        resultTypeToSearchCategoryMap.put(
            ResultType.IMAGE, SearchCategory.IMAGE);
        resultTypeToSearchCategoryMap.put(
            ResultType.DOCUMENT, SearchCategory.DOCUMENT);
        resultTypeToSearchCategoryMap.put(
            ResultType.PROGRAM, SearchCategory.PROGRAM);
        resultTypeToSearchCategoryMap.put(
            ResultType.OTHER, SearchCategory.OTHER);
    }
    
    private EventList<VisualSearchResult> originalEventList;
    
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
    
    /** The sponsored results. */
    private final SponsoredResultsPanel sponsoredResultsPanel;
    
    /** The scroll pane embedding the search results & sponsored results. */
    private JScrollPane scrollPane;

    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchInfo searchInfo,
            @Assisted final EventList<VisualSearchResult> eventList,
            @Assisted Search search,
            ResultsContainerFactory containerFactory,
            SponsoredResultsPanel sponsoredResultsPanel,
            final SortAndFilterPanel sortAndFilterPanel) {

        originalEventList = eventList;
        
        setBackground(Color.LIGHT_GRAY);
        
        this.sponsoredResultsPanel = sponsoredResultsPanel;
        sponsoredResultsPanel.setVisible(false);
        this.sortAndFilterPanel = sortAndFilterPanel;
        this.scrollPane = new SearchScrollPane();

        final SortedList<VisualSearchResult> filteredList =
            sortAndFilterPanel.getFilteredAndSortedList(eventList);
        
        // The ResultsContainerFactory create method takes two parameters
        // which it passes to the ResultsContainer constructor
        // for the parameters annotated with @Assisted.
        this.resultsContainer = containerFactory.create(filteredList, search);

        sortAndFilterPanel.addFilterListener(new SearchFilterListener() {
            @Override
            public void searchFiltered() {
                SearchCategory category = getCategory(filteredList);
                updateCategory(category);
            }
        });

        sortAndFilterPanel.addModeListener(new ModeListener() {
            @Override
            public void setMode(Mode mode) {
                resultsContainer.setMode(mode);
                syncColumnHeader();
            }
        });

        SearchTabItems.SearchTabListener listener =
            new SearchTabItems.SearchTabListener() {
            @Override
            public void categorySelected(SearchCategory category) {
                sortAndFilterPanel.clearFilterBox();
                sortAndFilterPanel.setSearchCategory(category);
                updateCategory(category);
            }
        };
        
        searchTab =
            new SearchTabItems(searchInfo.getSearchCategory(), listener);
        searchTab.setEventList(eventList);

        for (Map.Entry<SearchCategory, Action> entry : searchTab.getResultCountActions()) {
            resultsContainer.synchronizeResultCount(
                entry.getKey(), entry.getValue());
        }
        
        layoutComponents();
    }

    private SearchCategory getCategory(EventList<VisualSearchResult> eventList) {
        SearchCategory searchCategory = SearchCategory.ALL;

        int count = eventList.size();
        if (count >= 1) {
            // Get the SearchCategory of the first result.
            VisualSearchResult vsr = eventList.get(0);
            searchCategory = getSearchCategory(vsr);

            // Verify that all the rest match ... or use ALL.
            for (int i = 1; i < count; i++) {
                vsr = eventList.get(i);
                // Checking for null in case the size of the list
                // changes while this is running.
                if (vsr == null) break;

                SearchCategory category = getSearchCategory(vsr);
                if (category != searchCategory) {
                    searchCategory = SearchCategory.ALL;
                    break;
                }
            }
        }

        return searchCategory;
    }

    /**
     * Gets the SearchCategory that corresponds to the ResultType
     * of a given VisualSearchResult.
     * @param vsr the VisualSearchResult
     * @return the SearchCategory
     */
    private static SearchCategory getSearchCategory(VisualSearchResult vsr) {
        ResultType resultType = vsr.getCategory();
        return resultTypeToSearchCategoryMap.get(resultType);
    }

    public EventList<VisualSearchResult> getOriginalEventList() {
        return originalEventList;
    }
    
    public void addSponsoredResults(List<SponsoredResult> sponsoredResults){
        for (SponsoredResult result : sponsoredResults){
            sponsoredResultsPanel.addEntry(result);
        }
        
        if (!sponsoredResultsPanel.isVisible()) {
            sponsoredResultsPanel.setVisible(true);
            syncColumnHeader();
        }
    }
    
    private void syncColumnHeader() {
        Component resultHeader = resultsContainer.getScrollPaneHeader();
        if (resultHeader == null) {
            // If no headers, use nothing special.
            scrollPane.setColumnHeaderView(null);
            sponsoredResultsPanel.setTitleVisible(true);
        } else if (!sponsoredResultsPanel.isVisible()) {
            // If sponsored results aren't visible, just use the actual header.
            scrollPane.setColumnHeaderView(resultHeader);
        } else {
            // Otherwise, create a combined panel that has both sponsored results & header.
            JXPanel headerPanel = new JXPanel();
            // Make sure this syncs with the layout for the results & sponsored results!
            headerPanel.setLayout(new MigLayout("hidemode 3, gap 0!, insets 0 0 0 0", "[]", "[grow][]"));
            headerPanel.add(resultHeader, "grow, push, alignx left, aligny top");
            headerPanel.add(sponsoredResultsPanel.createTitleLabel(), "aligny top, alignx right");
            scrollPane.setColumnHeaderView(headerPanel);
            sponsoredResultsPanel.setTitleVisible(false);
        }
    }

    private void syncSortBy() {
        if (searchTab != null) {
            FancyTab tab = searchTab.getSelectedTab();
            
            if (tab != null) {
                String title = tab.getTitle();
                SearchCategory category =
                    "All".equals(title) ? SearchCategory.ALL :
                    "Music".equals(title) ? SearchCategory.AUDIO :
                    "Videos".equals(title) ? SearchCategory.VIDEO :
                    "Images".equals(title) ? SearchCategory.IMAGE :
                    "Documents".equals(title) ? SearchCategory.DOCUMENT :
                    "Programs".equals(title) ? SearchCategory.PROGRAM :
                    SearchCategory.OTHER;

                sortAndFilterPanel.setSearchCategory(category);
            }
        }
    }
    
    private void layoutComponents() {
        MigLayout layout = new MigLayout(
                "insets 0 0 0 0, gap 0!",
                "[grow][grow]",
                "[][grow]");
        
        setLayout(layout);
        add(searchTab, "push, growy");
        add(sortAndFilterPanel, "wrap, align right");
        add(scrollPane, "span, grow");
        
        JXPanel bottom = new JXPanel() {
            @Override
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
                    int direction) {
                return 20;
            }
            
            @Override
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
                    int direction) {
                return 20;
            }
        };
        bottom.setScrollableTracksViewportHeight(false);
        bottom.setLayout(new MigLayout("hidemode 3, gap 0!, insets 0 0 0 0", "[]", "[grow][]"));
        bottom.add(resultsContainer, "grow, push, alignx left, aligny top");
        bottom.add(sponsoredResultsPanel, "aligny top, alignx right");
        scrollPane.setViewportView(bottom);
        syncColumnHeader();
    }

    private void updateCategory(SearchCategory category) {
        resultsContainer.showCategory(category);
        syncColumnHeader();
        syncSortBy();
    }
}