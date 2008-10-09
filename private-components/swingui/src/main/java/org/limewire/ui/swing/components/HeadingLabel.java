package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;

public class HeadingLabel extends JLabel {
    
    //TODO: make resources
    private int dropOpacity = 150;
    
    public HeadingLabel(String text) {
        super(text);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String label = this.getText();
        Rectangle2D labelRect = this.getFont().getStringBounds(label, g2.getFontRenderContext());
        
        g2.setFont(this.getFont());
        
        g2.setColor(new Color(0,0,0,dropOpacity));
        g2.drawString(label, 1, (int) labelRect.getHeight()+1 -3);
        g2.setColor(Color.WHITE);
        g2.drawString(label, 0, (int) labelRect.getHeight() -3);
    }

}
