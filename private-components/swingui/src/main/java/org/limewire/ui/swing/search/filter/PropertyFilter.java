package org.limewire.ui.swing.search.filter;

import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.Objects;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

/**
 * Filter component to select search results according to a collection of
 * property values.
 */
class PropertyFilter extends AbstractFilter {

    private final FilterType filterType;
    private final FilePropertyKey propertyKey;
    private final FilterMatcherEditor editor;
    private final AllListItem allListItem;
    
    private final JList list = new JList();
    private final JScrollPane scrollPane = new JScrollPane();
    
    private EventListModel<Object> listModel;
    private EventSelectionModel<Object> selectionModel;
    
    /**
     * Constructs a PropertyFilterComponent using the specified results list,
     * filter type, and property key.
     */
    public PropertyFilter(FilterList<VisualSearchResult> resultsList,
            FilterType filterType, FilePropertyKey propertyKey) {
        
        if ((filterType == FilterType.PROPERTY) && (propertyKey == null)) {
            throw new IllegalArgumentException("Property filter cannot use null key");
        }
        
        this.filterType = filterType;
        this.propertyKey = propertyKey;
        this.allListItem = new AllListItem(getAllText());

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(5);

        // Create list of unique property values.
        EventList<Object> uniqueList = createUniquePropertyList(resultsList);
        
        // Create composite list including "all values" item. 
        EventList<Object> compositeList = createCompositeList(uniqueList);
        
        // Create list and selection models.
        listModel = new EventListModel<Object>(compositeList);
        selectionModel = new EventSelectionModel<Object>(compositeList);
        list.setModel(listModel);
        list.setSelectionModel(selectionModel);
        
        // Create matcher editor for filtering.
        editor = new FilterMatcherEditor();
        
        // Add selection listener to update filter.
        selectionModel.addListSelectionListener(new SelectionListener());
        
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(list);
    }
    
    @Override
    public JComponent getComponent() {
        return scrollPane;
    }

    @Override
    public MatcherEditor<VisualSearchResult> getMatcherEditor() {
        return editor;
    }
    
    @Override
    public void dispose() {
        selectionModel.dispose();
        listModel.dispose();
        // TODO verify whether resultsList needs to be disposed
    }
    
    /**
     * Returns the text string describing all property values for the current
     * filter type and property key. 
     */
    private String getAllText() {
        switch (filterType) {
        case EXTENSION:
            return I18n.tr("All Extensions");
            
        case PROPERTY:
            switch (propertyKey) {
            case AUTHOR:
                return I18n.tr("All Artists");
            case ALBUM:
                return I18n.tr("All Albums");
            case GENRE:
                return I18n.tr("All Genres");
            default:
                return "All " + propertyKey.toString();
            }
            
        default:
            throw new IllegalStateException("Unknown filter type " + filterType);
        }
    }
    
    /**
     * Creates the list of property values including an "all values" item.
     */
    private CompositeList<Object> createCompositeList(EventList<Object> propertyList) {
        // Create list with "all values" item.  This uses the same publisher 
        // and lock as the original list.
        ListEventPublisher publisher = propertyList.getPublisher();
        ReadWriteLock readWriteLock = propertyList.getReadWriteLock();
        EventList<Object> allList = new BasicEventList<Object>(publisher, readWriteLock);
        allList.add(allListItem);
        
        // Combine "all values" item and properties into a single list.
        CompositeList<Object> compositeList = new CompositeList<Object>(publisher, readWriteLock);
        compositeList.addMemberList(allList);
        compositeList.addMemberList(propertyList);
        return compositeList;
    }
    
    /**
     * Returns a list of unique property values in the specified list of search 
     * results.
     */
    private UniqueList<Object> createUniquePropertyList(EventList<VisualSearchResult> resultsList) {
        switch (filterType) {
        case EXTENSION:
        case PROPERTY:
            // Create list of property values.
            FunctionList<VisualSearchResult, Object> functionList = 
                GlazedListsFactory.functionList(resultsList, 
                    new PropertyFunction(filterType, propertyKey));
            
            // Return list of unique values.
            return GlazedListsFactory.uniqueList(functionList,
                    new PropertyComparator(filterType, propertyKey));
            
        default:
            throw new IllegalArgumentException("Invalid filter type " + filterType);
        }
    }
    
    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            // Get list of selected values.
            EventList<Object> selectedList = selectionModel.getSelected();
            
            // Change matcher in editor.
            Matcher<VisualSearchResult> newMatcher = 
                new PropertyMatcher(filterType, propertyKey, allListItem, selectedList);
            editor.setMatcher(newMatcher);
            
            // Notify filter listeners.
            fireFilterChanged(PropertyFilter.this);
        }
    }

    /**
     * List item representing all property values.
     */
    private static class AllListItem {
        private final String name;
        
        public AllListItem(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * A Comparator for values in a specific property.
     */
    private static class PropertyComparator implements Comparator<Object> {
        private final FilterType filterType;
        private final FilePropertyKey propertyKey;

        public PropertyComparator(FilterType filterType, FilePropertyKey propertyKey) {
            this.filterType = filterType;
            this.propertyKey = propertyKey;
        }

        @Override
        public int compare(Object o1, Object o2) {
            if ((filterType == FilterType.PROPERTY) && FilePropertyKey.isLong(propertyKey)) {
                Long long1 = (Long) o1;
                Long long2 = (Long) o2;
                return Objects.compareToNull(long1, long2, false);
                
            } else {
                String s1 = (String) o1;
                String s2 = (String) o2;
                return Objects.compareToNullIgnoreCase(s1, s2, false);
            }
        }
    }
    
    /**
     * A function to transform a list of visual search results into a list of
     * specific property values.
     */
    private static class PropertyFunction implements Function<VisualSearchResult, Object> {
        private final FilterType filterType;
        private final FilePropertyKey propertyKey;

        public PropertyFunction(FilterType filterType, FilePropertyKey propertyKey) {
            this.filterType = filterType;
            this.propertyKey = propertyKey;
        }
        
        @Override
        public Object evaluate(VisualSearchResult vsr) {
            switch (filterType) {
            case EXTENSION:
                return vsr.getFileExtension().toLowerCase();
            case PROPERTY:
                return vsr.getProperty(propertyKey);
            default:
                return null;
            }
        }
    }
}
