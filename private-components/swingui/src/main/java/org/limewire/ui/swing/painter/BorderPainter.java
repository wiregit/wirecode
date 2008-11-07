package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Paints a rounded border like box with 
 *  one pixel inner shadowing/beveling
 */

public class BorderPainter<X> extends AbstractPainter<X> {

    // Corner coordinates
    private final int x = 0; 
    private final int y = 0;
    
    private final int arcWidth;
    private final int arcHeight;
    private final Color border;
    private final Color borderBevel1;
    private final Color borderBevel2;
    private final Color borderBevel3;
    
    public BorderPainter(int arcWidth, int arcHeight, Color border, 
            Color bevel1, Color bevel2, Color bevel3) {
        
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.border = border;
        this.borderBevel1 = bevel1;
        this.borderBevel2 = bevel2;
        this.borderBevel3 = bevel3;
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
     
        // Draw shading/bevel
        g.setColor(this.borderBevel1);
        g.drawRoundRect(x, y+1, width-1, height-3, this.arcWidth, this.arcHeight);
        g.drawLine(x+arcWidth/2, y+height-1, x+width-this.arcWidth/2-1, y+height-2);
        g.setColor(this.borderBevel2);
        g.drawLine(x+arcWidth/2, y+1, x+width-this.arcWidth/2-1, y);
        g.setColor(this.borderBevel3);
        g.drawLine(x+width-1, y+this.arcHeight/2, x+width-2, y+height-this.arcHeight/2-1);
        
        // Draw final border
        g.setColor(this.border);
        g.drawRoundRect(x, y, width-1, height-1, this.arcWidth, this.arcHeight);
        
    }

}
