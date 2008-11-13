package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

public class ProgressBarBackgroundPainter extends AbstractPainter<JProgressBar> {

    
    @Resource private Color border;
    @Resource private Color borderDisabled;
    
    @Resource private Color barBackgroundGradientTop;
    @Resource private Color barBackgroundGradientBottom;
    
    private GradientPaint gradientBackground;
    
    private int heightCache = 0;
    
    public ProgressBarBackgroundPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JProgressBar object, int width, int height) {
        if (height != this.heightCache) {
            this.heightCache = height;
            
            this.gradientBackground = new GradientPaint(0, 0, barBackgroundGradientTop, 0, height-1, barBackgroundGradientBottom);
        }
        
        g.setPaint(this.gradientBackground);
        g.fillRect(0, 0, width-1, height-1);        
        
        g.setColor(object.isEnabled() ? border : borderDisabled);
        g.drawRect(0, 0, width-1, height-1);
    }

}
