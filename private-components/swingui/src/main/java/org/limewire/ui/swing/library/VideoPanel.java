/**
 * 
 */
package org.limewire.ui.swing.library;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 */
public class VideoPanel extends JPanel {
    public static final String NAME = "Videos";
    
    public VideoPanel(){
        add(new JLabel(NAME));
    }
}
