package org.limewire.ui.swing.util;

import java.awt.Dimension;

import javax.swing.BoundedRangeModel;
import javax.swing.JProgressBar;

/**
 * A JProgressBar that doesn't NPE when retrieving the preferredSize.
 * See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337517
 * (If the user has a custom XP Skin, it'll throw an NPE.)
 */
public class LimeJProgressBar extends JProgressBar {

	private final static Dimension PREFERRED_HORIZONTAL_SIZE = new Dimension(146, 17);  
	
    public LimeJProgressBar() {
        super();
    }

    public LimeJProgressBar(int orient) {
        super(orient);
    }

    public LimeJProgressBar(int min, int max) {
        super(min, max);
    }

    public LimeJProgressBar(int orient, int min, int max) {
        super(orient, min, max);
    }

    public LimeJProgressBar(BoundedRangeModel newModel) {
        super(newModel);
    }

    @Override
    public Dimension getMaximumSize() {
        try {
            return super.getMaximumSize();
        } catch (NullPointerException e) {
        	Dimension d;
        	if (getOrientation() == JProgressBar.HORIZONTAL) {
        		d = new Dimension(Short.MAX_VALUE, PREFERRED_HORIZONTAL_SIZE.height);
        	} else {
        		d = new Dimension(PREFERRED_HORIZONTAL_SIZE.width, Short.MAX_VALUE);
        	}
        	return d;
        }
    }
    
    @Override
    public Dimension getMinimumSize() {
        try {
            return super.getMinimumSize();
        } catch (NullPointerException npe) {
        	Dimension d;
        	if (getOrientation() == JProgressBar.HORIZONTAL) {
        		d = new Dimension(10, PREFERRED_HORIZONTAL_SIZE.height);
        	} else {
        		d = new Dimension(PREFERRED_HORIZONTAL_SIZE.width, 10);
        	}
        	return d;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        try {
            return super.getPreferredSize();
        } catch(NullPointerException npe) {
        	Dimension d;
        	if (getOrientation() == JProgressBar.HORIZONTAL) {
        		d = PREFERRED_HORIZONTAL_SIZE;
        	} else {
        		d = new Dimension(PREFERRED_HORIZONTAL_SIZE.height, PREFERRED_HORIZONTAL_SIZE.width);
        	}
            return d;
        }
    }

}
