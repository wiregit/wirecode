package org.limewire.ui.swing.search.filter;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * Filter component to select search results according to their sources.
 */
class SourceFilter extends AbstractFilter {
    /** Source types for search results. */
    public enum SourceType {
        ALL(I18n.tr("Everyone")),
        P2P(I18n.tr("P2P Network")), 
        FRIENDS(I18n.tr("Friends"));
        
        private String displayName;
        
        SourceType(String displayName) {
            this.displayName = displayName;
        }
        
        public String toString() {
            return displayName;
        }
    }

    private final JList list = new JList();
    
    private final FilterMatcherEditor editor;
    
    private final EventList<Object> sourceList;

    /**
     * Constructs a SourceFilterComponent.
     */
    public SourceFilter() {
        // Set up visual components.
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(3);
        
        // Create list of sources.
        sourceList = new BasicEventList<Object>();
        for (SourceType type : SourceType.values()) {
            sourceList.add(type);
        }
        
        // Create list and selection models.
        EventListModel<Object> listModel = new EventListModel<Object>(sourceList);
        final EventSelectionModel<Object> selectionModel = new EventSelectionModel<Object>(sourceList);
        list.setModel(listModel);
        list.setSelectionModel(selectionModel);
        
        // Create matcher editor for filtering.
        editor = new FilterMatcherEditor();
        
        // Add selection listener to update filter.
        list.addListSelectionListener(new SelectionListener());
    }

    @Override
    public JComponent getComponent() {
        return list;
    }

    @Override
    public MatcherEditor<VisualSearchResult> getMatcherEditor() {
        return editor;
    }
    
    @Override
    public void dispose() {
        // Do nothing.
    }

    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            Object value = list.getSelectedValue();
            if (value instanceof SourceType) {
                // Change matcher in editor.
                Matcher<VisualSearchResult> newMatcher = new SourceMatcher((SourceType) value);
                editor.setMatcher(newMatcher);
            }
        }
    }
}
