package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class is both a table cell renderer and a table cell editor
 * for displaying the "Download", "More Info" and "Mark as Junk" buttons.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionColumnTableCellEditor
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {
        
    private static Map<VisualSearchResult, Boolean> junkMap =
        new WeakHashMap<VisualSearchResult, Boolean>();
    
    private ActionButtonPanel panel;
    private VisualSearchResult vsr;
    
    @Override
    public Object getCellEditorValue() {
        return null;
    }
    
    private ActionButtonPanel getPanel(final JTable table) {
        if (panel != null) return panel;
        
        panel = new ActionButtonPanel();
        
        JToggleButton junkButton = panel.getJunkButton();
        junkButton.addItemListener(new ItemListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void itemStateChanged(ItemEvent event) {
                boolean junk = event.getStateChange() == ItemEvent.SELECTED;
                junkMap.put(vsr, junk);
            }
        });
        
        return panel;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected,
        int row, int column) {
        
        panel = getPanel(table);
        vsr = (VisualSearchResult) value;

        // If the VisualSearchResult for the current row is currently
        // marked as junk then display the junkButton as pressed.
        JToggleButton junkButton = panel.getJunkButton();
        boolean junk = isJunk(vsr);
        junkButton.setSelected(junk);

        return panel;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {
        
        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }
    
    @Override
    public boolean isCellEditable(EventObject event) {
        return true;
    }
    
    private boolean isJunk(VisualSearchResult vsr) {
        Boolean junk = junkMap.get(vsr);
        if (junk == null) junk = false;
        return junk;
    }
}