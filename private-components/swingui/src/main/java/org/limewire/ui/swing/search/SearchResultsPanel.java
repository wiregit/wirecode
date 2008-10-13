package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class displays search results in a panel.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultsPanel extends JXPanel {
    private final Log LOG = LogFactory.getLog(getClass());
        
    /**
     * This is the subpanel that appears in the upper-left corner
     * of each search results tab.  It displays the numbers of results
     * found for each file type.
     */
    private final SearchTabItems searchTabItems;
    
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
    
    /** The ScrollablePanel that the scroll pane is embedding. */
    private ScrollablePanel scrollablePanel;
    
    @Resource private Color toolbarTopGradientColor;
    @Resource private Color toolbarBottomGradientColor;
    @Resource private Color tabHighlightTopGradientColor;
    @Resource private Color tabHighlightBottomGradientColor;
    
    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchInfo searchInfo,
            @Assisted final EventList<VisualSearchResult> eventList,
            @Assisted Search search,
            ResultsContainerFactory containerFactory,
            SponsoredResultsPanel sponsoredResultsPanel,
            final SortAndFilterPanel sortAndFilterPanel,
            RowSelectionPreserver preserver) {        

        GuiUtils.assignResources(this);
        
        RectanglePainter gradientPainter = new RectanglePainter();
        gradientPainter.setFillPaint(new GradientPaint(50.0f, 0.0f, toolbarTopGradientColor, 
                                                       50.0f, 33.0f, toolbarBottomGradientColor));
        gradientPainter.setBorderPaint(null);
        gradientPainter.setBorderWidth(0.0f);
        setBackgroundPainter(gradientPainter);
        
        sortAndFilterPanel.setBackgroundPainter(gradientPainter);
        
        this.sponsoredResultsPanel = sponsoredResultsPanel;
        sponsoredResultsPanel.setVisible(false);
        this.sortAndFilterPanel = sortAndFilterPanel;
        this.scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        this.scrollablePanel = new ScrollablePanel();

        final EventList<VisualSearchResult> filteredList =
            sortAndFilterPanel.getFilteredAndSortedList(newVisibleFilterList(eventList), preserver);
        
        // The ResultsContainerFactory create method takes two parameters
        // which it passes to the ResultsContainer constructor
        // for the parameters annotated with @Assisted.
        this.resultsContainer = containerFactory.create(filteredList, search, searchInfo, preserver);

        // what is this for?
//        sortAndFilterPanel.addFilterListener(new SearchFilterListener() {
//            @Override
//            public void searchFiltered() {
//                SearchCategory category = getCategory(filteredList);
//                updateCategory(category);
//            }
//        });

        sortAndFilterPanel.addModeListener(new ModeListener() {
            @Override
            public void setMode(Mode mode) {
                resultsContainer.setMode(mode);
                syncScrollPieces();
            }
        });

        SearchTabItems.SearchTabListener listener =
            new SearchTabItems.SearchTabListener() {
            @Override
            public void categorySelected(SearchCategory category) {
                sortAndFilterPanel.clearFilterBox();
                sortAndFilterPanel.setSearchCategory(category);
                resultsContainer.showCategory(category);
                syncScrollPieces();
            }
        };
        
        searchTabItems = new SearchTabItems(searchInfo.getSearchCategory(), eventList);
        searchTabItems.addSearchTabListener(listener);

        for (Map.Entry<SearchCategory, Action> entry : searchTabItems.getResultCountActions()) {
            resultsContainer.synchronizeResultCount(
                entry.getKey(), entry.getValue());
        }
        
        layoutComponents();
    }
    
    private EventList<VisualSearchResult> newVisibleFilterList(
            EventList<VisualSearchResult> eventList) {
        return GlazedListsFactory.filterList(eventList, new Matcher<VisualSearchResult>() {
            @Override
            public boolean matches(VisualSearchResult item) {
                boolean visible = item.isVisible();
                LOG.debugf("filter... VSR urn {0} visibility {1}", item.getCoreSearchResults().get(0).getUrn(), visible);
                return visible;
            }
        });
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
        
    private void layoutComponents() {
        MigLayout layout = new MigLayout(
                "insets 0 0 0 0, gap 0!",
                "[grow][grow]",
                "[][grow]");
        
        setLayout(layout);
        setMinimumSize(new Dimension(getPreferredSize().width, 33));
        
        RectanglePainter tabHighlight = new RectanglePainter();
        tabHighlight.setFillPaint(new GradientPaint(20.0f, 0.0f, tabHighlightTopGradientColor, 
                                                    20.0f, 33.0f, tabHighlightBottomGradientColor));
        tabHighlight.setBorderPaint(null);
        
        FancyTabList searchTab = searchTabItems.getSearchTab();
        searchTab.setHighlightPainter(tabHighlight);
        
        searchTab.setSelectionPainter(tabHighlight);
        searchTab.setTabTextSelectedColor(Color.WHITE);
        
        add(searchTab, "growy");
        add(sortAndFilterPanel, "wrap, align right");
        add(scrollPane, "span, grow");
        
        scrollablePanel.setScrollableTracksViewportHeight(false);
        scrollablePanel.setLayout(new MigLayout("hidemode 3, gap 0!, insets 0 0 0 0", "[]", "[grow][]"));
        scrollablePanel.add(resultsContainer, "grow, push, alignx left, aligny top");
        scrollablePanel.add(sponsoredResultsPanel, "aligny top, alignx right, wmin 140, pad 8 8 8 0");
        scrollPane.setViewportView(scrollablePanel);
        syncScrollPieces();
    }

    private void syncScrollPieces() {
        scrollablePanel.setScrollable(resultsContainer.getScrollable());
        syncColumnHeader();
    }
    
    private class ScrollablePanel extends JXPanel {
        private Scrollable scrollable;

        public void setScrollable(Scrollable scrollable) {
            this.scrollable = scrollable;
        }
        
        @Override
        public Dimension getPreferredSize() {
            if(scrollable == null) {
                return super.getPreferredScrollableViewportSize();
            } else {
                return new Dimension(super.getPreferredSize().width, ((JComponent)scrollable).getPreferredSize().height);
            }
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            if(scrollable == null) {
                return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
            } else {
                return scrollable.getScrollableUnitIncrement(visibleRect, orientation, direction);
            }
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            if(scrollable == null) {
                return super.getScrollableBlockIncrement(visibleRect, orientation, direction);
            } else {
                return scrollable.getScrollableBlockIncrement(visibleRect, orientation, direction);
            }
        }
    }
}