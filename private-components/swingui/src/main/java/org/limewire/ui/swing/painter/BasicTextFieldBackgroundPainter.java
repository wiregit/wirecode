package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.JTextField;

import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;

public class BasicTextFieldBackgroundPainter extends CompoundPainter<JTextField> {

    public BasicTextFieldBackgroundPainter(Paint border, Paint bevelLeft, Paint bevelTop1,
            Paint bevelTop2, Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight,
            AccentType accentType) {
        
        RectanglePainter<JTextField> textBackgroundPainter = new RectanglePainter<JTextField>();
        
        textBackgroundPainter.setRounded(true);
        textBackgroundPainter.setFillPaint(Color.WHITE);
        textBackgroundPainter.setRoundWidth(arcWidth);
        textBackgroundPainter.setRoundHeight(arcHeight);
        textBackgroundPainter.setInsets(new Insets(2,2,2,2));
        textBackgroundPainter.setBorderPaint(null);
        textBackgroundPainter.setFillVertical(true);
        textBackgroundPainter.setFillHorizontal(true);
        textBackgroundPainter.setAntialiasing(true);
        textBackgroundPainter.setCacheable(true);
        
        setPainters(textBackgroundPainter, new BorderPainter<JTextField>(arcWidth, arcHeight,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, accentType));
        
        setCacheable(true);
    }
    
}
