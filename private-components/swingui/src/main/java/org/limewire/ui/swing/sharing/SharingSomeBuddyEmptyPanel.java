package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

@Singleton
public class SharingSomeBuddyEmptyPanel extends JPanel {

    @Resource
    Icon someIcon;
    
    private JLabel title;
    
    public  SharingSomeBuddyEmptyPanel() {
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
    
        title = new JLabel(someIcon);
        title.setText("Share files with Specific Friends by dragging from your library to their name");
        
        setLayout(new MigLayout("", "[grow]", "[][]"));
        
        add(title, "center, gaptop 120, wrap 70");
    }
}
