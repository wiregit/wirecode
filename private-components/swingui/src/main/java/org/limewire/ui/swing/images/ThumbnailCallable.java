package org.limewire.ui.swing.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.limewire.core.api.library.ImageLocalFileItem;
import org.limewire.ui.swing.util.GraphicsUtilities;

/**
 * Loads an image and creates a thumbnail of it on the ImageExceturService thread. 
 * If an error occurs while reading the file and error Image will be loaded in
 * its place. If a component is passed in, it will be treated as a callback to
 * refresh the component after the thumbnail has been created.
 */
public class ThumbnailCallable implements Callable<Void> {
    
    private final Map<File,Icon> thumbnailMap;
    private File file;
    private final Icon errorIcon;
    private final JComponent callback;
    
    /**
     * Reads the image file and create a thumbnail, storing the thumbnail
     * in the thumbnail map. If an error occurs, store the error icon instead.
     * 
     * @param thumbnailMap - map to store the thumbnail in
     * @param file - image file to read and create a thumbnail from
     * @param errorIcon - icon to show if the file can't be read
     */
    public ThumbnailCallable(Map<File,Icon> thumbnailMap, File file, Icon errorIcon) {
        this(thumbnailMap, file, errorIcon, null);
    }
    
    /**
     * Reads the image file and create a thumbnail, storing the thumbnail
     * in the thumbnail map. If an error occurs, store the error icon instead.
     * Call a repaint on the component once the thumbnail has been created. If
     * the component is no longer showing don't bother repainting it.
     * 
     * @param thumbnailMap - map to store the thumbnail in
     * @param file - image file to read and create a thumbnail from
     * @param errorIcon - icon to show if the file can't be read
     * @param callback - component to repaint once the thumbnail has been created.
     */
    public ThumbnailCallable(Map<File,Icon> thumbnailMap, File file, Icon errorIcon, JComponent callback) {
        this.thumbnailMap = thumbnailMap;
        this.file = file;
        this.errorIcon = errorIcon;
        this.callback = callback;
    }
    
    @Override
    public Void call() throws Exception {
        BufferedImage image = null;
        try {  
            URL url = file.toURI().toURL();
            image = GraphicsUtilities.loadCompatibleImage(url);
        } catch (Throwable e) { 
            handleUpdate(errorIcon);
            return null;
        }  
        if(image == null) {
            handleUpdate(errorIcon);
            return null;
        } 

        // if the image is larger than our viewport, resize the image before saving
        if(image.getWidth() > ImageLocalFileItem.WIDTH || image.getHeight() > ImageLocalFileItem.HEIGHT) {
            //TODO: this can be optimized for pictures within one step away from the target size
            //TODO: this seems to fail regularly if width > 2 * height or height > 2 * width
            image = GraphicsUtilities.createRatioPreservedThumbnail(image, ImageLocalFileItem.WIDTH, ImageLocalFileItem.HEIGHT);
        }
        ImageIcon imageIcon = new ImageIcon(image);
        handleUpdate(imageIcon);
        
        return null;
    }
    
    /**
     * Store the image in a hashmap. If a component callback was passed in, 
     * see if the component is still showing and if so call a repaint on it.
     */
    private void handleUpdate(Icon icon) {
        thumbnailMap.put(file, icon);
        if(callback != null && callback.isShowing()) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    callback.repaint();                    
                }
            });
        }
    }
}
