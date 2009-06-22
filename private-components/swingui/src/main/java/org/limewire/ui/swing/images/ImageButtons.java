package org.limewire.ui.swing.images;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.library.table.RemoveButton;

import com.google.inject.Inject;

/**
 * Draws a button row above thumbnails.
 */
public class ImageButtons extends JPanel {
    
    @Inject
    public ImageButtons(RemoveButton removeButton) {
        super(new MigLayout("fillx, insets 0, gap 0"));
        
        setOpaque(false);
        
        add(removeButton, "alignx right, gapright 5, gaptop 5");
    }
    
}
