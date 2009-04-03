package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.filter.Filter;
import org.limewire.ui.swing.search.filter.FilterFactory;
import org.limewire.ui.swing.search.filter.FilterListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualSearchResultTextFilterator;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
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
public class AdvancedFilterPanel extends JPanel {

    // TODO create resources
    private Color backgroundColor = Color.GRAY;
    private Font headerFont = new Font("Dialog", Font.BOLD, 12);
    
    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;

    /** List of editors being used for filtering. */
    private final EventList<MatcherEditor<VisualSearchResult>> editorList;

    /** Factory for creating filters. */
    private final FilterFactory filterFactory;
    
    private final HeaderBar headerBar;
    private final PromptTextField filterTextField = new PromptTextField(I18n.tr("Refine results..."));
    
    private final JLabel sourceLabel = new JLabel(I18n.tr("Results From:"));
    private final Filter sourceFilter;
    
    private final JLabel categoryLabel = new JLabel(I18n.tr("Show:"));
    private final CategoryFilterPanel categoryPanel;

    /**
     * Constructs a FilterPanel with the specified search results data model
     * and UI decorators.
     */
    @AssistedInject
    public AdvancedFilterPanel(
            @Assisted SearchResultsModel searchResultsModel,
            HeaderBarDecorator headerBarDecorator,
            TextFieldDecorator textFieldDecorator) {
        
        this.searchResultsModel = searchResultsModel;
        this.editorList = new BasicEventList<MatcherEditor<VisualSearchResult>>();
        this.filterFactory = new FilterFactory(searchResultsModel);
        
        setBackground(backgroundColor);
        setLayout(new MigLayout("insets 0 0 0 0, gap 0!", "[grow]", ""));
        
        headerBar = new HeaderBar();
        headerBarDecorator.decorateBasic(headerBar);
        headerBar.setPreferredSize(new Dimension(0, headerBar.getPreferredSize().height));
        
        textFieldDecorator.decorateClearablePromptField(filterTextField, AccentType.SHADOW);
        
        sourceLabel.setFont(headerFont);
        
        // Create source filter and display component.
        sourceFilter = filterFactory.getSourceFilter();
        JComponent sourceComp = sourceFilter.getComponent();
        
        categoryLabel.setFont(headerFont);
        categoryPanel = new CategoryFilterPanel();
        categoryPanel.setOpaque(false);
        
        add(headerBar      , "growx, wrap");
        add(filterTextField, "gap 3 3 6 12, growx, wrap");
        add(sourceLabel    , "gap 6 6 0 3, growx, wrap");
        add(sourceComp     , "gap 6 6 0 12, growx, wrap");
        add(categoryLabel  , "gap 6 6 0 3, growx, wrap");
        add(categoryPanel  , "gap 6 6, grow");
        
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
        
        // Add standard filters to editor list. 
        editorList.add(editor);
        editorList.add(sourceFilter.getMatcherEditor());
        
        // Create CompositeMatcherEditor to combine filters.
        CompositeMatcherEditor<VisualSearchResult> compositeEditor = new 
                CompositeMatcherEditor<VisualSearchResult>(editorList);
        
        // Configure filter in data model.
        searchResultsModel.setFilterEditor(compositeEditor);
    }

    /**
     * Sets the current search category, and updates the available filters.
     */
    public void setSearchCategory(SearchCategory searchCategory) {
        categoryPanel.setSearchCategory(searchCategory);
    }

    /**
     * Panel that displays property filters associated with the current search 
     * category. 
     */
    private class CategoryFilterPanel extends JPanel implements FilterListener {
        
        private FilterList<VisualSearchResult> categoryList;
        private Filter[] filters = new Filter[0];
        
        public CategoryFilterPanel() {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow]", ""));
        }

        public void setSearchCategory(SearchCategory searchCategory) {
            // Remove old filters.  We also remove the matcher/editor from the
            // editor list, and dispose of the filter.
            for (Filter filter : filters) {
                filter.removeFilterListener(this);
                remove(filter.getComponent());
                editorList.remove(filter.getMatcherEditor());
                filter.dispose();
            }
            
            // Dispose of old results list, and get new one.
            if (categoryList != null) categoryList.dispose();
            categoryList = filterFactory.getUnfilteredCategoryList(searchCategory);

            // Get filters for category.
            filters = filterFactory.getCategoryFilters(searchCategory, categoryList);
            
            // Add new filters.
            for (Filter filter : filters) {
                JComponent component = filter.getComponent();
                add(component, "aligny top, growx, gap 0 0 0 0, wrap 12");
                filter.addFilterListener(this);
            }
            
            // Validate layout and repaint container.
            validate();
            repaint();
        }

        @Override
        public void filterChanged(Filter filter) {
            MatcherEditor<VisualSearchResult> editor = filter.getMatcherEditor();
            if ((editor != null) && !editorList.contains(editor)) {
                editorList.add(editor);
            }
        }
    }
}
