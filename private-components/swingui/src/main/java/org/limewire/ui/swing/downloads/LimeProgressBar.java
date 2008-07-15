package org.limewire.ui.swing.downloads;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JProgressBar;

/**
 * Grayed out when setEnabled(false)
 */
public class LimeProgressBar extends JProgressBar {
	private static final Composite DISABLED_COMPOSITE = AlphaComposite
			.getInstance(AlphaComposite.SRC_ATOP).derive(.3f);
	
	private boolean isHidden = false;

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
				super.paintComponent(g);
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
