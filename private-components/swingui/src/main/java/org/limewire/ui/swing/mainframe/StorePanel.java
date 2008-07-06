/**
 * 
 */
package org.limewire.ui.swing.mainframe;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 */
public class StorePanel  extends JPanel{
    public static final String NAME = "LimeWire Store";
    
    public StorePanel(){
        add(new JLabel(NAME));
    }
}
