package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;

public class ProgressBarForegroundPainter extends AbstractPainter<JProgressBar> {
    
    private Paint foreground;
    private Paint disabledForeground;

    private int heightCache = 0;
    
    public ProgressBarForegroundPainter(Paint foreground, Paint disabledForeground) {
        this.foreground = foreground;
        this.disabledForeground = disabledForeground;
        
        this.setAntialiasing(false);
        this.setCacheable(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JProgressBar object, int width, int height) {
        if (height != this.heightCache) {
            this.heightCache = height;
            
            this.foreground = PaintUtils.resizeGradient(this.foreground, 0, height-2);
            this.disabledForeground = PaintUtils.resizeGradient(this.disabledForeground, 0, height-2);
        }
        
        int progress = (int) (width * object.getPercentComplete());
                
        if (object.isEnabled()) {
            g.setPaint(this.foreground);
        } 
        else {
            g.setPaint(this.disabledForeground);
        }
        
        g.fillRect(1, 1, progress-2, height-2);
        
    }
}
