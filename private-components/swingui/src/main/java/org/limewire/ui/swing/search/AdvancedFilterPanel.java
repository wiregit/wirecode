package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.filter.CategoryDetector;
import org.limewire.ui.swing.search.filter.CategoryFilter;
import org.limewire.ui.swing.search.filter.Filter;
import org.limewire.ui.swing.search.filter.FilterFactory;
import org.limewire.ui.swing.search.filter.FilterListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualSearchResultTextFilterator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Filter panel for search results.  AdvancedFilterPanel presents advanced 
 * filtering options, including an input text field and category-specific 
 * property filters. 
 */
public class AdvancedFilterPanel extends JPanel implements Disposable {

    // TODO create resources
    private Color backgroundColor = new Color(216, 216, 216);
    
    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;

    /** List of editors being used for filtering. */
    private final EventList<MatcherEditor<VisualSearchResult>> editorList;

    /** Factory for creating filters. */
    private final FilterFactory filterFactory;

    /** Factory for creating filters. */
    private final List<CategoryListener> listenerList = new ArrayList<CategoryListener>();

    /** Filter for file category. */
    private final CategoryFilter categoryFilter;
    
    /** Filter for file source. */
    private final Filter sourceFilter;
    
    private final PromptTextField filterTextField = new PromptTextField(I18n.tr("Refine results..."));
    
    private final FilterDisplayPanel filterDisplayPanel = new FilterDisplayPanel();
    
    private final PropertyFilterPanel propertyPanel = new PropertyFilterPanel();
    
    private SearchCategory defaultSearchCategory;
    private SearchCategory defaultFilterCategory;

    /**
     * Constructs a FilterPanel with the specified search results data model
     * and UI decorators.
     */
    @AssistedInject
    public AdvancedFilterPanel(
            @Assisted SearchResultsModel searchResultsModel,
            TextFieldDecorator textFieldDecorator,
            FriendActions friendManager,
            IconManager iconManager) {
        
        this.searchResultsModel = searchResultsModel;
        this.editorList = new BasicEventList<MatcherEditor<VisualSearchResult>>();
        this.filterFactory = new FilterFactory(searchResultsModel, iconManager);
        
        setBackground(backgroundColor);
        setLayout(new MigLayout("insets 0 0 0 0, gap 0!, hidemode 2", 
                "[grow]", ""));
        
        textFieldDecorator.decorateClearablePromptField(filterTextField, AccentType.SHADOW);
        
        // Create category filter and display component.
        categoryFilter = filterFactory.getCategoryFilter();
        JComponent categoryComp = categoryFilter.getComponent();
        
        // Create source filter and display component.
        sourceFilter = filterFactory.getSourceFilter();
        JComponent sourceComp = sourceFilter.getComponent();
        sourceComp.setVisible(friendManager.isSignedIn());
        
        add(filterTextField   , "gap 3 3 6 6, growx, wrap");
        add(filterDisplayPanel, "gap 0 0 0 0, growx, wrap");
        add(categoryComp      , "gap 6 6 0 0, growx, wrap");
        add(sourceComp        , "gap 6 6 0 0, growx, wrap");
        add(new JSeparator()  , "gap 6 6 6 0, growx, wrap");
        add(propertyPanel     , "gap 6 6 0 0, grow");
        
        configureFilters();
    }

    /**
     * Configures the search results filters by creating a composite filter
     * that uses a list of MatcherEditor objects.
     */
    private void configureFilters() {
        // Create text filter with "live" filtering.
        MatcherEditor<VisualSearchResult> editor =
            new TextComponentMatcherEditor<VisualSearchResult>(
                filterTextField, new VisualSearchResultTextFilterator(), true);
        
        // Add text filter to editor list. 
        editorList.add(editor);
        
        // Create CompositeMatcherEditor to combine filters.
        CompositeMatcherEditor<VisualSearchResult> compositeEditor = new 
                CompositeMatcherEditor<VisualSearchResult>(editorList);
        
        // Configure filter in data model.
        searchResultsModel.setFilterEditor(compositeEditor);
        
        // Hide filter display.
        filterDisplayPanel.setVisible(false);
        
        // Add listeners to standard filters.
        categoryFilter.addFilterListener(new AddFilterListener());
        sourceFilter.addFilterListener(new AddFilterListener());
    }

    /**
     * Adds the specified filter to the list of active filters.
     */
    private void addActiveFilter(Filter filter) {
        // Add matcher/editor to list.
        MatcherEditor<VisualSearchResult> editor = filter.getMatcherEditor();
        if ((editor != null) && !editorList.contains(editor)) {
            editorList.add(editor);
        }
        
        // Add filter to display.
        filterDisplayPanel.addFilter(filter);
        
        // Update display category.
        updateCategory(filter);
    }

    /**
     * Removes the specified filter from the list of active filters.
     */
    private void removeActiveFilter(Filter filter) {
        // Remove filter from display.
        filterDisplayPanel.removeFilter(filter);
        
        // Remove matcher/editor from list.
        MatcherEditor<VisualSearchResult> editor = filter.getMatcherEditor();
        if ((editor != null) && editorList.contains(editor)) {
            editorList.remove(editor);
        }
        
        // Reset filter for use.
        filter.reset();
        
        // Update display category.
        updateCategory(filter);
    }
    
    /**
     * Updates the display category using the specified filter.  If the filter
     * is a CategoryFilter, then the property filters are updated and a
     * categorySelected event is fired.  (The event is used to update the 
     * column layout in the table view.) 
     */
    private void updateCategory(Filter filter) {
        if (filter instanceof CategoryFilter) {
            // Get selected category.
            Category category = ((CategoryFilter) filter).getSelectedCategory();
            if (category != null) {
                // Category in use so apply to property filters and fire event.
                SearchCategory searchCategory = SearchCategory.forCategory(category);
                propertyPanel.setFilterCategory(searchCategory);
                fireCategorySelected(searchCategory);
            } else {
                // Category removed so reset in property filters and fire event.
                propertyPanel.setFilterCategory(defaultFilterCategory);
                fireCategorySelected(defaultSearchCategory);
            } 
        }
    }

    /**
     * Adds the specified listener to the list that is notified when the 
     * selected category changes.
     */
    public void addCategoryListener(CategoryListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified listener from the list that is notified when the 
     * selected category changes.
     */
    public void removeCategoryListener(CategoryListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies registered listeners when the specified category is selected.
     */
    private void fireCategorySelected(SearchCategory searchCategory) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).categorySelected(searchCategory);
        }
    }
    
    /**
     * Sets the default search category, and updates the available filters.
     */
    public void setSearchCategory(SearchCategory searchCategory) {
        // Save default search category.
        defaultSearchCategory = searchCategory;
        
        if (searchCategory == SearchCategory.ALL) {
            // Start detector to determine default filter category based on 
            // actual search results.
            CategoryDetector detector = new CategoryDetector(searchResultsModel, categoryFilter);
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
        filterFactory.dispose();
    }
    
    /**
     * Listener to apply a filter when its state changes. 
     */
    private class AddFilterListener implements FilterListener {

        @Override
        public void filterChanged(Filter filter) {
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
            for (Filter filter : filterDisplayPanel.getActiveFilters()) {
                removeActiveFilter(filter);
            }
        }
    }
    
    /**
     * Action to remove active filter.
     */
    private class RemoveFilterAction extends AbstractAction {

        private final Filter filter;
        
        public RemoveFilterAction(Filter filter) {
            this.filter = filter;
            putValue(Action.NAME, filter.getActiveText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            removeActiveFilter(filter);
        }
    }

    /**
     * Panel that displays the active filters applied to the search results.
     */
    private class FilterDisplayPanel extends JPanel {
        
        private final JLabel displayLabel = new JLabel(I18n.tr("Showing only:"));
        private final JPanel displayPanel = new JPanel();
        private final HyperlinkButton resetButton = new HyperlinkButton();
        
        private final Map<Filter, JComponent> displayMap = new HashMap<Filter, JComponent>();
        
        public FilterDisplayPanel() {
            setLayout(new MigLayout("insets 0 6 0 6, gap 0!", "[grow]", ""));
            
            displayPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", "[grow]", ""));
            
            resetButton.setAction(new RemoveAllAction());
            
            add(displayLabel, "growx, wrap");
            add(displayPanel, "growx, wrap");
            add(resetButton , "alignx right");
        }
        
        /**
         * Adds the specified filter to the display.
         */
        public void addFilter(Filter filter) {
            if (displayMap.get(filter) != null) {
                removeFilter(filter);
            }
            
            // Create filter display and save in map.
            JComponent activeFilterPanel = new ActiveFilterPanel(new RemoveFilterAction(filter));
            displayMap.put(filter, activeFilterPanel);
            
            // Add filter display to container.
            displayPanel.add(activeFilterPanel, "gap 6 6, wmax 132, growx, wrap");
            
            // Display this container.
            setVisible(true);
            validate();
            repaint();
        }
        
        /**
         * Removes the specified filter from the display.
         */
        public void removeFilter(Filter filter) {
            // Remove filter display from container.
            JComponent activeFilterPanel = displayMap.get(filter);
            if (activeFilterPanel != null) {
                displayPanel.remove(activeFilterPanel);
            }
            
            // Remove filter display from map.
            displayMap.remove(filter);

            // Hide this container if no active filters.
            if (displayMap.size() < 1) {
                setVisible(false);
            }
            validate();
            repaint();
        }
        
        public Filter[] getActiveFilters() {
            Set<Filter> filterSet = displayMap.keySet();
            return filterSet.toArray(new Filter[filterSet.size()]);
        }
    }
    
    /**
     * Panel that displays property filters associated with the current filter 
     * category. 
     */
    private class PropertyFilterPanel extends JPanel implements FilterListener {
        
        private Filter[] filters = new Filter[0];
        private SearchCategory currentCategory;
        
        public PropertyFilterPanel() {
            setLayout(new MigLayout("insets 0 0 0 0, gap 0!, hidemode 2", 
                    "[grow]", ""));
            setOpaque(false);
        }

        public void setFilterCategory(SearchCategory filterCategory) {
            // Skip if category not changed.
            if (currentCategory == filterCategory) {
                return;
            }
            currentCategory = filterCategory;

            // Save old property filters.
            Filter[] oldFilters = filters;
            
            // Get new property filters for category.
            filters = filterFactory.getPropertyFilters(filterCategory);
            List<Filter> newFilterList = Arrays.asList(filters);
            
            // Remove old filters, and deactivate if not in new list.
            for (Filter filter : oldFilters) {
                filter.removeFilterListener(this);
                remove(filter.getComponent());
                if (!newFilterList.contains(filter)) {
                    removeActiveFilter(filter);
                }
            }
            
            // Add new filters to container.
            for (Filter filter : filters) {
                JComponent component = filter.getComponent();
                add(component, "gaptop 6, aligny top, growx, wrap");
                filter.addFilterListener(this);
            }
            
            // Validate layout and repaint container.
            validate();
            repaint();
        }

        @Override
        public void filterChanged(Filter filter) {
            if (filter.isActive()) {
                addActiveFilter(filter);
            } else {
                removeActiveFilter(filter);
            }
        }
    }

    /**
     * Defines a listener for category change events.
     */
    public static interface CategoryListener {
        
        /**
         * Invoked when the specified search category is selected.
         */
        void categorySelected(SearchCategory searchCategory);
        
    }
}
