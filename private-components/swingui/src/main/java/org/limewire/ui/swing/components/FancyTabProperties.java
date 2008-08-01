package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.I18n;

class FancyTabProperties implements Cloneable {
    
    private Painter<JXButton> highlightPainter;
    private Painter<JXButton> normalPainter;
    private Painter<JXButton> selectedPainter;
    private Color selectionColor;
    private Color normalColor;
    private Font textFont;
    private boolean removable;
    private String closeOneText;
    private String closeAllText;
    private String closeOtherText;

    FancyTabProperties() {
        highlightPainter = new RectanglePainter<JXButton>(2, 2, 2, 2, 5, 5, true, Color.YELLOW, 0f, Color.LIGHT_GRAY);
        selectedPainter = new RectanglePainter<JXButton>(2, 2, 2, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY);
        normalPainter = null;
        selectionColor = new Color(0, 100, 0);
        normalColor = Color.BLUE;
        removable = false;
        closeOneText = I18n.tr("Close Tab");
        closeAllText = I18n.tr("Close All Tabs");
        closeOtherText = I18n.tr("Close Other Tabs");
    }
    
    public FancyTabProperties clone() {
        try {
            return (FancyTabProperties)super.clone();
        } catch(CloneNotSupportedException cnse) {
            throw new Error(cnse);
        }
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public Painter<JXButton> getHighlightPainter() {
        return highlightPainter;
    }

    public void setHighlightPainter(Painter<JXButton> highlightPainter) {
        this.highlightPainter = highlightPainter;
    }

    public Painter<JXButton> getNormalPainter() {
        return normalPainter;
    }

    public void setNormalPainter(Painter<JXButton> normalPainter) {
        this.normalPainter = normalPainter;
    }

    public Painter<JXButton> getSelectedPainter() {
        return selectedPainter;
    }

    public void setSelectedPainter(Painter<JXButton> selectedPainter) {
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

    public String getCloseOneText() {
        return closeOneText;
    }

    public String getCloseAllText() {
        return closeAllText;
    }

    public String getCloseOtherText() {
        return closeOtherText;
    }
    
    public void setCloseOneText(String closeOneText) {
        this.closeOneText = closeOneText;
    }

    public void setCloseAllText(String closeAllText) {
        this.closeAllText = closeAllText;
    }

    public void setCloseOtherText(String closeOtherText) {
        this.closeOtherText = closeOtherText;
    }


}
