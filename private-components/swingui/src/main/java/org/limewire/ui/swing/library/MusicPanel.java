/**
 * 
 */
package org.limewire.ui.swing.library;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 */
public class MusicPanel extends JPanel {
    public static final String NAME = "Music";
    
    public MusicPanel(){
        add(new JLabel(NAME));
    }
}
