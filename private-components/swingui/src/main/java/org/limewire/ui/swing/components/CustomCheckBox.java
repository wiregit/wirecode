package org.limewire.ui.swing.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.JCheckBox;

public class CustomCheckBox extends JCheckBox{
    
    public CustomCheckBox(Action action) {
        super(action);
        
        this.setOpaque(false);
    }
    
    
    @Override
    protected void paintComponent(Graphics g) {
        
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        
        String label = this.getText();
        Rectangle2D labelRect = this.getFont().getStringBounds(label, g2.getFontRenderContext());
        
        g2.setFont(this.getFont());
        g2.drawString(label, 13, (int) labelRect.getHeight()+1);
        
        int top = this.getHeight() / 2 - 5;
        
        g2.fillRect(0,top,10,10);
        g2.setColor(new Color(0x31,0x31,0x31));
        g2.drawRect(0,top,10,10);
        
        if (this.isSelected()) {
            g2.setStroke(new BasicStroke(2));
            // g2.drawLine(3,top + 3, 8, top + 8);
            // g2.drawLine(8,top + 3, 3, top + 8);
            
            g2.drawLine(3,top + 3, 7, top + 7);
            g2.drawLine(7,top + 3, 3, top + 7);
        }
        
        
           
    }

}
