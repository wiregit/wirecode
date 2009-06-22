package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class ImageCellEditor extends JPanel {

    @Resource
    private int width;
    @Resource
    private int height;
    
    @Inject
    public ImageCellEditor(ImageButtons imageButtons) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        setBounds(0, 0, width, height);
        
        add(imageButtons, "growx, aligny top, wrap, gaptop 1, gapright 1");
    }
}
