package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel.ListViewTable;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This is the top-level container for the search results display.  
 * SearchResultsPanel contains several UI components, including the category
 * tab items, the sort and filter panel, the sponsored results, and the search
 * results tables.
 */
public class SearchResultsPanel extends JXPanel implements SponsoredResultsView, Disposable {
    
    /** Decorator used to set the appearance of the header bar. */
    private final HeaderBarDecorator headerBarDecorator;
    
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
    
    /** The label where text about your search started poorly or later is written. */
    private JLabel messageLabel;
    /** The panel that displays the {@link #messageLabel}. */
    private JPanel messagePanel;
    
    /** The class search warning panel. */
    private ClassicSearchWarningPanel classicSearchReminderPanel;
    
    /** Listener for changes in the view type. */
    private final SettingListener viewTypeListener;
    
    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;
    
    @Resource private Color tabHighlightTopGradientColor;
    @Resource private Color tabHighlightBottomGradientColor;
    @Resource private Color tabSelectionTopGradientColor;
    @Resource private Color tabSelectionBottomGradientColor;
    @Resource private Color tabSelectionTextColor;
    @Resource private Color tabSelectionBorderTopGradientColor;;
    @Resource private Color tabSelectionBorderBottomGradientColor;;

    private boolean lifeCycleComplete = true;

    private boolean fullyConnected = true;

    /**
     * Constructs a SearchResultsPanel with the specified components.
     */
    @AssistedInject
    public SearchResultsPanel(
            @Assisted SearchResultsModel searchResultsModel,
            ResultsContainerFactory containerFactory,
            SortAndFilterPanelFactory sortAndFilterFactory,
            SearchTabItemsFactory searchTabItemsFactory,
            SponsoredResultsPanel sponsoredResultsPanel,
            RowSelectionPreserver preserver,
            HeaderBarDecorator headerBarDecorator) {        

        GuiUtils.assignResources(this);
        
        this.searchResultsModel = searchResultsModel;
        this.headerBarDecorator = headerBarDecorator; 
        
        this.sponsoredResultsPanel = sponsoredResultsPanel;
        this.sponsoredResultsPanel.setVisible(false);
        
        // Create sort and filter components.
        this.sortAndFilterPanel = sortAndFilterFactory.create(searchResultsModel, preserver);
        
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollablePanel = new ScrollablePanel();
        configureEnclosingScrollPane();
        
        // Create results container with tables.
        this.resultsContainer = containerFactory.create(searchResultsModel, preserver);
        
        viewTypeListener = new SettingListener() {
            int oldSearchViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
            @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtilities.invokeLater(new Runnable() {
                   @Override
                   public void run() {
                       int newSearchViewTypeId = SwingUiSettings.SEARCH_VIEW_TYPE_ID.getValue();
                       if(newSearchViewTypeId != oldSearchViewTypeId) {
                           SearchViewType newSearchViewType = SearchViewType.forId(newSearchViewTypeId);
                           resultsContainer.setViewType(newSearchViewType);
                           syncScrollPieces();
                           oldSearchViewTypeId = newSearchViewTypeId;
                       }
                   }               
               });
            } 
        };
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.addSettingListener(viewTypeListener);

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
        
        searchTabItems = searchTabItemsFactory.create(
                searchResultsModel.getSearchCategory(), 
                searchResultsModel.getObservableSearchResults());
        searchTabItems.addSearchTabListener(listener);

        for (Map.Entry<SearchCategory, Action> entry : searchTabItems.getResultCountActions()) {
            resultsContainer.synchronizeResultCount(
                entry.getKey(), entry.getValue());
        }

        messageLabel = new JLabel();
        messagePanel = new JPanel();
        messagePanel.add(messageLabel);
        messagePanel.setVisible(false);
        
        classicSearchReminderPanel = new ClassicSearchWarningPanel();
        layoutComponents();
    }

    /**
     * Disposes of resources used by the container.  This method is called when 
     * the search is closed.
     */
    @Override
    public void dispose() {
        SwingUiSettings.SEARCH_VIEW_TYPE_ID.removeSettingListener(viewTypeListener);
        sortAndFilterPanel.dispose();
        classicSearchReminderPanel.dispose();
        searchResultsModel.stop();
    }

    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane() {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }
    
    /**
     * Adds the specified list of sponsored results to the display.
     */
    @Override
    public void addSponsoredResults(List<SponsoredResult> sponsoredResults){
        for (SponsoredResult result : sponsoredResults){
            sponsoredResultsPanel.addEntry(result);
        }
        
        if (!sponsoredResultsPanel.isVisible()) {
            sponsoredResultsPanel.setVisible(true);
            syncColumnHeader();
        }
    }
    
    /**
     * Updates the column header component in the scroll pane.  This depends on
     * the current results view and whether the sponsored results are visible. 
     */
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
            header.setDefaultRenderer(new TableCellHeaderRenderer());
            header.setReorderingAllowed(false);
            header.setResizingAllowed(false);
            header.setTable(new JXTable(0, 1));
            
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
    
    /**
     * Initializes the components and adds them to the container.  Called by
     * the constructor.  
     */
    private void layoutComponents() {
        MigLayout layout = new MigLayout("hidemode 2, insets 0 0 0 0, gap 0!", 
        		                "[grow]", "[][][][grow]");
        
        setLayout(layout);
        setMinimumSize(new Dimension(getPreferredSize().width, 33));
        
        RectanglePainter tabHighlight = new RectanglePainter();
        tabHighlight.setFillPaint(new GradientPaint(20.0f, 0.0f, tabHighlightTopGradientColor, 
                                                    20.0f, 33.0f, tabHighlightBottomGradientColor));

        tabHighlight.setInsets(new Insets(0,0,1,0));
        tabHighlight.setBorderPaint(null);
        
        FancyTabList searchTab = searchTabItems.getSearchTab();
        
        searchTab.setUnderlineEnabled(false);
        searchTab.setHighlightPainter(tabHighlight);
        
        searchTab.setSelectionPainter(createTabSelectionPainter());
        searchTab.setTabTextSelectedColor(tabSelectionTextColor);
        
        HeaderBar header = new HeaderBar(searchTab);
        headerBarDecorator.decorateBasic(header);
        
        sortAndFilterPanel.layoutComponents(header);
        add(header, "growx, wrap");
        add(classicSearchReminderPanel, "growx, wrap");
        add(messagePanel, "growx, wrap");
        add(scrollPane, "grow, wrap");

        scrollablePanel.setScrollableTracksViewportHeight(false);
        scrollablePanel.setLayout(new MigLayout("hidemode 3, gap 0, insets 0", "[]", "[grow][]"));
        
        scrollablePanel.add(resultsContainer, "grow, push, alignx left, aligny top");
        scrollablePanel.add(sponsoredResultsPanel, "aligny top, alignx right, wmin " + sponsoredResultsPanel.getPreferredSize().width + ", pad 8 8 8 0");
        scrollPane.setViewportView(scrollablePanel);
        syncScrollPieces();
    }

    /**
     * Updates the view components in the scroll pane. 
     */
    private void syncScrollPieces() {
        scrollablePanel.setScrollable(resultsContainer.getScrollable());
        syncColumnHeader();
    }
    
    /**
     * Panel used as the viewport view in the scroll pane.  This contains the
     * results table and sponsored results panel in a single, scrollable area.
     */
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
                
                // the list view has some weird rendering sometimes (double space after last result)
                // so don't fill full screen on list view
                if( (scrollable instanceof ListViewTable)) {
                    // old check, if sponsored results aren't showing properlly revert to just using this
                    if(sponsoredResultsPanel.isVisible()) {
                        height = Math.max(height, sponsoredResultsPanel.getPreferredSize().height);
                    }
                } else { // classic view
                    int headerHeight = 0;
                    
                    //the table headers aren't being set on the scrollpane, so if its visible check its
                    // height and subtract it from the viewport size
                    JTableHeader header = ((JTable)scrollable).getTableHeader();
                    if(header != null && header.isShowing()) {
                        headerHeight = header.getHeight();
                    }
                    
                    // if the height of table is less than the scrollPane height, set preferred height
                    // to same size as scrollPane
                    if(height < scrollPane.getSize().height - headerHeight) {
                        height = scrollPane.getSize().height - headerHeight;
                    }
                }
                return new Dimension(width, height);
            }
        }
        
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
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

    /**
     * Sets an indicator to determine whether the application has finished
     * loading, and updates the user message.
     */
    public void setLifeCycleComplete(boolean lifeCycleComplete) {
        this.lifeCycleComplete = lifeCycleComplete;
        updateMessages();
    }

    /**
     * Sets an indicator to determine whether the application is fully 
     * connected to the P2P network, and updates the user message.
     */
    public void setFullyConnected(boolean fullyConnected) {
        this.fullyConnected = fullyConnected;
        updateMessages();        
    }
    
    /**
     * Updates the user message based on the current state of the application. 
     */
    private void updateMessages() {
        if (!lifeCycleComplete) {
            messageLabel.setText(I18n.tr("LimeWire will start your search right after it finishes loading."));
            messagePanel.setVisible(true);
        } else if (!fullyConnected) {
            messageLabel.setText(I18n.tr("You might not receive many results until LimeWire finishes loading..."));
            messagePanel.setVisible(true);
        } else {
            messagePanel.setVisible(false);
        }
    }
    
    /**
     * Creates a Painter used to render the selected category tab.
     */
    private AbstractPainter<FancyTab> createTabSelectionPainter() {
        RectanglePainter<FancyTab> painter = new RectanglePainter<FancyTab>();
        
        painter.setFillPaint(new GradientPaint(0, 0, tabSelectionTopGradientColor, 
                0, 1, tabSelectionBottomGradientColor));
        painter.setBorderPaint(new GradientPaint(0, 0, tabSelectionBorderTopGradientColor, 
                0, 1, tabSelectionBorderBottomGradientColor));
        
        painter.setRoundHeight(10);
        painter.setRoundWidth(10);
        painter.setRounded(true);
        painter.setPaintStretched(true);
        painter.setInsets(new Insets(6,0,7,0));
                
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        return painter;
    }
}
