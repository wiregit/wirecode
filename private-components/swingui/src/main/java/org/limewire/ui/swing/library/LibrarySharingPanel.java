package org.limewire.ui.swing.library;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibrarySharingPanel extends JPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    
    @Inject
    public LibrarySharingPanel() {
        super(new MigLayout("insets 0, gap 0, fill", "[125!]", ""));
        
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
        
        setVisible(false);
    }
}
