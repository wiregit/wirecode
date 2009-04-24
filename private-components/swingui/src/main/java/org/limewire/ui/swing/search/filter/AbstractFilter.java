package org.limewire.ui.swing.search.filter;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Base implementation of Filter that provides listener support.
 */
abstract class AbstractFilter implements Filter {

    /** List of listeners notified when filter is changed. */
    private final List<FilterListener> listenerList = new ArrayList<FilterListener>();
    
    /** Matcher/editor used to filter search results. */
    private final FilterMatcherEditor editor;
    
    /** Resources for filters. */
    private final FilterResources resources = new FilterResources();
    
    /** Indicator that determines whether the filter is active. */
    private boolean active;
    
    /** Description for active filter. */
    private String activeText;
    
    /**
     * Constructs an AbstractFilter.
     */
    public AbstractFilter() {
        // Create matcher editor for filtering.
        editor = new FilterMatcherEditor();
    }
    
    /**
     * Adds the specified listener to the list that is notified when the 
     * filter changes.
     */
    @Override
    public void addFilterListener(FilterListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes the specified listener from the list that is notified when the 
     * filter changes.
     */
    @Override
    public void removeFilterListener(FilterListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies all registered listeners that the filter has changed for the
     * specified filter component.
     */
    protected void fireFilterChanged(Filter filter) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).filterChanged(filter);
        }
    }
    
    /**
     * Returns an indicator that determines whether the filter is in use.
     */
    @Override
    public boolean isActive() {
        return active;
    }
    
    /**
     * Returns the display text for an active filter.
     */
    @Override
    public String getActiveText() {
        return activeText;
    }
    
    /**
     * Returns the matcher/editor used to filter search results.
     */
    @Override
    public MatcherEditor<VisualSearchResult> getMatcherEditor() {
        return editor;
    }

    /**
     * Resets the filter.  The default implementation calls 
     * <code>deactivate()</code>.  Subclasses may override this method to 
     * update the Swing component when the filter is reset.
     */
    @Override
    public void reset() {
        deactivate();
    }
    
    /**
     * Activates the filter using the specified text description and matcher.
     */
    protected void activate(String activeText, Matcher<VisualSearchResult> matcher) {
        this.activeText = activeText;
        editor.setMatcher(matcher);
        active = true;
    }

    /**
     * Deactivates the filter by clearing the text description and matcher.
     */
    protected void deactivate() {
        editor.setMatcher(null);
        activeText = null;
        active = false;
    }
    
    /**
     * Returns the text color for the filter header.
     */
    protected Color getHeaderColor() {
        return resources.headerColor;
    }
    
    /**
     * Returns the text font for the filter header.
     */
    protected Font getHeaderFont() {
        return resources.headerFont;
    }
    
    /**
     * Returns the icon for the "more" button.
     */
    protected Icon getMoreIcon() {
        return resources.moreIcon;
    }
    
    /**
     * Returns the header background for the filter popup.
     */
    protected Color getPopupHeaderBackground() {
        return resources.popupHeaderBackground;
    }
    
    /**
     * Returns the header foreground for the filter popup.
     */
    protected Color getPopupHeaderForeground() {
        return resources.popupHeaderForeground;
    }
    
    /**
     * Returns the text color for filter rows.
     */
    protected Color getRowColor() {
        return resources.rowColor;
    }
    
    /**
     * Returns the text font for filter rows.
     */
    protected Font getRowFont() {
        return resources.rowFont;
    }
    
    /**
     * Resource container for filters.
     */
    private static class FilterResources {
        @Resource(key="AdvancedFilter.headerColor")
        Color headerColor;
        @Resource(key="AdvancedFilter.headerFont")
        Font headerFont;
        @Resource(key="AdvancedFilter.rowColor")
        Color rowColor;
        @Resource(key="AdvancedFilter.rowFont")
        Font rowFont;
        @Resource(key="AdvancedFilter.popupHeaderBackground")
        Color popupHeaderBackground;
        @Resource(key="AdvancedFilter.popupHeaderForeground")
        Color popupHeaderForeground;
        @Resource(key="AdvancedFilter.moreIcon")
        Icon moreIcon;
        
        FilterResources() {
            GuiUtils.assignResources(this);
        }
    }
}
