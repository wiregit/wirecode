package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.painter.ProgressBarBackgroundPainter;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;

/**
 * Grayed out when setEnabled(false)
 */
public class LimeProgressBar extends JProgressBar {
    

    private static Painter<JProgressBar> BACKGROUND_PAINTER = new ProgressBarBackgroundPainter();
    private static Painter<JProgressBar> FOREGROUND_PAINTER = new ProgressBarForegroundPainter();
    
    private Painter<JProgressBar> foregroundPainter = FOREGROUND_PAINTER;
    private Painter<JProgressBar> backgroundPainter = BACKGROUND_PAINTER;
    
	private boolean isHidden = false;
	
	public LimeProgressBar(int min, int max){
	    super(min, max);
	    
	    init();
	}
	
	public LimeProgressBar(){
	    
	    init();
    }

	private void init() {
	    this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	}
	 
	public void setForegroundPainter(Painter<JProgressBar> painter) {
	    this.foregroundPainter = painter;
	}
	
	public void setBackgroundPainter(Painter<JProgressBar> painter) {
        this.backgroundPainter = painter;
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
