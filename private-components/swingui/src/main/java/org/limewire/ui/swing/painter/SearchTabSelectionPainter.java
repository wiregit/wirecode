package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Area;

import javax.swing.JComponent;

import org.jdesktop.swingx.painter.RectanglePainter;

public class SearchTabSelectionPainter extends RectanglePainter<JComponent> {

    // TODO: Use resources.
    public SearchTabSelectionPainter() {
        setInsets(new Insets(0, 0, 0, 0));
        setFillVertical(true);
        setFillHorizontal(true);
        setRoundWidth(-10);
        setRoundHeight(-10);
        setRounded(true);
        setBorderWidth(0f);
        setFillPaint(new GradientPaint(0, 0, new Color(0f, 0f, 0f, 0.32f), 0, 1, Color.decode("#787878")));
        setBorderPaint(new GradientPaint(0, 0, new Color(0f, 0f, 0f, 0.32f), 0, 1, Color.decode("#787878")));
        setPaintStretched(true);
    }
    
    @Override
    public Shape provideShape(Graphics2D g, JComponent comp, int width, int height) {
        Shape shape = super.provideShape(g, comp, width, height);
        Area area = new Area(shape);
        
        // TODO: Add the bottom area that curves outwards.
        return area;
    }
    
    
}