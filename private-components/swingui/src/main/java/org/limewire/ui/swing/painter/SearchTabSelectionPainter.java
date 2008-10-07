package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;

import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.jdesktop.swingx.painter.AbstractAreaPainter.Style;

public class SearchTabSelectionPainter extends CompoundPainter<JComponent> {
    
    
    public SearchTabSelectionPainter() {
        RectanglePainter background = new RectanglePainter();
        background.setStyle(Style.FILLED);
        background.setFillPaint(Color.decode("#787878"));
        background.setRoundHeight(10);
        background.setRoundWidth(10);
        background.setFillHorizontal(true);
        background.setFillVertical(true);
        background.setRounded(true);
        background.setPaintStretched(true);
        
        RectanglePainter gradient = new RectanglePainter();
        gradient.setStyle(Style.FILLED);
        gradient.setFillPaint(new GradientPaint(0, 0, new Color(0f, 0f, 0f, 0.32f), 0, 1, Color.decode("#787878")));
        gradient.setRoundHeight(10);
        gradient.setRoundWidth(10);
        gradient.setFillHorizontal(true);
        gradient.setFillVertical(true);
        gradient.setRounded(true);
        gradient.setPaintStretched(true);
        
        setPainters(background,
                    gradient);

    }
}