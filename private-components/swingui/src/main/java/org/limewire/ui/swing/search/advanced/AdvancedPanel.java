package org.limewire.ui.swing.search.advanced;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.CollectionBackedComboBoxModel;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchInfo;

/** An abstract panel for creating advanced searches. */
abstract class AdvancedPanel extends JPanel {
    
    private final Map<FilePropertyKey, JComponent> componentMap =
        new EnumMap<FilePropertyKey, JComponent>(FilePropertyKey.class);
    private final SearchCategory category;

    /** Constructs an AdvancedPanel that will search the given category. */
    public AdvancedPanel(SearchCategory category) {
        super(new MigLayout("fill", "[]related[grow]", ""));
        this.category = category;
    }
        
    /** Adds a new JTextField that will search using the given FilePropertyKey. */ 
    protected void addField(String name, FilePropertyKey key) {
        add(new JLabel(name));
        JTextField textField = new JTextField();
        add(textField, "growx, wrap");
        componentMap.put(key, textField);
    }
    
    /**
     * Adds a new JComboBox that will search using the given FilePropertyKey.
     * The contents of the combo are all the possible values.
     */
    protected void addField(String name, FilePropertyKey key, List<String> possibleValues) {
        add(new JLabel(name));
        JComboBox comboBox = new JComboBox(new CollectionBackedComboBoxModel(possibleValues));
        add(comboBox, "growx, wrap");
        componentMap.put(key, comboBox);
    }

    /** Gets a SearchInfo describing this advanced search request. */
    public SearchInfo getSearchInfo() {
        Map<FilePropertyKey, String> searchData = new EnumMap<FilePropertyKey, String>(FilePropertyKey.class);
        for(Map.Entry<FilePropertyKey, JComponent> entry : componentMap.entrySet()) {
            String value = getData(entry.getValue());
            if(value != null && !value.equals("")) {
                searchData.put(entry.getKey(), value);
            }
        }
        if(searchData.size() > 0) {
            return DefaultSearchInfo.createAdvancedSearch(searchData, category);
        } else {
            return null;
        }
    }
    
    private String getData(JComponent component) {
        if(component instanceof JTextField) {
            return ((JTextField)component).getText();
        } else if(component instanceof JComboBox) {
            return ((JComboBox)component).getSelectedItem().toString();
        } else {
            throw new IllegalStateException("invalid component: " + component);
        }
    }
    

}
