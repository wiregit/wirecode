package org.limewire.ui.swing.search.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.Objects;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * Filter component to select search results according to a collection of
 * property values.
 */
class PropertyFilter extends AbstractFilter {

    private final FilterType filterType;
    private final FilePropertyKey propertyKey;
    private final IconManager iconManager;
    
    private final JPanel panel = new JPanel();
    private final JLabel propertyLabel = new JLabel();
    private final JList list = new JList();
    private final HyperlinkButton moreButton = new HyperlinkButton();
    
    private FunctionList<VisualSearchResult, Object> propertyList;
    private UniqueList<Object> uniqueList;
    private EventSelectionModel<Object> selectionModel;
    private EventSelectionModel<Object> popupSelectionModel;
    private JPopupMenu morePopup;
    
    /**
     * Constructs a PropertyFilterComponent using the specified results list,
     * filter type, property key, and icon manager.
     */
    public PropertyFilter(EventList<VisualSearchResult> resultsList,
            FilterType filterType, FilePropertyKey propertyKey, 
            IconManager iconManager) {
        
        if ((filterType == FilterType.PROPERTY) && (propertyKey == null)) {
            throw new IllegalArgumentException("Property filter cannot use null key");
        }
        
        this.filterType = filterType;
        this.propertyKey = propertyKey;
        this.iconManager = iconManager;
        
        panel.setLayout(new MigLayout("insets 6 0 6 0, gap 0!, hidemode 2", 
                "[left,grow]", ""));
        panel.setOpaque(false);
        
        propertyLabel.setFont(getHeaderFont());
        propertyLabel.setForeground(getHeaderColor());
        propertyLabel.setText(getPropertyText());

        list.setCellRenderer(new PropertyCellRenderer());
        list.setFont(getRowFont());
        list.setForeground(getRowColor());
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
        
        moreButton.setAction(new MoreAction());
        moreButton.setFont(getRowFont());
        
        // Apply results list to filter.
        initialize(resultsList);
        
        // Calculate max list height.
        list.setPrototypeCellValue("Type");
        int listHeight = 3 * list.getFixedCellHeight();
        
        panel.add(propertyLabel, "wrap");
        panel.add(list         , "wmax 144, hmax " + listHeight + ", grow, wrap");
        panel.add(moreButton   , "");
    }
    
    /**
     * Initializes the filter using the specified list of search results.
     */
    private void initialize(EventList<VisualSearchResult> resultsList) {
        // Create list of unique property values.
        propertyList = createPropertyList(resultsList);
        uniqueList = createUniqueList(propertyList);
        
        // Initialize "more" button.
        moreButton.setVisible(uniqueList.size() > 3);

        // Add listener to display "more" button when needed.
        uniqueList.addListEventListener(new ListEventListener<Object>() {
            @Override
            public void listChanged(ListEvent listChanges) {
                if (!moreButton.isVisible() && (uniqueList.size() > 3)) {
                    moreButton.setVisible(true);
                } else if (moreButton.isVisible() && (uniqueList.size() < 4)) {
                    moreButton.setVisible(false);
                }
            }
        });
        
        // Create sorted list to display most popular values.
        EventList<Object> sortedList = GlazedListsFactory.sortedList(uniqueList, new PropertyCountComparator());
        
        // Create list and selection models.
        EventListModel<Object> listModel = new EventListModel<Object>(sortedList);
        selectionModel = new EventSelectionModel<Object>(sortedList);
        list.setModel(listModel);
        list.setSelectionModel(selectionModel);
        
        // Add selection listener to update filter.
        selectionModel.addListSelectionListener(new SelectionListener(selectionModel));
    }
    
    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void reset() {
        // Clear selections.
        if (selectionModel != null) {
            selectionModel.clearSelection();
        }
        if (popupSelectionModel != null) {
            popupSelectionModel.clearSelection();
        }
        // Deactivate filter.
        deactivate();
    }
    
    @Override
    public void dispose() {
        // Dispose of property list.  Since all other lists are based on the
        // property list, these should be freed for GC also.
        propertyList.dispose();
    }
    
    /**
     * Activates the filter using the specified text description and matcher.
     * This method also hides the filter component.
     */
    protected void activate(String activeText, Matcher<VisualSearchResult> matcher) {
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
     * Returns the text string describing the property for the current filter 
     * type and property key. 
     */
    private String getPropertyText() {
        switch (filterType) {
        case EXTENSION:
            return I18n.tr("Extensions");
            
        case PROPERTY:
            switch (propertyKey) {
            case AUTHOR:
                return I18n.tr("Artists");
            case ALBUM:
                return I18n.tr("Albums");
            case GENRE:
                return I18n.tr("Genres");
            default:
                return propertyKey.toString();
            }
            
        case TYPE:
            return I18n.tr("Types");
            
        default:
            throw new IllegalStateException("Unknown filter type " + filterType);
        }
    }
    
    /**
     * Returns a list of property values in the specified list of search
     * results.
     */
    private FunctionList<VisualSearchResult, Object> createPropertyList(
            EventList<VisualSearchResult> resultsList) {
        switch (filterType) {
        case EXTENSION:
        case PROPERTY:
        case TYPE:
            // Create list of property values.
            return GlazedListsFactory.functionList(resultsList, 
                    new PropertyFunction(filterType, propertyKey));
            
        default:
            throw new IllegalArgumentException("Invalid filter type " + filterType);
        }
    }

    /**
     * Returns a list of unique, non-null values in the specified list of
     * property values.
     */
    private UniqueList<Object> createUniqueList(EventList<Object> propertyList) {
        // Create list of non-null values.
        FilterList<Object> nonNullList = GlazedListsFactory.filterList(propertyList, 
            new Matcher<Object>() {
                @Override
                public boolean matches(Object item) {
                    return (item != null);
                }
            }
        );
        
        // Create list of unique values.
        return GlazedListsFactory.uniqueList(nonNullList, new PropertyComparator(filterType, propertyKey));
    }

    /**
     * Returns a new instance of the popup that displays all property values.
     */
    private JPopupMenu createMorePopup() {
        // Create popup containing display panel.
        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        popup.add(new MorePopupPanel());
        return popup;
    }
    
    /**
     * Displays the "more" popup that displays all property values.
     */
    private void showMorePopup() {
        if (morePopup == null) {
            morePopup = createMorePopup();
        }
        // Limit popup width.
        if (morePopup.getPreferredSize().width > 240) {
            morePopup.setPreferredSize(new Dimension(240, morePopup.getPreferredSize().height));
        }
        // Display popup next to property label.
        morePopup.show(moreButton, list.getWidth(), -moreButton.getY() + 3);
    }
    
    /**
     * Hides the "more" popup that displays all property values.
     */
    private void hideMorePopup() {
        if (morePopup != null) {
            morePopup.setVisible(false);
        }
    }
    
    /**
     * Action to display list of all property values.
     */
    private class MoreAction extends AbstractAction {

        public MoreAction() {
            super(I18n.tr("more"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            showMorePopup();
        }
    }
    
    /**
     * Display panel for "more" popup component. 
     */
    private class MorePopupPanel extends JPanel {
        
        private final JLabel titleLabel = new JLabel();
        private final JList moreList = new JList();
        private final JScrollPane scrollPane = new JScrollPane();
        
        public MorePopupPanel() {
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setLayout(new BorderLayout());
            
            titleLabel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
            titleLabel.setBackground(getPopupHeaderBackground());
            titleLabel.setForeground(getPopupHeaderForeground());
            titleLabel.setOpaque(true);
            titleLabel.setText(I18n.tr("All {0}", getPropertyText()));
            
            moreList.setCellRenderer(new PropertyCellRenderer());
            moreList.setFont(getRowFont());
            moreList.setForeground(getRowColor());
            moreList.setOpaque(false);
            moreList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            // Add listener to show cursor on mouse over.
            moreList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    e.getComponent().setCursor(Cursor.getDefaultCursor());
                }
            });
            
            // Set list and selection models.  We use the unique list directly
            // to display values alphabetically.
            EventListModel<Object> listModel = new EventListModel<Object>(uniqueList);
            popupSelectionModel = new EventSelectionModel<Object>(uniqueList);
            moreList.setModel(listModel);
            moreList.setSelectionModel(popupSelectionModel);
            
            // Add selection listener to update filter.
            popupSelectionModel.addListSelectionListener(new SelectionListener(popupSelectionModel));
            
            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setViewportView(moreList);
            
            add(titleLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }
    }
    
    /**
     * Listener to handle selection changes to update the matcher editor.  
     */
    private class SelectionListener implements ListSelectionListener {
        private final EventSelectionModel<Object> selectionModel;

        public SelectionListener(EventSelectionModel<Object> selectionModel) {
            this.selectionModel = selectionModel;
        }
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            // Skip selection change if filter is active.
            if (isActive()) {
                return;
            }
            
            // Get list of selected values.
            EventList<Object> selectedList = selectionModel.getSelected();
            if (selectedList.size() > 0) {
                // Create new matcher and activate.
                Matcher<VisualSearchResult> newMatcher = 
                    new PropertyMatcher(filterType, propertyKey, iconManager, selectedList);
                activate(selectedList.get(0).toString(), newMatcher);
                
            } else {
                // Deactivate to clear matcher.
                deactivate();
            }
            
            // Hide popup if showing.
            hideMorePopup();
            
            // Notify filter listeners.
            fireFilterChanged(PropertyFilter.this);
        }
    }
    
    /**
     * Cell renderer for property values.
     */
    private class PropertyCellRenderer extends DefaultListCellRenderer {
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Component renderer = super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            
            if ((renderer instanceof JLabel) && (value != null)) {
                // Get count for property value.
                int count = uniqueList.getCount(value);
                
                // Display count in cell.
                StringBuilder buf = new StringBuilder();
                buf.append(value.toString()).append(" (").append(count).append(")");
                ((JLabel) renderer).setText(buf.toString());
                
                // Set appearance.
                ((JLabel) renderer).setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 1));
                ((JLabel) renderer).setOpaque(false);
            }
            
            return renderer;
        }
    }
    
    /**
     * Comparator to sort property values by their result count.
     */
    private class PropertyCountComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            int count1 = uniqueList.getCount(o1);
            int count2 = uniqueList.getCount(o2);
            // Return inverse value to sort in descending order.
            return (count1 < count2) ? 1 : ((count1 > count2) ? -1 : 0);
        }
    }
    
    /**
     * A Comparator for values in a specific property.  This is used to create
     * a list of unique property values.
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
    private class PropertyFunction implements Function<VisualSearchResult, Object> {
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
            case TYPE:
                return iconManager.getMIMEDescription(vsr.getFileExtension());
            default:
                return null;
            }
        }
    }
}
