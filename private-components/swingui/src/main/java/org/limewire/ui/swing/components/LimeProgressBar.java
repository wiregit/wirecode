package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.Painter;

public class LimeProgressBar extends JProgressBar {

    private static final boolean CACHING_SUPPORTED = true;
    
    private AbstractPainter<JProgressBar> foregroundPainter;
    private Painter<JComponent> backgroundPainter;
    
	private boolean isHidden = false;
	
	public LimeProgressBar(AbstractPainter<JProgressBar> foregroundPainter, 
	        Painter<JComponent> backgroundPainter) {
	    
	    this.foregroundPainter = foregroundPainter;
	    this.backgroundPainter = backgroundPainter;
	    
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    }
	 
	public void setForegroundPainter(AbstractPainter<JProgressBar> painter) {
	    this.foregroundPainter = painter;
	    painter.setCacheable(true);
	}
	
	public void setBackgroundPainter(Painter<JComponent> painter) {
        this.backgroundPainter = painter;
    }

	public boolean hasCacheSupport() {
	    return CACHING_SUPPORTED;
	}
	
	@Override 
	public void setValue(int v) {
	    if (this.getValue() != v) {
            this.foregroundPainter.clearCache();
        }
	    super.setValue(v);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
	    if (!isHidden) {
	        this.backgroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
	        this.foregroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
		}
	}
	
	/**
	 * Hides progress bar but maintains size and position in layout.
	 * 
	 * @param isHidden true to hide progress bar
	 */
	public void setHidden(boolean isHidden){
		this.isHidden = isHidden;
	}

}
