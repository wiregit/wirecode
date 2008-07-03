package org.limewire.ui.swing.mainframe;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

public class LimeMenuBar extends JMenuBar {
    
     public LimeMenuBar() {
         add(new JMenu("File"));
         add(new JMenu("Edit"));
         add(new JMenu("Tools"));
         add(new JMenu("Help"));
    }

}
