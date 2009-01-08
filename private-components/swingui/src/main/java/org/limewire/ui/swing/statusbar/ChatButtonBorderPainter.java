package org.limewire.ui.swing.statusbar;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractAreaPainter;

/**
 * Paints the border around the panel containing the chat button on the status bar
 * The border changes color and sometimes doesn't paint the top edge in order
 * to produce a visual "tab" effect with the visible chat/friends panel.
 */
class ChatButtonBorderPainter extends AbstractAreaPainter<JXPanel> {
    private Paint secondaryBorderPaint;
    
    public ChatButtonBorderPainter(Paint primaryBorderPaint, Paint secondaryBorderPaint) {
        setBorderPaint(primaryBorderPaint);
        this.secondaryBorderPaint = secondaryBorderPaint;
    }
    
    public void setSecondaryBorderPaint(Paint secondaryBorderPaint) {
        Paint oldPaint = secondaryBorderPaint;
        this.secondaryBorderPaint = secondaryBorderPaint;
        setDirty(true);
        firePropertyChange("secondaryBorderPaint", oldPaint, secondaryBorderPaint);
    }
    
    @Override
    protected Shape provideShape(Graphics2D g, JXPanel comp, int width, int height) {
        return calculateShape(width, height);
    }
    
    protected RectangularShape calculateShape(int width, int height) {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;

        return new Rectangle2D.Double(x, y, width, height);
    }

    @Override
    protected void doPaint(Graphics2D g, JXPanel object, int width, int height) {
        RectangularShape shape = calculateShape(width, height);
        
        Paint primaryBorderPaint = getBorderPaint();

        g.setPaint(primaryBorderPaint);

        g.setStroke(new BasicStroke(1.0f));
        double x = shape.getX();
        double y = shape.getY();
        double h = shape.getHeight();
        double w = shape.getWidth();
        //left edge
        g.draw(new Line2D.Double(x, y, x, (y + h)));
        //right edge
        g.draw(new Line2D.Double((x + w -1), y, (x + w - 1), (y + h)));
        //bottom edge
        g.draw(new Line2D.Double(x, (y + h - 1), (x + w - 2), (y + h - 1)));
        if (secondaryBorderPaint != null) {
            g.setPaint(secondaryBorderPaint);
            //top edge
            g.draw(new Line2D.Double(x, y, (x + w), y));
        }
    }

}
