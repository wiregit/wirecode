package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.NotImplementedException;


public class ProgressBarForegroundPainter<X extends JComponent> extends AbstractPainter<X> {
    
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
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        if (height != this.heightCache) {
            this.heightCache = height;
            
            this.foreground = PaintUtils.resizeGradient(this.foreground, 0, height-2);
            this.disabledForeground = PaintUtils.resizeGradient(this.disabledForeground, 0, height-2);
        }
        
        int progress = (int) (width * getPercentComplete(object));
                        
        if (object.isEnabled()) {
            g.setPaint(this.foreground);
        } 
        else {
            g.setPaint(this.disabledForeground);
        }
        
        g.fillRect(1, 1, progress-2, height-2);
        
    }
    
    /**
     * Avoiding using 2 painter classes for exactly the same function.
     *  Shortcut method to avoid using providers for now.  There
     *  is no nice way to do this anyways since in order to be consistent
     *  any provider MUST match the object passed into doPaint.  
     */
    private static double getPercentComplete(Object object) {
        if (object instanceof JProgressBar) {
            return ((JProgressBar)object).getPercentComplete();
        }
     
        if (object instanceof JSlider) {
            JSlider slider = (JSlider)object;
            
            return   (double)(slider.getValue()   - slider.getMinimum()) 
                   / (double)(slider.getMaximum() - slider.getMinimum()); 
        }
        
        throw new NotImplementedException("ProgressForegroundPainter" +
        		" applied to a not yet supported component");
    }
}
