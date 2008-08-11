package org.limewire.ui.swing.sharing;

import java.awt.Insets;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class ViewSelectionPanel extends JPanel {

    @Resource private Icon listViewPressedIcon;
    @Resource private Icon listViewUnpressedIcon;
    @Resource private Icon tableViewPressedIcon;
    @Resource private Icon tableViewUnpressedIcon;
    
    private final JToggleButton listViewToggleButton;
    private final JToggleButton tableViewToggleButton;
    
    public ViewSelectionPanel(ItemListener list, ItemListener table) {
        
        listViewToggleButton = new JToggleButton();
        tableViewToggleButton = new JToggleButton();
        
        GuiUtils.assignResources(this);
        
        configureViewButtons(list, table);
        
        add(listViewToggleButton);
        add(tableViewToggleButton);
    }
    
    private void configureViewButtons(ItemListener list, ItemListener table) {
        Insets insets = new Insets(0, 0, 0, 0);
        
        listViewToggleButton.setIcon(listViewUnpressedIcon);
        listViewToggleButton.setPressedIcon(listViewPressedIcon);
        listViewToggleButton.setSelected(true);
        listViewToggleButton.setMargin(insets);
        listViewToggleButton.setToolTipText("List View");
        listViewToggleButton.addItemListener(list);
        
        tableViewToggleButton.setIcon(tableViewUnpressedIcon);
        tableViewToggleButton.setPressedIcon(tableViewPressedIcon);
        tableViewToggleButton.setMargin(insets);
        tableViewToggleButton.setToolTipText("Table View");
        tableViewToggleButton.addItemListener(table);
        tableViewToggleButton.setSelected(true);
        
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewToggleButton);
        viewGroup.add(tableViewToggleButton);
    }
    
    
}
