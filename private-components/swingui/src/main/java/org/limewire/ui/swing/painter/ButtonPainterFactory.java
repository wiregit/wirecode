package org.limewire.ui.swing.painter;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ButtonPainterFactory {
    
    @Resource private Color miniHoverTextForeground;
    @Resource private Color miniDownTextForeground;
    
    @Resource private Color darkFullHoverTextForeground;
    @Resource private Color darkFullDownTextForeground;
    
    @Inject
    ButtonPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public ButtonForegroundPainter createMiniButtonForegroundPainter() {
        return new ButtonForegroundPainter(miniHoverTextForeground, miniDownTextForeground);
    }
    
    public ButtonForegroundPainter createLightFullButtonForegroundPainter() {
        return new ButtonForegroundPainter();
    }
    
    public ButtonForegroundPainter createDarkFullButtonForegroundPainter() {
        return new ButtonForegroundPainter(darkFullHoverTextForeground, darkFullDownTextForeground);
    }

    public PopupButtonBackgroundPainter createMiniButtonBackgroundPainter() {
        return new PopupButtonBackgroundPainter();
    }

    public ButtonBackgroundPainter createLightFullButtonBackgroundPainter() {
        return new LightButtonBackgroundPainter();
    }
    
    public ButtonBackgroundPainter createDarkFullButtonBackgroundPainter() {
        return new DarkButtonBackgroundPainter();
    }
    
    public ButtonBackgroundPainter createDarkFullButtonBackgroundPainter(DrawMode mode) {
        return new DarkButtonBackgroundPainter(mode);
    }
}
