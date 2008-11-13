package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;

public class ProgressBarForegroundPainter extends AbstractPainter<JProgressBar> {

    @Resource private Color barForegroundGradientTop;
    @Resource private Color barForegroundGradientBottom;
    @Resource private Color barDisabledForegroundGradientTop;
    @Resource private Color barDisabledForegroundGradientBottom;
    
    private GradientPaint gradientForeground;
    private GradientPaint gradientDisabledForeground;

    private int heightCache = 0;
    
    public ProgressBarForegroundPainter() {
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JProgressBar object, int width, int height) {
        if (height != this.heightCache) {
            this.heightCache = height;
            
            this.gradientForeground = new GradientPaint(0,1, barForegroundGradientTop, 0, height-2, barForegroundGradientBottom);
            this.gradientDisabledForeground = new GradientPaint(0,1, barDisabledForegroundGradientTop,
                    0, height-2, barDisabledForegroundGradientBottom);
        }
        
        int progress = (int) (width * object.getPercentComplete());
                
        if (object.isEnabled()) {
            g.setPaint(this.gradientForeground);
        } 
        else {
            g.setPaint(this.gradientDisabledForeground);
        }
        
        g.fillRect(1, 1, progress-2, height-2);
        
    }

}
