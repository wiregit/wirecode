package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.nav.Navigator;
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
        
    private ActionButtonPanel panel;
    private final Navigator navigator;
    private VisualSearchResult vsr;
    
    public ActionColumnTableCellEditor(Navigator navigator) {
        this.navigator = navigator;
    }
    
    @Override
    public Object getCellEditorValue() {
        return null;
    }
    
    private ActionButtonPanel getPanel(final JTable table) {
        if (panel != null) return panel;
        
        panel = new ActionButtonPanel(navigator);
        panel.setOpaque(false);
        
        JToggleButton junkButton = panel.getSpamButton();
        junkButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                boolean spam = event.getStateChange() == ItemEvent.SELECTED;
                vsr.setSpam(spam);
                table.editingStopped(new ChangeEvent(table));
            }
        });
        
        return panel;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected,
        int row, int column) {
        
        ActionButtonPanel panel = getPanel(table);
        vsr = (VisualSearchResult) value;
        panel.setVisualSearchResult(vsr);
        panel.setRow(row);
        boolean spam = vsr.isSpam();
        panel.setAlpha(spam ? 0.2f : 1.0f);

        panel.getSpamButton().setSelected(spam);

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
}