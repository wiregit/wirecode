package org.limewire.ui.swing.nav;

import org.limewire.ui.swing.library.DocumentPanel;
import org.limewire.ui.swing.library.ImagePanel;
import org.limewire.ui.swing.library.MusicPanel;
import org.limewire.ui.swing.library.VideoPanel;
import org.limewire.ui.swing.nav.Navigator.NavItem;

public class LibraryNavList extends NavList {
    
    LibraryNavList(Navigator navigator) {
        super("Library", NavItem.LIBRARY, navigator);
        
        addNavItem(new DocumentPanel(), DocumentPanel.NAME);
        addNavItem(new ImagePanel(), ImagePanel.NAME);
        addNavItem(new MusicPanel(), MusicPanel.NAME);
        addNavItem(new VideoPanel(), VideoPanel.NAME);
    }

}
