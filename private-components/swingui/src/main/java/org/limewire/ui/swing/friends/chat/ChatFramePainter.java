package org.limewire.ui.swing.friends.chat;

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
 * Paints the border of the chat frame, using two colors: one for the majority
 * of the border and one for the portion of the lower-right edge that is adjacent
 * to the chat button.
 */
class ChatFramePainter extends AbstractAreaPainter<JXPanel> {
    private double secondaryColorWidth;
    private Paint secondaryBorderPaint;
    
    public ChatFramePainter(Paint primaryBorderPaint, Paint secondaryBorderPaint) {
        setBorderPaint(primaryBorderPaint);
        this.secondaryBorderPaint = secondaryBorderPaint;
    }
    
    public void setSecondaryBorderPaint(Paint secondaryBorderPaint) {
        Paint oldPaint = secondaryBorderPaint;
        this.secondaryBorderPaint = secondaryBorderPaint;
        setDirty(true);
        firePropertyChange("secondaryBorderPaint", oldPaint, secondaryBorderPaint);
    }
    
    public void setSecondaryColorWidth(double width) {
        double oldWidth = secondaryColorWidth;
        this.secondaryColorWidth = width;
        setDirty(true);
        firePropertyChange("secondaryColorWidth", oldWidth, width);
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
        
        Paint p = getBorderPaint();

        g.setPaint(p);

        g.setStroke(new BasicStroke(1.0f));
        double x = shape.getX();
        double y = shape.getY();
        double h = shape.getHeight();
        double w = shape.getWidth();
        g.draw(new Line2D.Double(x, y, x, (y + h)));
        g.draw(new Line2D.Double(x, y, (x + w), y));
        g.draw(new Line2D.Double((x + w -1), y, (x + w - 1), (y + h)));
        g.draw(new Line2D.Double(x, (y + h - 1), (x + w - secondaryColorWidth), (y + h - 1)));
        g.setPaint(secondaryBorderPaint);
        g.draw(new Line2D.Double((x + w - secondaryColorWidth + 1), (y + h -1), (x + w - 2), (y + h - 1)));
    }
}
