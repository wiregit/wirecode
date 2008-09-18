package org.limewire.ui.swing.images;

import java.io.File;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Manages thumbnails for image files. 
 */
public interface ThumbnailManager {

    /**
     * Returns the thumbnail for this file. If this file is not immediately 
     * available, it returns a place holder and lazily loads the image in
     * the background. Once the image has been loaded, subsequent requests 
     * for the image will return the thumbnail immediately.
     */
    public Icon getThumbnailForFile(File file);
    
    /**
	 * Returns the thumbnail for this file. If this file is not immediately 
     * available, it returns a place holder and lazily loads the image in
     * the background. Once the image has been loaded, subsequent requests 
     * for the image will return the thumbnail immediately.
     * 
     * Once the thumbnail has been loaded, if the component is still showing, 
     * it will cause a repaint at which time the correct image should replace
     * the place holder image.
	 */
    public Icon getThumbnailForFile(File file, JComponent callback);
    
    /**
     * Returns true if the thumbnail is available immediately.
     */
    public boolean isThumbnailForFileAvailable(File file);
}
