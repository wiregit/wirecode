package org.limewire.ui.swing.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.util.GraphicsUtilities;

/**
 * Loads an image and creates a thumbnail of it on the ImageExceturService thread. 
 * If an error occurs while reading the file and error Image will be loaded in
 * its place. If a component is passed in, it will be treated as a callback to
 * refresh the component after the thumbnail has been created.
 * 
 * If a component is passed in expecting a callback, the component must still be
 * visible when the thumbnail is set to be created. If the component is a list
 * that is expecting a callback, the index for the thumbnail must still be 
 * visible prior to the thumbnail being loaded.
 */
public class ThumbnailCallable implements Callable<Void> {
    
    private final Map<File,Icon> thumbnailMap;
    private final Map<File,String> loadingMap;
    private File file;
    private final Icon errorIcon;
    private final JComponent callback;
    
    private int index = -1;
    
    /**
     * Reads the image file and create a thumbnail, storing the thumbnail
     * in the thumbnail map. If an error occurs, store the error icon instead.
     * 
     * @param thumbnailMap - map to store the thumbnail in
     * @param loadingMap - map of files waiting to be loaded as thumbnails
     * @param file - image file to read and create a thumbnail from
     * @param errorIcon - icon to show if the file can't be read
     */
    public ThumbnailCallable(Map<File,Icon> thumbnailMap, Map<File,String> loadingMap, File file, Icon errorIcon) {
        this(thumbnailMap, loadingMap, file, errorIcon, null);
    }
    
    /**
     * Reads the image file and create a thumbnail, storing the thumbnail
     * in the thumbnail map. If an error occurs, store the error icon instead.
     * Call a repaint on the component once the thumbnail has been created. If
     * the component is no longer showing don't bother repainting it.
     * 
     * If the callback is no longer visible when the load method is created, the
     * thumbnail is not created. 
     * 
     * @param thumbnailMap - map to store the thumbnail in
     * @param loadingMap - map of files waiting to be loaded as thumbnails
     * @param file - image file to read and create a thumbnail from
     * @param errorIcon - icon to show if the file can't be read
     * @param callback - component to repaint once the thumbnail has been created.
     */
    public ThumbnailCallable(Map<File,Icon> thumbnailMap, Map<File,String> loadingMap, File file, Icon errorIcon, JComponent callback) {
        this.thumbnailMap = thumbnailMap;
        this.loadingMap = loadingMap;
        this.file = file;
        this.errorIcon = errorIcon;
        this.callback = callback;
    }
    
    /**
     * Reads the image file and create a thumbnail, storing the thumbnail
     * in the thumbnail map. If an error occurs, store the error icon instead.
     * Call a repaint on the component once the thumbnail has been created. If
     * the component is no longer showing don't bother repainting it.
     * 
     * If the callback is no longer visible when the load method is created, the
     * thumbnail is not created. If the list is still visible but the index is 
     * no longer shown in the list, the image is not loaded. 
     * 
     * @param thumbnailMap - map to store the thumbnail in
     * @param loadingMap - map of files waiting to be loaded as thumbnails
     * @param file - image file to read and create a thumbnail from
     * @param errorIcon - icon to show if the file can't be read
     * @param callback - component to repaint once the thumbnail has been created.
     * @param index - index within this list, this thumbnail is intended for
     */
    public ThumbnailCallable(Map<File,Icon> thumbnailMap, Map<File,String> loadingMap, File file, Icon errorIcon, JList list, int index) {
        this.thumbnailMap = thumbnailMap;
        this.loadingMap = loadingMap;
        this.file = file;
        this.errorIcon = errorIcon;
        this.callback = list;
        this.index = index;
    }
    
    @Override
    public Void call() throws Exception {
        // do some testing. If the component this thumbnail is intended for isn't showing anymore,
        // don't waste the time loading the thumbnail
        if(callback != null) {
            if(!callback.isShowing()) {
                // thumbnail wasn't loaded, remove it from the list of pending thumbnails
                loadingMap.remove(file);
                return null;
            }
            if((callback instanceof JList) && index != -1 && (((JList)callback).getFirstVisibleIndex() > index || ((JList)callback).getLastVisibleIndex() < index)){
                // thumbnail wasn't loaded, remove it from the list of pending thumbnails
                loadingMap.remove(file);
                return null;
            }
        }
        BufferedImage image = null;
        try {  
            image = ImageIO.read(file);
        } catch (Throwable e) {
            handleUpdate(errorIcon);
            return null;
        }  
        if(image == null) {
            handleUpdate(errorIcon);
            return null;
        }
        // if the image is larger than our viewport, resize the image before saving
        if(image.getWidth() > ThumbnailManager.WIDTH || image.getHeight() > ThumbnailManager.HEIGHT) { 
            //TODO: this seems to fail regularly if width > 2 * height or height > 2 * width
            // image manipulation can cause a whole host of errors, it should always be wrapped in a try/catch block
            try { 
                image = GraphicsUtilities.createRatioPreservedThumbnail(image, ThumbnailManager.WIDTH, ThumbnailManager.HEIGHT);
            } catch(Throwable t) {
                try { // if there was an error, try creating a less detailed thumbnail
                    image = GraphicsUtilities.createRatioPreservedThumbnailFast(image, ThumbnailManager.WIDTH, ThumbnailManager.HEIGHT);
                } catch (Throwable e) { // give up
                    image = null;
                }
            }
        } else {
            // if the image didn't need to be scaled, make sure it can be accelerated
            image = GraphicsUtilities.toCompatibleImage(image);
        }
        
        if(image == null || image.getWidth() == 0 || image.getHeight() == 0) {
            handleUpdate(errorIcon);
            return null;
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
        loadingMap.remove(file);
        if(callback != null && callback.isShowing()) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    callback.repaint();                    
                }
            });
        }
    }
}
