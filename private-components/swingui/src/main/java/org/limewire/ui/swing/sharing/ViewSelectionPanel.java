package org.limewire.ui.swing.sharing;

import java.awt.Insets;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Shows two toggle buttons. Designed to toggle between two different views.
 * Each toggle button should have an ItemListener which gets passed in on construction time.
 */
public class ViewSelectionPanel extends JPanel {

    public static String LIST_SELECTED = "LIST";
    public static String TABLE_SELECTED = "TABLE";
    public static String DISABLED = "DISABLED";
    
    @Resource private Icon listViewPressedIcon;
    @Resource private Icon listViewUnpressedIcon;
    @Resource private Icon tableViewPressedIcon;
    @Resource private Icon tableViewUnpressedIcon;
    
    private final SelectionButton listViewToggleButton;
    private final SelectionButton tableViewToggleButton;
    
    public ViewSelectionPanel(ItemListener list, ItemListener table) {
        GuiUtils.assignResources(this);
        
        listViewToggleButton = new SelectionButton(listViewUnpressedIcon, listViewPressedIcon, true, "List View", list);
        tableViewToggleButton = new SelectionButton(tableViewUnpressedIcon, tableViewPressedIcon, false, "Table View", table);
        
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewToggleButton);
        viewGroup.add(tableViewToggleButton);
        
        add(listViewToggleButton);
        add(tableViewToggleButton);
    }
    
    @Override
    public void setEnabled(boolean value) {
        listViewToggleButton.setEnabled(value);
        tableViewToggleButton.setEnabled(value);
    }
    
    public String getSelectedButton() {
        if(!listViewToggleButton.isEnabled())
            return DISABLED;
        
        if(listViewToggleButton.isSelected()) {
            return LIST_SELECTED;
        } else {
            return TABLE_SELECTED;
        }
    }
    
    /**
     * Toggle button for View Selection
     */
    private final class SelectionButton extends JToggleButton {
        private final Insets insets = new Insets(0, 0, 0, 0);
        
        public SelectionButton(Icon unSelected, Icon selected, boolean isSelected, String toolTip, ItemListener listener) {
            setIcon(unSelected);
            setSelectedIcon(selected);
            setSelected(isSelected);
            setFocusable(false);
            setMargin(insets);
            setToolTipText(toolTip);
            addItemListener(listener);
        }
    }
}
