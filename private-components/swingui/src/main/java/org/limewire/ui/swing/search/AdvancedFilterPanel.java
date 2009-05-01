package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.components.SideLineBorder;
import org.limewire.ui.swing.components.SideLineBorder.Side;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.filter.CategoryDetector;
import org.limewire.ui.swing.search.filter.CategoryFilter;
import org.limewire.ui.swing.search.filter.Filter;
import org.limewire.ui.swing.search.filter.FilterListener;
import org.limewire.ui.swing.search.filter.FilterManager;
import org.limewire.ui.swing.search.filter.FilterableItem;
import org.limewire.ui.swing.search.filter.FilterableItemTextFilterator;
import org.limewire.ui.swing.search.filter.FilterableSource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

/**
 * Filter panel for filterable data.  AdvancedFilterPanel presents advanced 
 * filtering options, including an input text field and category-specific 
 * property filters. 
 */
public class AdvancedFilterPanel<E extends FilterableItem> extends JPanel implements Disposable {

    @Resource(key="AdvancedFilter.filterWidth") private int filterWidth;
    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    @Resource private Color dividerBackgroundColor;
    @Resource private Color dividerForegroundColor;
    @Resource private Color moreTextColor;
    @Resource private Font moreTextFont;
    @Resource private Color resetTextColor;
    @Resource private Font resetTextFont;
    
    /** Filterable data source. */
    private final FilterableSource<E> filterableSource;

    /** List of editors being used for filtering. */
    private final EventList<MatcherEditor<E>> editorList;

    /** Manager for filters. */
    private final FilterManager<E> filterManager;

    /** List of category selection listeners. */
    private final List<CategoryListener> listenerList = new ArrayList<CategoryListener>();

    /** Filter for file category. */
    private final CategoryFilter<E> categoryFilter;
    
    /** Filter for file source. */
    private final Filter<E> sourceFilter;
    
    /** Text field for text filter. */
    private final PromptTextField filterTextField = new PromptTextField(I18n.tr("Refine results..."));
    
    /** Container to display active filters. */
    private final FilterDisplayPanel filterDisplayPanel;
    
    /** Container to display filter components. */
    private final JPanel filterPanel = new JPanel();
    
    private final JSeparator separator = new JSeparator();
    private final JScrollPane filterScrollPane = new JScrollPane();
    
    /** Container for category-specific filters. */
    private final PropertyFilterPanel propertyPanel;
    
    /** Default display category; determines the default table format. */
    private SearchCategory defaultDisplayCategory;
    
    /** Category that determines the default filters to display. */
    private SearchCategory defaultFilterCategory;
    
    /** Indicator that determines whether filter layout is being adjusted. */
    private boolean layoutAdjusting;

    /**
     * Constructs a FilterPanel with the specified filterable data source
     * and UI decorators.
     */
    public AdvancedFilterPanel(FilterableSource<E> filterableSource,
            TextFieldDecorator textFieldDecorator,
            FriendActions friendManager,
            IconManager iconManager) {
        
        this.filterableSource = filterableSource;
        this.editorList = new BasicEventList<MatcherEditor<E>>();
        this.filterManager = new FilterManager<E>(filterableSource, iconManager);
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        setBorder(new SideLineBorder(borderColor, Side.RIGHT));
        setLayout(new MigLayout("insets 0 0 0 0, gap 0!, fill, hidemode 3", 
                "", ""));
        
        textFieldDecorator.decorateClearablePromptField(filterTextField, AccentType.NONE);
        filterTextField.setMinimumSize(new Dimension(filterWidth, filterTextField.getMinimumSize().height));
        filterTextField.setPreferredSize(new Dimension(filterWidth, filterTextField.getPreferredSize().height));
        
        filterDisplayPanel = new FilterDisplayPanel();
        
        filterPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!, hidemode 3", 
                "[grow]", ""));
        filterPanel.setOpaque(false);
        
        separator.setBackground(dividerBackgroundColor);
        separator.setForeground(dividerForegroundColor);
        
        filterScrollPane.setBorder(BorderFactory.createEmptyBorder());
        filterScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        filterScrollPane.setViewportView(filterPanel);
        
        // Add listener to update filter layout when scroll bar appears.
        filterScrollPane.getVerticalScrollBar().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                if (!layoutAdjusting) doFilterLayout();
            }
            
            @Override
            public void componentShown(ComponentEvent e) {
                if (!layoutAdjusting) doFilterLayout();
            }
        });
        
        propertyPanel = new PropertyFilterPanel();
        
        // Create category filter and display component.
        categoryFilter = filterManager.getCategoryFilter();
        JComponent categoryComp = categoryFilter.getComponent();
        categoryComp.setVisible(filterableSource.getFilterCategory() == SearchCategory.ALL);
        
        // Create source filter and display component.
        sourceFilter = filterManager.getSourceFilter();
        JComponent sourceComp = sourceFilter.getComponent();
        sourceComp.setVisible(friendManager.isSignedIn());
        
        // Layout components.
        add(filterTextField   , "gap 6 6 6 6, growx, wrap");
        add(filterDisplayPanel, "gap 0 0 2 0, growx, wrap");
        add(separator         , "gap 0 0 6 0, hmin 2, growx, wrap");
        add(filterScrollPane  , "gap 0 0 0 0, grow");
        
        doFilterLayout();
        
        configureFilters();
    }
    
    /**
     * Updates the layout of the filter components depending on the state of 
     * the vertical scroll bar. 
     */
    private void doFilterLayout() {
        layoutAdjusting = true;
        
        try {
            // Determine max width.
            JScrollBar scrollBar = filterScrollPane.getVerticalScrollBar();
            int maxWidth = filterWidth - (scrollBar.isVisible() ? scrollBar.getWidth() : 0);
            
            // Add components using max width.
            filterPanel.removeAll();
            filterPanel.add(categoryFilter.getComponent(), "gap 6 6 8 6, wmax " + maxWidth + ", growx, wrap");
            filterPanel.add(sourceFilter.getComponent()  , "gap 6 6 8 6, wmax " + maxWidth + ", growx, wrap");
            filterPanel.add(propertyPanel                , "gap 6 6 0 0, wmax " + maxWidth + ", grow");
            
            // Update separator visibility.
            updateSeparator();
            validate();
            repaint();
            
        } finally {
            layoutAdjusting = false;
        }
    }
    
    /**
     * Updates the visibility of the separator.
     */
    private void updateSeparator() {
        separator.setVisible(filterDisplayPanel.isVisible() || 
                filterScrollPane.getVerticalScrollBar().isVisible());
    }

    /**
     * Configures the filters by creating a composite filter that uses a list 
     * of MatcherEditor objects.
     */
    private void configureFilters() {
        // Create text filter with "live" filtering.
        MatcherEditor<E> editor =
            new TextComponentMatcherEditor<E>(
                filterTextField, new FilterableItemTextFilterator<E>(), true);
        
        // Add text filter to editor list. 
        editorList.add(editor);
        
        // Create CompositeMatcherEditor to combine filters.
        CompositeMatcherEditor<E> compositeEditor = new 
                CompositeMatcherEditor<E>(editorList);
        
        // Configure filter in data model.
        filterableSource.setFilterEditor(compositeEditor);
        
        // Hide filter display.
        filterDisplayPanel.setVisible(false);
        
        // Add listeners to standard filters.
        categoryFilter.addFilterListener(new AddFilterListener());
        sourceFilter.addFilterListener(new AddFilterListener());
    }

    /**
     * Adds the specified filter to the list of active filters.
     */
    private void addActiveFilter(Filter<E> filter) {
        // Add matcher/editor to list.
        MatcherEditor<E> editor = filter.getMatcherEditor();
        if ((editor != null) && !editorList.contains(editor)) {
            editorList.add(editor);
        }
        
        // Add filter to display.
        filterDisplayPanel.addFilter(filter);
        
        // Update display category.
        updateCategory();
    }

    /**
     * Removes the specified filter from the list of active filters.
     */
    private void removeActiveFilter(Filter<E> filter) {
        // Remove filter from display.
        filterDisplayPanel.removeFilter(filter);
        
        // Remove matcher/editor from list.
        MatcherEditor<E> editor = filter.getMatcherEditor();
        if ((editor != null) && editorList.contains(editor)) {
            editorList.remove(editor);
        }
        
        // Reset filter for use.
        filter.reset();
        
        // Update display category.
        updateCategory();
    }
    
    /**
     * Updates the display category.  This method updates the displayed 
     * property filters, and fires a <code>categorySelected</code> event to
     * update the display category.  (The event is used to update the column
     * layout in the table view.) 
     */
    private void updateCategory() {
        if (categoryFilter.isActive()) {
            // Get selected category.
            Category category = categoryFilter.getSelectedCategory();
            SearchCategory displayCategory = SearchCategory.forCategory(category);
            
            // Apply category to property filters and fire event.
            propertyPanel.setFilterCategory(displayCategory);
            fireCategorySelected(displayCategory);
            
        } else if (categoryFilter.getCategoryCount() == 1) {
            // Get only remaining category.
            Category category = categoryFilter.getDefaultCategory();
            SearchCategory displayCategory = SearchCategory.forCategory(category);
            
            // Apply category to property filters and fire event.
            propertyPanel.setFilterCategory(displayCategory);
            fireCategorySelected(displayCategory);
            
        } else {
            // No specific category so reapply defaults.
            propertyPanel.setFilterCategory(defaultFilterCategory);
            fireCategorySelected(defaultDisplayCategory);
        }
    }

    /**
     * Adds the specified listener to the list that is notified when the 
     * display category changes.
     */
    public void addCategoryListener(CategoryListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified listener from the list that is notified when the 
     * display category changes.
     */
    public void removeCategoryListener(CategoryListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies registered listeners when the specified category should be 
     * displayed.
     */
    private void fireCategorySelected(SearchCategory displayCategory) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).categorySelected(displayCategory);
        }
    }
    
    /**
     * Sets the default display category, and updates the available filters.
     */
    public void setSearchCategory(SearchCategory searchCategory) {
        // Save default display category.
        defaultDisplayCategory = searchCategory;
        
        if (searchCategory == SearchCategory.ALL) {
            // Start detector to determine default filter category based on 
            // actual list of filterable items.
            CategoryDetector detector = new CategoryDetector<E>(filterableSource, categoryFilter);
            detector.start(new CategoryDetector.CategoryDetectorListener() {
                @Override
                public void categoryFound(Category category) {
                    defaultFilterCategory = (category != null) ? 
                            SearchCategory.forCategory(category) : SearchCategory.ALL;
                    propertyPanel.setFilterCategory(defaultFilterCategory);
                }
            });
            
        } else {
            // Save default filter category and update filters.
            defaultFilterCategory = searchCategory;
            propertyPanel.setFilterCategory(searchCategory);
        }
    }

    @Override
    public void dispose() {
        filterManager.dispose();
    }
    
    /**
     * Listener to apply a filter when its state changes. 
     */
    private class AddFilterListener implements FilterListener<E> {

        @Override
        public void filterChanged(Filter<E> filter) {
            if (filter.isActive()) {
                addActiveFilter(filter);
            } else {
                removeActiveFilter(filter);
            }
        }
    }
    
    /**
     * Action to remove all active filters.
     */
    private class RemoveAllAction extends AbstractAction {

        public RemoveAllAction() {
            putValue(Action.NAME, I18n.tr("reset"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            Set<Filter<E>> filterSet = filterDisplayPanel.getActiveFilters();
            for (Filter<E> filter : filterSet) {
                removeActiveFilter(filter);
            }
        }
    }
    
    /**
     * Action to remove active filter.
     */
    private class RemoveFilterAction extends AbstractAction {
        private final Filter<E> filter;
        
        public RemoveFilterAction(Filter<E> filter) {
            this.filter = filter;
            putValue(Action.NAME, filter.getActiveText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            removeActiveFilter(filter);
        }
    }
    
    /**
     * Action to display more or less filters.
     */
    private class MoreFilterAction extends AbstractAction {
        private final String MORE = I18n.tr("more filters");
        private final String LESS = I18n.tr("less filters");

        public MoreFilterAction() {
            putValue(Action.NAME, MORE);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            propertyPanel.setShowAll(!propertyPanel.isShowAll());
            update(propertyPanel.isShowAll());
        }
        
        public void update(boolean showAll) {
            putValue(Action.NAME, (showAll ? LESS : MORE));
        }
    }

    /**
     * Panel that displays the active filters applied to the items.
     */
    private class FilterDisplayPanel extends JPanel {
        
        private final JPanel displayPanel = new JPanel();
        private final JButton resetButton = new JButton();
        
        private final Map<Filter<E>, ActiveFilterPanel> displayMap = 
            new HashMap<Filter<E>, ActiveFilterPanel>();
        
        public FilterDisplayPanel() {
            setLayout(new MigLayout("insets 0 0 0 0, gap 0!, hidemode 3", 
                    "[grow]", ""));
            setOpaque(false);
            
            displayPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                    "[grow]", ""));
            displayPanel.setOpaque(false);
            
            resetButton.setAction(new RemoveAllAction());
            resetButton.setBorder(BorderFactory.createEmptyBorder());
            resetButton.setContentAreaFilled(false);
            resetButton.setFocusPainted(false);
            resetButton.setFont(resetTextFont);
            resetButton.setForeground(resetTextColor);
            resetButton.addMouseListener(new RolloverCursorListener());
            
            add(displayPanel, "gap 6 6 0 0, growx, wrap");
            add(resetButton , "gap 6 6 0 0, alignx right, wrap");
        }
        
        /**
         * Adds the specified filter to the display.
         */
        public void addFilter(Filter<E> filter) {
            if (displayMap.get(filter) != null) {
                removeFilter(filter);
            }
            
            // Create filter display and save in map.
            ActiveFilterPanel activeFilterPanel = new ActiveFilterPanel(new RemoveFilterAction(filter));
            displayMap.put(filter, activeFilterPanel);
            
            // Add filter display to container.
            displayPanel.add(activeFilterPanel, "gaptop 4, wmax " + filterWidth + ", wrap");
            
            // Display reset button if multiple filters.
            resetButton.setVisible(displayMap.size() > 1);
            
            // Display this container.
            setVisible(true);
            updateSeparator();
            
            // Repaint filter display.
            AdvancedFilterPanel.this.validate();
            AdvancedFilterPanel.this.repaint();
        }
        
        /**
         * Removes the specified filter from the display.
         */
        public void removeFilter(Filter<E> filter) {
            // Remove filter display from container.
            ActiveFilterPanel activeFilterPanel = displayMap.get(filter);
            if (activeFilterPanel != null) {
                displayPanel.remove(activeFilterPanel);
            }
            
            // Remove filter display from map.
            displayMap.remove(filter);
            
            // Hide reset button if not multiple filters.
            resetButton.setVisible(displayMap.size() > 1);

            // Hide this container if no active filters.
            if (displayMap.size() < 1) {
                setVisible(false);
                updateSeparator();
            }
            
            // Repaint filter display.
            AdvancedFilterPanel.this.validate();
            AdvancedFilterPanel.this.repaint();
        }
        
        /**
         * Returns the set of filters currently in use.
         */
        public Set<Filter<E>> getActiveFilters() {
            return new CopyOnWriteArraySet<Filter<E>>(displayMap.keySet());
        }
    }
    
    /**
     * Panel that displays property filters associated with the current filter 
     * category. 
     */
    private class PropertyFilterPanel extends JPanel implements FilterListener<E> {
        
        private final JButton moreButton = new JButton();
        
        /** Action to toggle "show-all" filters state. */
        private final MoreFilterAction moreFilterAction = new MoreFilterAction();
        
        /** Map of "show-all" indicators by search category. */
        private final Map<SearchCategory, Boolean> showAllMap = 
            new EnumMap<SearchCategory, Boolean>(SearchCategory.class);
        
        /** List of available filters. */
        private List<Filter<E>> filterList = Collections.emptyList();
        
        private SearchCategory currentCategory;
        
        public PropertyFilterPanel() {
            setLayout(new MigLayout("insets 0 0 0 0, gap 0!, hidemode 3", 
                    "[grow]", ""));
            setOpaque(false);
            
            moreButton.setAction(moreFilterAction);
            moreButton.setBorder(BorderFactory.createEmptyBorder());
            moreButton.setContentAreaFilled(false);
            moreButton.setFocusPainted(false);
            moreButton.setFont(moreTextFont);
            moreButton.setForeground(moreTextColor);
            moreButton.addMouseListener(new RolloverCursorListener());
        }

        /**
         * Sets the specified filter category, and updates the visible filters.
         */
        public void setFilterCategory(SearchCategory filterCategory) {
            // Skip if category not changed.
            if (currentCategory == filterCategory) {
                return;
            }
            currentCategory = filterCategory;

            // Save old property filters.
            List<Filter<E>> oldFilterList = filterList;
            
            // Get new property filters for category.
            filterList = filterManager.getPropertyFilterList(filterCategory);
            int filterMin = filterManager.getPropertyFilterMinimum(filterCategory);
            
            // Remove old filters, and deactivate filters that are NOT in the
            // list of new filters.
            removeAll();
            for (Filter<E> filter : oldFilterList) {
                filter.removeFilterListener(this);
                if (!filterList.contains(filter)) {
                    removeActiveFilter(filter);
                }
            }
            
            // Add new filters to container, and set visibility for filters
            // that are not active.
            for (int i = 0, size = filterList.size(); i < size; i++) {
                Filter<E> filter = filterList.get(i);
                JComponent component = filter.getComponent();
                add(component, "gap 0 0 8 6, aligny top, growx, wrap");
                if (!filter.isActive()) {
                    component.setVisible(isFilterVisible(i));
                }
                filter.addFilterListener(this);
            }
            
            // Add more/less button if needed.
            if ((filterMin > 0) && (filterList.size() > filterMin)) {
                add(moreButton, "gap 0 0 8 3, aligny top");
            }
            
            // Update more/less button.
            moreFilterAction.update(isShowAll());
            
            // Validate layout and repaint container.
            validate();
            repaint();
        }
        
        /**
         * Sets an indicator to display all property filters for the current
         * category.  If <code>showAll</code> is false, then only the minimum 
         * number of filters is displayed.  
         */
        public void setShowAll(boolean showAll) {
            // Save indicator for category.
            showAllMap.put(currentCategory, showAll);
            
            // Set visibility for current filters.
            for (int i = 0, size = filterList.size(); i < size; i++) {
                Filter<E> filter = filterList.get(i);
                JComponent component = filter.getComponent();
                if (!filter.isActive()) {
                    component.setVisible(isFilterVisible(i));
                }
            }
        }
        
        /**
         * Returns an indicator that determines whether all property filters 
         * are displayed for the current category, or only the minimum number.
         */
        public boolean isShowAll() {
            Boolean showAll = showAllMap.get(currentCategory);
            return (showAll != null) ? showAll.booleanValue() : false;
        }
        
        /**
         * Returns an indicator that determines whether the filter at the 
         * specified index is visible.
         */
        private boolean isFilterVisible(int index) {
            // Get showAll indicator for category.
            boolean visible = isShowAll();
            
            // If not showAll, compare index with minimum filter count.
            if (!visible) {
                int filterMin = filterManager.getPropertyFilterMinimum(currentCategory);
                visible = (filterMin < 1) || (index < filterMin);
            }
            
            return visible;
        }

        @Override
        public void filterChanged(Filter<E> filter) {
            if (filter.isActive()) {
                addActiveFilter(filter);
            } else {
                removeActiveFilter(filter);
            }
        }
    }

    /**
     * Defines a listener to handle display category events.
     */
    public static interface CategoryListener {
        
        /**
         * Invoked when the specified display category is selected.
         */
        void categorySelected(SearchCategory displayCategory);
        
    }
}
