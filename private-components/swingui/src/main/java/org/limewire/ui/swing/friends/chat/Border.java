package org.limewire.ui.swing.friends.chat;

import java.awt.*;

/**
 * A panel containing a single component, around which a border is drawn. Of
 * course, the single component may be a container which may contain other
 * components, so a Border can surround multiple components.
 * <p>
 * <p/> Thickness of the border, and the gap between the Component and the
 * border are specified at time of construction. 
 * <p>
 * <p/> Border color may be set via setLineColor(Color).
 * <p>
 * <p/> Border employs a DrawnRectangle to paint the border. Derived classes are
 * free to override DrawnRectangle border() if they wish to use an extension of
 * DrawnRectangle for drawing their border.
 * <p>
 * 
 * @author David Geary
 * @version 1.1, Nov 8 1996 <p/> Added getComponent() for accessing component
 *          bordered.
 * @see DrawnRectangle
 */
class Border extends Panel {
    private int thickness;
    private int gap;
    private DrawnRectangle border;

    public Border(int thickness, int gap) {
        this.thickness = thickness;
        this.gap = gap;

        setLayout(new BorderLayout());
    }
    
    @Override
    public Component add(Component comp) {
        add(comp, BorderLayout.CENTER);
        return comp;
    }

    public Rectangle getInnerBounds() {
        return border().getInnerBounds();
    }

    public void setLineColor(Color c) {
        border().setLineColor(c);
    }

    public Color getLineColor() {
        return border().getLineColor();
    }

    public void paint(Graphics g) {
        border().paint();
        super.paint(g); // ensures lightweight comps get drawn
    }

    /**
     * @deprecated for JDK1.1
     */
    @SuppressWarnings("deprecation")
    public Insets insets() {
        return new Insets(thickness + gap, thickness + gap, thickness + gap, thickness + gap);
    }

    public Insets getInsets() {
        return insets();
    }

    /**
     * @deprecated for JDK1.1
     */
    @SuppressWarnings("deprecation")
    public void resize(int w, int h) {
        Point location = getLocation();
        setBounds(location.x, location.y, w, h);
    }

    public void setSize(int w, int h) {
        resize(w, h);
    }

    /**
     * @deprecated for JDK1.1
     */
    @SuppressWarnings("deprecation")
    public void reshape(int x, int y, int w, int h) {
        // compiler will issue a deprecation warning, but we can't call
        // super.setBounds()!
        super.reshape(x, y, w, h);
        border().setSize(w, h);
    }

    public void setBounds(int x, int y, int w, int h) {
        reshape(x, y, w, h);
    }

    protected String paramString() {
        return super.paramString() + ",border=" + border().toString() + ",thickness=" + thickness
                + ",gap=" + gap;
    }

    protected DrawnRectangle border() {
        if (border == null)
            border = new DrawnRectangle(this, thickness);
        return border;
    }
}