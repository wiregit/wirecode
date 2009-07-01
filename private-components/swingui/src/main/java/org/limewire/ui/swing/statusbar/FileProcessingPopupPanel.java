package org.limewire.ui.swing.statusbar;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLayeredPane;

import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;

import com.google.inject.Inject;

public class FileProcessingPopupPanel extends OverlayPopupPanel {
    
    private final Component parentButton;
    private final FileProcessingPopupContentPanel childPanel;
    
    @Inject
    public FileProcessingPopupPanel(@GlobalLayeredPane JLayeredPane layeredPane,
            FileProcessingPopupContentPanel childPanel,
            FileProcessingPanel parent) {
        super(layeredPane, childPanel);

        this.parentButton = parent;
        this.childPanel = childPanel;
        
        resize();
        validate();
    }

    @Inject
    public void register() {
        parentButton.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                resize();
            }
        });
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = layeredPane.getBounds();
        int w = 200;
        int h = 46;
        
        int x = 0; 
            
        if (parentButton != null) {
            x = parentButton.getX();
        }
        
        setBounds(x, parentBounds.height - h, w, h);
    }
    
    public void notifyDone() {
        childPanel.notifyDone();
    }
}
