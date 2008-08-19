package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;

public class SharingBuddyEmptyPanel extends SharingEmptyPanel {

    private SharingCheckBox musicCheckBox;
    private SharingCheckBox videoCheckBox;
    private SharingCheckBox imageCheckBox;
    
    private JButton shareButton;
    
    public SharingBuddyEmptyPanel(String name, Icon icon) {
        super(name, icon);
        
        musicCheckBox = new SharingCheckBox("All my music");
        videoCheckBox = new SharingCheckBox("All my video");
        imageCheckBox = new SharingCheckBox("All my images");
        
        shareButton = new JButton("Shared with all my friends");
        shareButton.setBackground(Color.BLACK);
        shareButton.setForeground(Color.WHITE);
        shareButton.setFocusable(false);
        
        add(musicCheckBox, "sizegroupx1, center, gaptop 30, wrap");
        add(videoCheckBox, "sizegroupx1, center, wrap");
        add(imageCheckBox, "sizegroupx1, center, wrap 30");
        add(shareButton, "center, wrap");

    }
    
    private class SharingCheckBox extends JCheckBox {
        
        public SharingCheckBox(String text) {
            super(text);
            setBorder(null);
            setFocusable(false);
            setOpaque(false);
        }
    }
}
