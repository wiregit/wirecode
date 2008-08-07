package org.limewire.ui.swing.search;

import ca.odell.glazedlists.swing.EventTableModel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is both a table cell renderer and a table cell editor
 * for displaying the "Download", "More Info" and "Mark as Junk" buttons.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionColumnTableCellEditor
extends AbstractCellEditor
implements TableCellEditor, TableCellRenderer {
        
    private static final String[] TOOLTIPS =
        { "Download", "More Info", "Mark as Junk" };
    
    private static final int MARK_AS_JUNK = 2;    
    
    private static final int HGAP = 10;    
    private static final int VGAP = 0;    
    
    private static Map<VisualSearchResult, Boolean> junkMap =
        new WeakHashMap<VisualSearchResult, Boolean>();
    
    // The icons displayed in the action column,
    // supplied by the call to GuiUtils.assignResources().
    @Resource private Icon downloadDownIcon;
    @Resource private Icon downloadOverIcon;
    @Resource private Icon downloadUpIcon;
    @Resource private Icon infoDownIcon;
    @Resource private Icon infoOverIcon;
    @Resource private Icon infoUpIcon;
    @Resource private Icon junkDownIcon;
    @Resource private Icon junkOverIcon;
    @Resource private Icon junkUpIcon;
    
    private Icon[][] icons;
    private JPanel panel;
    private JToggleButton junkButton = new JToggleButton();
    private VisualSearchResult vsr;
    private boolean internalSelect;
    private int height;
    
    public ActionColumnTableCellEditor() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
        
        icons = new Icon[][] {
            { downloadUpIcon, downloadOverIcon, downloadDownIcon },
            { infoUpIcon, infoOverIcon, infoDownIcon },
            { junkUpIcon, junkOverIcon, junkDownIcon }
        };
        
        height = 0;
        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            height = Math.max(height, upIcon.getIconHeight());
        }
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected,
        int row, int column) {
        
        Component component = getPanel(table);
        vsr = (VisualSearchResult) value;
        //System.out.println(
        //    "edit " + vsr.getDescription() + ' ' + isJunk(vsr));
        //junkButton.setIcon(junkUpIcon);
        internalSelect = true;
        //junkButton.setSelected(isJunk(vsr));
        junkButton.getModel().setPressed(isJunk(vsr));
        internalSelect = false;
        return component;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {
        
        Component component = getTableCellEditorComponent(
            table, value, isSelected, row, column);
        
        //vsr = (VisualSearchResult) value;
        //System.out.println(
        //    "render " + vsr.getDescription() + ' ' + isJunk(vsr));
        //junkButton.getModel().setPressed(isJunk(vsr));
        //junkButton.setIcon(isJunk(vsr) ? junkDownIcon : junkUpIcon);
        
        return component;
    }
    
    private JPanel getPanel(final JTable table) {
        if (panel != null) return panel;
        
        table.setRowHeight(height + 2*table.getRowMargin());
        
        panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
        
        int buttonIndex = 0;
        for (Icon[] iconSet : icons) {
            Icon upIcon = iconSet[0];
            Icon overIcon = iconSet[1];
            Icon downIcon = iconSet[2];
            
            AbstractButton button = buttonIndex == MARK_AS_JUNK ?
                junkButton : new JButton();
            button.setIcon(upIcon);
            button.setRolloverIcon(overIcon);
            button.setPressedIcon(downIcon);
            button.setSelectedIcon(downIcon);
            
            button.setToolTipText(TOOLTIPS[buttonIndex]);
            
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            
            Dimension size =
                new Dimension(upIcon.getIconWidth(), upIcon.getIconHeight());
            button.setPreferredSize(size);
            
            panel.add(button);
            
            ++buttonIndex;
        }
        
        junkButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (internalSelect) return;
                
                int row = table.getSelectedRow();
                int column = table.getSelectedColumn();
                
                EventTableModel<VisualSearchResult> model =
                    (EventTableModel<VisualSearchResult>) table.getModel();
                VisualSearchResult vsr = model.getElementAt(row);
                //System.out.println("Clicked junk button on row " +
                //    row + " \"" + vsr.getDescription() + '"');
                
                //System.out.println(
                //    "junkButton selected? " + junkButton.isSelected());
                
                boolean junk = event.getStateChange() == ItemEvent.SELECTED;
                //System.out.println("junk = " + junk);
                
                junkMap.put(vsr, junk);
                
                model.fireTableCellUpdated(row, column);
                
                fireEditingStopped(); // make renderer reappear
            }
        });
        
        return panel;
    }
    
    public boolean isCellEditable(EventObject event) {
        return true;
    }
    
    private boolean isJunk(VisualSearchResult vsr) {
        if (vsr == null) return false;
        
        Boolean junk = junkMap.get(vsr);
        if (junk == null) junk = false;
        //System.out.println(vsr.getDescription() + ' ' +
        //    (junk ? "is" : "isn't") + " junk");
        return junk;
    }
}
