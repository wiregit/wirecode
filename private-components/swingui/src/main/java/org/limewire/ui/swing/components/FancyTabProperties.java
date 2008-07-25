package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;

public class FancyTabProperties implements Cloneable {
    
    private Painter<?> highlightPainter;
    private Painter<?> normalPainter;
    private Painter<?> selectedPainter;
    private Color selectionColor;
    private Color normalColor;
    private Font textFont;
    private int width;
    
    public FancyTabProperties() {
        highlightPainter = new RectanglePainter<JXPanel>(2, 2, 2, 2, 5, 5, true, Color.YELLOW, 0f, Color.LIGHT_GRAY);
        selectedPainter = new RectanglePainter<JXPanel>(2, 2, 2, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY);
        normalPainter = null;
        selectionColor = new Color(0, 100, 0);
        normalColor = Color.BLUE;
        width = 20;
    }
    
    public FancyTabProperties clone() {
        try {
            return (FancyTabProperties)super.clone();
        } catch(CloneNotSupportedException cnse) {
            throw new Error(cnse);
        }
    }

    public Painter<?> getHighlightPainter() {
        return highlightPainter;
    }

    public void setHighlightPainter(Painter<?> highlightPainter) {
        this.highlightPainter = highlightPainter;
    }

    public Painter<?> getNormalPainter() {
        return normalPainter;
    }

    public void setNormalPainter(Painter<?> normalPainter) {
        this.normalPainter = normalPainter;
    }

    public Painter<?> getSelectedPainter() {
        return selectedPainter;
    }

    public void setSelectedPainter(Painter<?> selectedPainter) {
        this.selectedPainter = selectedPainter;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public void setSelectionColor(Color selectionColor) {
        this.selectionColor = selectionColor;
    }

    public Color getNormalColor() {
        return normalColor;
    }

    public void setNormalColor(Color normalColor) {
        this.normalColor = normalColor;
    }

    public Font getTextFont() {
        return textFont;
    }

    public void setTextFont(Font textFont) {
        this.textFont = textFont;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

}
