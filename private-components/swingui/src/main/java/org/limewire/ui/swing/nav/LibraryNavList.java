package org.limewire.ui.swing.nav;

import org.limewire.ui.swing.library.DocumentPanel;
import org.limewire.ui.swing.library.ImagePanel;
import org.limewire.ui.swing.library.MusicPanel;
import org.limewire.ui.swing.library.VideoPanel;

public class LibraryNavList extends NavList {
    
    LibraryNavList() {
        super("Library");
        
        addListItem(MusicPanel.NAME);
        addListItem(VideoPanel.NAME);
        addListItem(ImagePanel.NAME);
        addListItem(DocumentPanel.NAME);
    }

}
