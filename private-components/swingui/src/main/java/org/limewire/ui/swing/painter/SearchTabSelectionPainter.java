package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.painter.Painter;

public class SearchTabSelectionPainter implements Painter {
    
    
    public SearchTabSelectionPainter() {
        
    }

    @Override
    public void paint(Graphics2D g, Object object, int width, int height) {
        Area compound = new Area(new RoundRectangle2D.Float(0, 0, width-1, height-1, 10, 10));
        
        compound.add(new Area(new Rectangle2D.Float(0,height-11,width-1,height-1)));
        
        g.setPaint(new GradientPaint(0, 0, new Color(0f, 0f, 0f, 0.32f), 0, 1, Color.decode("#787878")));
        g.fill(compound);
    }

}