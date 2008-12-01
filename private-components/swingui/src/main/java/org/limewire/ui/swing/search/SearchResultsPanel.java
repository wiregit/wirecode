package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.settings.SearchSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class displays search results in a panel.
 */
public class SearchResultsPanel extends JXPanel {
    private final Log LOG = LogFactory.getLog(getClass());
       
    private final LimeHeaderBarFactory headerBarFactory;
    
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
    
    private JLabel messageLabel;
    private JPanel messagePanel;
    
    @Resource private Color tabHighlightTopGradientColor;
    @Resource private Color tabHighlightBottomGradientColor;
    @Resource private Color tabSelectionTopGradientColor;
    @Resource private Color tabSelectionBottomGradientColor;
    @Resource private Color tabSelectionTextColor;
    @Resource private Color tabSelectionBorderTopGradientColor;;
    @Resource private Color tabSelectionBorderBottomGradientColor;;
    

    private boolean lifeCycleComplete = true;

    private boolean fullyConnected = true;
    
    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchInfo searchInfo,
            @Assisted final EventList<VisualSearchResult> eventList,
            @Assisted Search search,
            ResultsContainerFactory containerFactory,
            SearchTabItemsFactory searchTabItemsFactory,
            SponsoredResultsPanel sponsoredResultsPanel,
            final SortAndFilterPanel sortAndFilterPanel,
            RowSelectionPreserver preserver,
            LimeHeaderBarFactory headerBarFactory) {        

        GuiUtils.assignResources(this);
        
        this.headerBarFactory = headerBarFactory; 
        
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
        
        //TODO this is not a singleton, need to remove the listener when we are done with this panel
        SearchSettings.SEARCH_VIEW_TYPE_ID.addSettingListener( new SettingListener() {
           int oldSearchViewTypeId = SearchSettings.SEARCH_VIEW_TYPE_ID.getValue();
           @Override
            public void settingChanged(SettingEvent evt) {
               int newSearchViewTypeId = SearchSettings.SEARCH_VIEW_TYPE_ID.getValue();
               if(newSearchViewTypeId != oldSearchViewTypeId) {
                   SearchViewType newSearchViewType = SearchViewType.forId(newSearchViewTypeId);
                   resultsContainer.setViewType(newSearchViewType);
                   syncScrollPieces();
                   oldSearchViewTypeId = newSearchViewTypeId;
               }
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
        
        searchTabItems = searchTabItemsFactory.create(searchInfo.getSearchCategory(), eventList);
        searchTabItems.addSearchTabListener(listener);

        for (Map.Entry<SearchCategory, Action> entry : searchTabItems.getResultCountActions()) {
            resultsContainer.synchronizeResultCount(
                entry.getKey(), entry.getValue());
        }

        messageLabel = new JLabel();
        messagePanel = new JPanel();
        messagePanel.add(messageLabel);
        messagePanel.setVisible(false);
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
        } else if (!sponsoredResultsPanel.isVisible()) {
            // If sponsored results aren't visible, just use the actual header.
            scrollPane.setColumnHeaderView(resultHeader);
        } else {
            // Otherwise, create a combined panel that has both sponsored results & header.
            JXPanel headerPanel = new JXPanel();
            // Make sure this syncs with the layout for the results & sponsored results!
            headerPanel.setLayout(new MigLayout("hidemode 3, gap 0, insets 0", "[]", "[grow][]"));
            headerPanel.add(resultHeader, "grow, push, alignx left, aligny top");
            
            DefaultTableColumnModel model = new DefaultTableColumnModel();
            TableColumn column = new TableColumn();
            model.addColumn(column);
            JTableHeader header = new JTableHeader(model);
            header.setReorderingAllowed(false);
            header.setResizingAllowed(false);
            header.setTable(new JTable());
            int width = sponsoredResultsPanel.getPreferredSize().width;
            int height = resultHeader.getPreferredSize().height;
            column.setWidth(width);
            Dimension dimension = new Dimension(width, height);
            header.setPreferredSize(dimension);
            header.setMaximumSize(dimension);
            header.setMinimumSize(dimension);
            
            headerPanel.add(header, "aligny top, alignx right");
            scrollPane.setColumnHeaderView(headerPanel);
        }
        
        scrollPane.validate();
    }
        
    private void layoutComponents() {
        MigLayout layout = new MigLayout(
                "hidemode 2, insets 0 0 0 0, gap 0!",
                "[grow]",
                "[][][grow]");
        
        setLayout(layout);
        setMinimumSize(new Dimension(getPreferredSize().width, 33));
        
        RectanglePainter tabHighlight = new RectanglePainter();
        tabHighlight.setFillPaint(new GradientPaint(20.0f, 0.0f, tabHighlightTopGradientColor, 
                                                    20.0f, 33.0f, tabHighlightBottomGradientColor));
        
        tabHighlight.setBorderPaint(null);
        
        FancyTabList searchTab = searchTabItems.getSearchTab();
        
        searchTab.setUnderlineEnabled(false);
        searchTab.setHighlightPainter(tabHighlight);
        
        searchTab.setSelectionPainter(createTabSelectionPainter());
        searchTab.setTabTextSelectedColor(tabSelectionTextColor);
        

        LimeHeaderBar header = headerBarFactory.createBasic(searchTab);
        sortAndFilterPanel.layoutComponents(header);
        add(header, "wrap");
        
        add(messagePanel, "span, growx");
        add(scrollPane, "span, grow");

        scrollablePanel.setScrollableTracksViewportHeight(false);
        scrollablePanel.setLayout(new MigLayout("hidemode 3, gap 0, insets 0", "[]", "[grow][]"));
        
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
                int width = super.getPreferredSize().width;
                int height = ((JComponent)scrollable).getPreferredSize().height;
                if(sponsoredResultsPanel.isVisible()) {
                    height = Math.max(height, sponsoredResultsPanel.getPreferredSize().height);
                }
                return new Dimension(width, height);
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

    public void setLifeCycleComplete(boolean lifeCycleComplete) {
        this.lifeCycleComplete = lifeCycleComplete;
        updateMessages();
    }

    public void setFullyConnected(boolean fullyConnected) {
        this.fullyConnected = fullyConnected;
        updateMessages();        
    }
    
    private void updateMessages() {
        if(!lifeCycleComplete) {
            messageLabel.setText(I18n.tr("TODO get better copy... LimeWire is currently starting. When it has completed, your search will continue."));
            messagePanel.setVisible(true);
        } else if(!fullyConnected) {
            messageLabel.setText(I18n.tr("TODO get better copy... LimeWire is not fully connected. You will not receive many search results until it finishes connecting."));
            messagePanel.setVisible(true);
        } else {
            messagePanel.setVisible(false);
        }
    }
    
    private AbstractPainter<FancyTab> createTabSelectionPainter() {
        AbstractPainter<FancyTab> painter = new AbstractPainter<FancyTab>() {

            @Override
            protected void doPaint(Graphics2D g, FancyTab object, int width, int height) {
                g.setPaint(new GradientPaint(20.0f, 0.0f, tabSelectionTopGradientColor, 
                        20.0f, 33.0f, tabSelectionBottomGradientColor));
                
                g.fillRoundRect(4,0, width-1-6, height, 10,10);
                
                g.setPaint(new GradientPaint(20.0f, 0.0f, tabSelectionBorderTopGradientColor, 
                        20.0f, 33.0f, tabSelectionBorderBottomGradientColor));
                
                g.drawRoundRect(4,0, width-1-6, height, 10,10);  
           }
        };
        
        painter.setAntialiasing(true);
        return painter;
    }
}