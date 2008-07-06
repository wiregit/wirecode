/**
 * 
 */
package org.limewire.ui.swing.mainframe;


import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 */
public class HomePanel extends JPanel{
    
    public static final String NAME = "Home";

    public HomePanel(){
        add(new JLabel(NAME));
    }
}
