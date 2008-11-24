package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;

/**
 * Paints a rounded border like box with 
 *  one pixel inner shadowing/beveling
 */

public class BorderPainter<X> extends AbstractPainter<X> {

    private final int arcWidth;
    private final int arcHeight;
    private final Paint border;
    private final Paint bevelLeft;
    private final Paint bevelTop1;
    private final Paint bevelTop2;
    private final Paint bevelRight;
    private final Paint bevelBottom;
    
    private final boolean hasBubble;
    
    private static final Paint BUBBLE_PAINT1 = new Color(0xee,0xee,0xee);
    private static final Paint BUBBLE_PAINT2 = new Color(0xed,0xed,0xed);
    private static final Paint BUBBLE_PAINT3 = new Color(0xf0,0xf0,0xf0);
    
    private static final Paint SHADOW_PAINT1 = new Color(0x5f,0x5f,0x5f);
    private static final Paint SHADOW_PAINT2 = new Color(0x5e,0x5e,0x5e);
    private static final Paint SHADOW_PAINT3 = new Color(0x64,0x64,0x64);
    
    public BorderPainter(int arcWidth, int arcHeight, Paint border, 
            Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom) {
        
        this(arcWidth, arcHeight, border, 
            bevelLeft, bevelTop1, bevelTop2, 
            bevelRight, bevelBottom, true);
    }
    
    public BorderPainter(int arcWidth, int arcHeight, Paint border, 
            Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, boolean hasBubble) {
        
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.border = border;
        this.bevelLeft = bevelLeft;
        this.bevelTop1 = bevelTop1;
        this.bevelTop2 = bevelTop2;
        this.bevelRight = bevelRight;
        this.bevelBottom = bevelBottom;
        
        this.hasBubble = hasBubble;
        
        this.setCacheable(true);
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        
        int singleArcHeight = this.arcHeight/2;
        
        // Draw upper bevels
        g.setClip(0, 0, width-2, 7);
        g.setPaint(this.bevelTop2);
        g.drawRoundRect(1, 2, width-2, height-5, this.arcWidth, this.arcHeight);
        g.setPaint(this.bevelTop1);
        g.drawRoundRect(1, 1, width-3, height-4, this.arcWidth, this.arcHeight);
        
        // Draw side and bottom bevels
        g.setClip(0, singleArcHeight, width-2, height);
        g.setPaint(this.bevelBottom);        
        g.drawRoundRect(1, 1, width-4, height-4, this.arcWidth, this.arcHeight);
        g.setClip(0, singleArcHeight-1, width-2, height);
        g.setPaint(PaintUtils.resizeGradient(this.bevelLeft, 0, height-singleArcHeight+1));
        g.drawLine(2,singleArcHeight-1,2,height-singleArcHeight);
        g.setPaint(PaintUtils.resizeGradient(this.bevelRight, 0, height-singleArcHeight+1));
        g.drawLine(width-3,singleArcHeight-1,width-3,height-singleArcHeight);
                
        // Draw the bottom accent bubble or shadow
        g.setClip(0, singleArcHeight, width, height);
        g.setPaint(this.hasBubble ? BUBBLE_PAINT3 : SHADOW_PAINT3);
        g.drawRoundRect(0, 0, width-1, height-1, this.arcWidth, this.arcHeight);
        g.setPaint(this.hasBubble ? BUBBLE_PAINT2 : SHADOW_PAINT2);        
        g.drawLine(0,singleArcHeight,0,height/2);
        g.drawLine(width-1,singleArcHeight,width-1,height/2);
        g.setPaint(this.hasBubble ? BUBBLE_PAINT1 : SHADOW_PAINT1);
        g.drawLine(0,height/2,0,height-singleArcHeight);
        g.drawLine(width-1,height/2,width-1,height-singleArcHeight);
         
        g.setClip(0, 0, width, height);
        
        // Draw final border
        g.setPaint(PaintUtils.resizeGradient(this.border, 0, height));
        g.drawRoundRect(1, 0, width-3, height-2, this.arcWidth, this.arcHeight);
    }

}
