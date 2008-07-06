/**
 * 
 */
package org.limewire.ui.swing.library;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 */
public class ImagePanel extends JPanel{
    public static final String NAME = "Images";
    
    public ImagePanel(){
        add(new JLabel(NAME));
    }
}
