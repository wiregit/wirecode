package org.limewire.ui.swing.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.JCheckBox;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class CustomCheckBox extends JCheckBox {
    
    @Resource
    private int dropOpacity;
    
    @Resource
    private Color checkBorder;
    
    private final Color dropColour;
    
    public CustomCheckBox(Action action) {
        super(action);
        
        GuiUtils.assignResources(this);
        
        this.dropColour = new Color(0,0,0,dropOpacity);
        this.setOpaque(false);
    }
    
    
    @Override
    protected void paintComponent(Graphics g) {
        
        // TODO: Remove this class or use painters.
        
        Graphics2D g2 = (Graphics2D) g;
        
        String label = this.getText();
        Rectangle2D labelRect = this.getFont().getStringBounds(label, g2.getFontRenderContext());
        
        g2.setFont(this.getFont());
        
        g2.setColor(this.dropColour);
        PainterUtils.drawSmoothString(g2, label, 16, (int) labelRect.getHeight()+4);
        g2.setColor(Color.WHITE);
        PainterUtils.drawSmoothString(g2, label, 17, (int) labelRect.getHeight()+3);
        
        int top = this.getHeight() / 2 - 6;
        
        g2.fillRect(0,top,12,12);
        g2.setColor(checkBorder);
        g2.drawRect(0,top,12,12);
        
        if (this.isSelected()) {
            g2.setStroke(new BasicStroke(2));
            
            g2.drawLine(3,top + 3, 9, top + 9);
            g2.drawLine(9,top + 3, 3, top + 9);
        }
      
    }

}
