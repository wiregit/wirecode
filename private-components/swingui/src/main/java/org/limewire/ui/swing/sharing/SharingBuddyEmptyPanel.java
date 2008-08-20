package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class SharingBuddyEmptyPanel extends JPanel {

    @Resource
    Icon buddyIcon;
    
    private JLabel title;
    private JLabel text;
    
    private SharingCheckBox musicCheckBox;
    private SharingCheckBox videoCheckBox;
    private SharingCheckBox imageCheckBox;
    
    private JButton shareButton;
    
    public SharingBuddyEmptyPanel() {
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        title = new JLabel(buddyIcon);
        title.setText("You are not sharing anything with All Friends");
        
        text = new JLabel();
        text.setText("To share with All Friends, drag files here, or use the shortcuts below to share files");
        
        musicCheckBox = new SharingCheckBox("All my music");
        videoCheckBox = new SharingCheckBox("All my video");
        imageCheckBox = new SharingCheckBox("All my images");
        
        shareButton = new JButton("Shared with all my friends");
        shareButton.setFocusable(false);
        
        setLayout(new MigLayout("", "[grow]", ""));
        
        add(title, "center, gaptop 120, wrap 70");
        add(text, "center, top, wrap");
        
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
