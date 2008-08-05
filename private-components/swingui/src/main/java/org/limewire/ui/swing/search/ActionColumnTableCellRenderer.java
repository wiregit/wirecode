package org.limewire.ui.swing.search;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class ActionColumnTableCellRenderer extends JPanel
implements TableCellRenderer {
        
    private static final int HGAP = 10;    
    private static final int VGAP = 0;    
    
    // The icons displayed in the action column,
    // supplied by the call to GuiUtils.assignResources().
    @Resource private Icon downloadIcon;
    @Resource private Icon infoIcon;
    @Resource private Icon junkIcon;
    
    private int height;
    
    public ActionColumnTableCellRenderer() {
        // Cause the @Resource fields to be injected
        // using properties in AppFrame.properties.
        // The icon PNG file is in swingui/src/main/resources/
        // org/limewire/ui/swing/mainframe/resources/icons.
        GuiUtils.assignResources(this);
        
        Icon[] icons = { downloadIcon, infoIcon, junkIcon };
        
        setLayout(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP));
        
        height = 0;
        for (Icon icon : icons) {
            height = Math.max(height, icon.getIconHeight());
            
            JButton button = new JButton(icon);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            Dimension size =
                new Dimension(icon.getIconWidth(), icon.getIconHeight());
            button.setPreferredSize(size);
            add(button);
        }
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object obj, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        table.setRowHeight(height + 2*table.getRowMargin());
        return this;
    }
}
