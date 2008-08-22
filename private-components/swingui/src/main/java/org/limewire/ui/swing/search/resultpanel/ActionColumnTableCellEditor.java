package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.swing.EventTableModel;

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
    private boolean internalSelect;
    
    @Override
    public Object getCellEditorValue() {
        return null;
    }
    
    private ActionButtonPanel getPanel(final JTable table) {
        if (panel != null) return panel;
        
        panel = new ActionButtonPanel();
        
        table.setRowHeight(panel.getIconHeight() + 2*table.getRowMargin());
        
        JToggleButton junkButton = panel.getJunkButton();
        junkButton.addItemListener(new ItemListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (internalSelect) return;
                
                int row = table.getSelectedRow();
                int column = table.getSelectedColumn();
                
                EventTableModel<VisualSearchResult> model =
                    (EventTableModel<VisualSearchResult>) table.getModel();
                VisualSearchResult vsr = model.getElementAt(row);
                
                boolean junk = event.getStateChange() == ItemEvent.SELECTED;
                
                junkMap.put(vsr, junk);
                
                model.fireTableCellUpdated(row, column);
                
                fireEditingStopped(); // make renderer reappear
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
        internalSelect = true;
        JToggleButton junkButton = panel.getJunkButton();
        junkButton.getModel().setPressed(isJunk(vsr));
        internalSelect = false;
        return panel;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {
        
        Component component = getTableCellEditorComponent(
            table, value, isSelected, row, column);
        
        return component;
    }
    
    @Override
    public boolean isCellEditable(EventObject event) {
        return true;
    }
    
    private boolean isJunk(VisualSearchResult vsr) {
        if (vsr == null) return false;
        
        Boolean junk = junkMap.get(vsr);
        if (junk == null) junk = false;
        return junk;
    }
}
