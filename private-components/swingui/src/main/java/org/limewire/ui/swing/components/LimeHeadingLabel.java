package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.painter.AbstractPainter;

public class LimeHeadingLabel extends JXLabel {
    
    private static AbstractPainter<JXLabel> sharedShadowPainter = new TextShadowPainter();
        
    private static class TextShadowPainter extends AbstractPainter<JXLabel> {
        
        private final Color shadowColour = new Color(0,0,0,150);
            
        TextShadowPainter() {
            this.setAntialiasing(true);
            this.setCacheable(true);
        }
            
        @Override
        protected void doPaint(Graphics2D g, JXLabel object, int width, int height) {
            String label = object.getText();
            
            g.setFont(object.getFont());
                
            int h = g.getFontMetrics().getAscent();
                
            g.setColor(this.shadowColour);
            g.drawString(label, 1, height/2 + h/2 - 1);
            g.setColor(Color.WHITE);
            g.drawString(label, 0, height/2 + h/2 - 2);
        }
    };
    
    public LimeHeadingLabel(String text) {
        super(text);
        
        this.setForegroundPainter(sharedShadowPainter);
    }
}
