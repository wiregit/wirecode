package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.painter.Painter;

public class SearchTabSelectionPainter implements Painter {
    
    private Area tabAreaCache   = null;
    private int  tabWidthCache  = 0;
    private int  tabHeightCache = 0;
    
    public SearchTabSelectionPainter() {
    }

    
    private void cacheTabArea(int width, int height) {
        
        if (this.tabWidthCache == width && this.tabHeightCache == height)  return;        
        
        Area compound = new Area(new RoundRectangle2D.Float(10, 0, width-1-20, height-1, 20, 20));
        
        compound.add(new Area(new Rectangle2D.Float(0,height-10-1,width-1,height-1)));
        
        compound.subtract(new Area(new Arc2D.Float(-10,height-1-21,20,20,270,90,Arc2D.PIE)));
        compound.subtract(new Area(new Arc2D.Float(width-1-10,height-1-21,20,20,360,270,Arc2D.PIE)));
        
        this.tabAreaCache   = compound;
        this.tabWidthCache  = width;
        this.tabHeightCache = height;
    }
    
    @Override
    public void paint(Graphics2D g, Object object, int width, int height) {

        cacheTabArea(width, height);
        
        g.setPaint(new GradientPaint(0, 0, new Color(0f, 0f, 0f, 0.32f), 0, 1, Color.decode("#787878")));
        g.fill(this.tabAreaCache);
    }

}