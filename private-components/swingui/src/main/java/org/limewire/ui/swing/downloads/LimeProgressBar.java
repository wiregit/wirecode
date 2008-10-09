package org.limewire.ui.swing.downloads;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JProgressBar;

/**
 * Grayed out when setEnabled(false)
 */
public class LimeProgressBar extends JProgressBar {
    
    // TODO: convert to resources
    private Color barBackgroundGradientTop = new Color(0xe4,0xe4,0xe4);
    private Color barBackgroundGradientBottom = new Color(0xc4,0xc4,0xc4);
    private Color barForegroundGradientTop = new Color(0x16,0x68,0xa8);
    private Color barForegroundGradientBottom = new Color(0x22,0x50,0xa4);
    
	private static final Composite DISABLED_COMPOSITE = AlphaComposite
			.getInstance(AlphaComposite.SRC_ATOP).derive(.3f);
	
	private boolean isHidden = false;
	
	public LimeProgressBar(int min, int max){
	    super(min, max);
	}
	
	public LimeProgressBar(){
        super();
    }

	@Override
	protected void paintComponent(Graphics g) {
		if (!isHidden) {
			if (!isEnabled()) {
				Graphics2D g2 = (Graphics2D) g;
				Composite oldComposite = g2.getComposite();
				g2.setComposite(DISABLED_COMPOSITE);
				super.paintComponent(g);
				g2.setComposite(oldComposite);
			} else {
			    Graphics2D g2 = (Graphics2D) g;
			    int height = this.getHeight();
			    int width = this.getWidth();
			   

			    int progress = (int) (width * this.getPercentComplete());
			    
			    g2.setPaint(new GradientPaint(0,1, barBackgroundGradientTop, 0, height-2, barBackgroundGradientBottom));
			    g2.fillRect(progress, 0, width-progress, height);
			    
			    g2.setPaint(new GradientPaint(0,1, barForegroundGradientTop, 0, height-2, barForegroundGradientBottom));
			    g2.fillRect(0, 0, progress, height);
			    
			}
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
