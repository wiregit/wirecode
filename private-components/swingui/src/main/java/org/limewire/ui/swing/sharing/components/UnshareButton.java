package org.limewire.ui.swing.sharing.components;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a button with an remove Icon
 */
public class UnshareButton extends JButton {

    @Resource
    Icon removeIcon;
    
    public UnshareButton() {
        GuiUtils.assignResources(this); 
        
        setIcon(removeIcon);
    }
    
    public UnshareButton(Action action) {
        super(action);
        
        GuiUtils.assignResources(this); 
        
        setIcon(removeIcon);
    }
}
