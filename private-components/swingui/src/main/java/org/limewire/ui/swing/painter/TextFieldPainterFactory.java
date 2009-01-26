package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Paint;

import javax.swing.JTextField;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TextFieldPainterFactory {

    @Resource private Color promptForeground; 
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color border;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    @Inject
    TextFieldPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public BasicTextFieldBackgroundPainter createBasicBackgroundPainter(AccentType accentType) {
        return new BasicTextFieldBackgroundPainter(border, bevelLeft, bevelTop1,
                bevelTop2, bevelRight, bevelBottom, arcWidth, arcHeight,
                accentType);
    }
    
    public BasicTextFieldBackgroundPainter createBasicBackgroundPainter(AccentType accentType, Paint border) {
        return new BasicTextFieldBackgroundPainter(border, bevelLeft, bevelTop1,
                bevelTop2, bevelRight, bevelBottom, arcWidth, arcHeight,
                accentType);
    }
 
    public FilterPainter<JTextField> createClearableBackgroundPainter() {
        // TODO: ernie
        // return new FilterPainter<JTextField>(null);
        return null;
    }
    
    public BasicTextFieldPromptPainter<JTextField> createBasicPromptPainter() {
        return new BasicTextFieldPromptPainter<JTextField>(promptForeground);
    }
}
