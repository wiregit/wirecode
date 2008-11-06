package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.AbstractPainter;

public class HeadingLabel extends JXLabel {
    
    private static AbstractPainter<JXLabel> painter = new TextShadowPainter();
        
        
        private static class TextShadowPainter extends AbstractPainter<JXLabel> {
        
            private final Color shadowColour = new Color(0,0,0,150);
            
            TextShadowPainter() {
                this.setAntialiasing(true);
            }
            
            @Override
            protected void doPaint(Graphics2D g, JXLabel object, int width, int height) {
                String label = object.getText();
                
                g.setFont(object.getFont());
                
                int h = g.getFontMetrics().getAscent();
                Insets insets = object.getInsets();
                
                g.setColor(this.shadowColour);
                g.drawString(label, insets.left+1, insets.top+h-2);
                g.setColor(Color.WHITE);
                g.drawString(label, insets.left, insets.top+h-3);
            }
    };
    
    public HeadingLabel(String text) {
        super(text);
        
        this.setForegroundPainter(painter);
    }
}
