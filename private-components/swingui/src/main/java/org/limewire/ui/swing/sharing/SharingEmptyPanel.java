package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Singleton;

@Singleton
public class SharingEmptyPanel extends JPanel {

    @Resource
    Icon gnutellaIcon;
    
    private JLabel title;
    private JLabel text;
    
    public SharingEmptyPanel() {
        
        GuiUtils.assignResources(this); 
        
        setBackground(Color.WHITE);
        
        title = new JLabel(gnutellaIcon);
        title.setText(I18n.tr("You are not sharing anything with the LimeWire Network"));
        
        text = new JLabel();
        text.setText(I18n.tr("To share with the LimeWire Network, go to My Library"));
        
        setLayout(new MigLayout("", "[grow]", "[][]"));
        
        add(title, "center, gaptop 120, wrap 70");
        add(text, "center, top, wrap");
    }
}
