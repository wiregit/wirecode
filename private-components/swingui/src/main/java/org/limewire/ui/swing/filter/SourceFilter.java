package org.limewire.ui.swing.filter;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * Filter component to select items according to their sources.
 */
class SourceFilter<E extends FilterableItem> extends AbstractFilter<E> {
    /** Source types for search results. */
    public enum SourceType {
        P2P(I18n.tr("P2P Network")), 
        FRIENDS(I18n.tr("All Friends"));
        
        private String displayName;
        
        SourceType(String displayName) {
            this.displayName = displayName;
        }
        
        public String toString() {
            return displayName;
        }
    }

    private final JPanel panel = new JPanel();
    private final JLabel label = new JLabel();
    private final JList list = new JList();
    
    private final EventList<SourceType> sourceList = new BasicEventList<SourceType>();
    
    private EventSelectionModel<SourceType> selectionModel;

    /**
     * Constructs a SourceFilter.
     */
    public SourceFilter() {
        FilterResources resources = getResources();
        
        // Set up visual components.
        panel.setLayout(new MigLayout("insets 0 0 0 0, gap 0!", 
                "[left,grow]", ""));
        panel.setOpaque(false);
        
        label.setFont(resources.getHeaderFont());
        label.setForeground(resources.getHeaderColor());
        label.setText(I18n.tr("From"));
        
        list.setCellRenderer(new SourceCellRenderer());
        list.setFont(resources.getRowFont());
        list.setForeground(resources.getRowColor());
        list.setOpaque(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add listener to show cursor on mouse over.
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getDefaultCursor());
            }
        });
        
        panel.add(label, "wrap");
        panel.add(list , "gap 6 6, grow");
        
        initialize();
    }
    
    /**
     * Initializes the filter.
     */
    private void initialize() {
        // Create list of sources.
        for (SourceType type : SourceType.values()) {
            sourceList.add(type);
        }
        
        // Create list and selection models.
        EventListModel<SourceType> listModel = new EventListModel<SourceType>(sourceList);
        selectionModel = new EventSelectionModel<SourceType>(sourceList);
        list.setModel(listModel);
        list.setSelectionModel(selectionModel);
        
        // Add selection listener to update filter.
        list.addListSelectionListener(new SelectionListener());
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        selectionModel.clearSelection();
        // Deactivate filter.
        deactivate();
    }
    
    @Override
    public void dispose() {
        // Do nothing.
    }
    
    /**
     * Activates the filter using the specified text description and matcher.
     * This method also hides the filter component.
     */
    protected void activate(String activeText, Matcher<E> matcher) {
        super.activate(activeText, matcher);
        getComponent().setVisible(false);
    }
    
    /**
     * Deactivates the filter by clearing the text description and matcher.
     * This method also displays the filter component.
     */
    protected void deactivate() {
        super.deactivate();
        getComponent().setVisible(true);
    }

    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            // Skip selection change if filter is active.
            if (isActive()) {
                return;
            }
            
            // Get list of selected values.
            EventList<SourceType> selectedList = selectionModel.getSelected();
            if (selectedList.size() > 0) {
                SourceType value = selectedList.get(0);
                // Create new matcher and activate.
                Matcher<E> newMatcher = new SourceMatcher<E>(value);
                activate(value.toString(), newMatcher);
                
            } else {
                // Deactivate to clear matcher.
                deactivate();
            }
            
            // Notify filter listeners.
            fireFilterChanged(SourceFilter.this);
        }
    }
    
    /**
     * Cell renderer for source values.
     */
    private class SourceCellRenderer extends DefaultListCellRenderer {
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component renderer = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            
            if (renderer instanceof JLabel) {
                // Set appearance.
                ((JLabel) renderer).setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 1));
                ((JLabel) renderer).setOpaque(false);
            }

            return renderer;
        }
    }
}
