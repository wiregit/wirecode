package org.limewire.ui.swing.util;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.painter.ButtonPainterFactory;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ButtonDecorator {

    private final ButtonPainterFactory painterFactory;
    
    @Resource private Font  miniTextFont;
    @Resource private Color miniTextForeground;
    
    @Resource private Font  lightFullTextFont;
    @Resource private Color lightFullTextForeground;
    
    @Resource private Font  darkFullTextFont;
    @Resource private Color darkFullTextForeground;
    
    @Inject
    ButtonDecorator(ButtonPainterFactory painterFactory) {
        GuiUtils.assignResources(this);
        
        this.painterFactory = painterFactory;
    }
    
    public void decorateMiniButton(JXButton button) {
        button.setForegroundPainter(painterFactory.createMiniButtonForegroundPainter());
        button.setBackgroundPainter(painterFactory.createMiniButtonBackgroundPainter());
        
        decorateGeneral(button);
        
        button.setBorder(BorderFactory.createEmptyBorder(2,6,2,15));
        button.setForeground(miniTextForeground);
        button.setFont(miniTextFont);
    }
    
    public void decorateDarkFullButton(JXButton button) {
        decorateDarkFullButton(button, DrawMode.FULLY_ROUNDED);
    }
    
    public void decorateDarkFullImageButton(JXButton button, DrawMode mode) {
        decorateDarkFullButton(button, button.getForegroundPainter(), 
                painterFactory.createDarkFullButtonBackgroundPainter(mode));
        
        button.setContentAreaFilled(false);
        button.setPaintBorderInsets(true);
    }
    
    public void decorateDarkFullButton(JXButton button, DrawMode mode) {
        decorateDarkFullButton(button, painterFactory.createDarkFullButtonForegroundPainter(), 
                painterFactory.createDarkFullButtonBackgroundPainter(mode));
        
        button.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
    }
    
    private void decorateDarkFullButton(JXButton button,
            Painter<JXButton> foregroundPainter, Painter<JXButton> backgroundPainter) {
        
        button.setForegroundPainter(foregroundPainter);
        button.setBackgroundPainter(backgroundPainter);
        
        decorateGeneral(button);
        
        button.setForeground(darkFullTextForeground);
        button.setFont(darkFullTextFont);
    }
    
    public void decorateLightFullButton(JXButton button) {
        button.setForegroundPainter(painterFactory.createLightFullButtonForegroundPainter());
        button.setBackgroundPainter(painterFactory.createLightFullButtonBackgroundPainter());

        decorateGeneral(button);
        
        button.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        button.setForeground(lightFullTextForeground);
        button.setFont(lightFullTextFont);
    }
        
    private static void decorateGeneral(JXButton button) {
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
}
