package org.limewire.ui.swing.action;

import org.limewire.ui.swing.util.GuiUtils;

/**
 * Abstract class that allows the name of the menu to have an ampersand to 
 * mark the mnemonic of the action in its name.
 */
public abstract class AbstractMenu extends javax.swing.JMenu {
    
    public AbstractMenu(String name) {
        super();
        int mnemonicKeyCode = GuiUtils.getMnemonicKeyCode(name);
        name = GuiUtils.stripAmpersand(name);
        setMnemonic(mnemonicKeyCode);
        setText(name);
    }
}
