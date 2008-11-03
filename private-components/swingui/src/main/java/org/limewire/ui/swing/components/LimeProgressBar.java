package org.limewire.ui.swing.components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Grayed out when setEnabled(false)
 */
public class LimeProgressBar extends JProgressBar {
    
    @Resource private Color barBackgroundGradientTop;
    @Resource private Color barBackgroundGradientBottom;
    @Resource private Color barForegroundGradientTop;
    @Resource private Color barForegroundGradientBottom;
    
	private static final Composite DISABLED_COMPOSITE = AlphaComposite
			.getInstance(AlphaComposite.SRC_ATOP).derive(.3f);
	
	private boolean isHidden = false;
	
	public LimeProgressBar(int min, int max){
	    super(min, max);
	    
	    GuiUtils.assignResources(this);
	}
	
	public LimeProgressBar(){
	    GuiUtils.assignResources(this);        
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
