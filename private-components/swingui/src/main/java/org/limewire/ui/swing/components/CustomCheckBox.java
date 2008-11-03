package org.limewire.ui.swing.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.JCheckBox;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

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
        
        Graphics2D g2 = (Graphics2D) g;
        
        // get original antialiasing value for reset
        Object origAntiAliasHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String label = this.getText();
        Rectangle2D labelRect = this.getFont().getStringBounds(label, g2.getFontRenderContext());
        
        g2.setFont(this.getFont());
        
        g2.setColor(this.dropColour);
        g2.drawString(label, 16, (int) labelRect.getHeight()+2);
        g2.setColor(Color.WHITE);
        g2.drawString(label, 15, (int) labelRect.getHeight()+1);
        
        int top = this.getHeight() / 2 - 6;
        
        g2.fillRect(0,top,12,12);
        g2.setColor(checkBorder);
        g2.drawRect(0,top,12,12);
        
        if (this.isSelected()) {
            g2.setStroke(new BasicStroke(2));
            
            g2.drawLine(3,top + 3, 9, top + 9);
            g2.drawLine(9,top + 3, 3, top + 9);
        }
       
        // reset antialiasing propery
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntiAliasHint);
    }

}
