package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.GuiUtils;

public class SharingEmptyPanel extends JPanel {

    private JLabel title;
    private JLabel text;
    
    public SharingEmptyPanel(String name, Icon icon) {
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        title = new JLabel(icon);
        title.setText("You are not sharing anything with the " + name);
        
        text = new JLabel();
        text.setText("To share with the " + name + ", drag files here");
        
        setLayout(new MigLayout("", "[grow]", "[][]"));
        
        add(title, "center, gaptop 120, wrap 70");
        add(text, "center, top, wrap");
    }
}
