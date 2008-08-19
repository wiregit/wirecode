package org.limewire.ui.swing.images;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingWorker;


import org.jdesktop.swingx.graphics.GraphicsUtilities;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.ImageFileItem;
import org.limewire.ui.swing.util.ImageScaler;

/**
 * Lazily loads a thumbnail for a FileItem. Once the image is loaded its 
 * put into the property field of that FileItem so thumbnails are only 
 * created once. 
 */
public class ImageLoader extends SwingWorker<Icon, Object> {
    
    private final JList list;
    private final FileItem fileItem;
    private final Icon errorIcon;
    
    public ImageLoader(JList list, FileItem fileItem, Icon errorIcon) {
        this.list = list;
        this.fileItem = fileItem;
        this.errorIcon = errorIcon;
    }

    @Override
    protected Icon doInBackground() throws Exception {
        BufferedImage image = null;
        try { 
            image = GraphicsUtilities.loadCompatibleImage(fileItem.getFile().toURI().toURL());
        } catch (MalformedURLException e) {
            fileItem.setProperty("Image", errorIcon);
            return errorIcon;
        } catch (Exception e) {
            fileItem.setProperty("Image", errorIcon);
            return errorIcon;
        } 
        
        // if the image is larger than our viewport, resize the image before saving
        if( image != null && (image.getWidth() > ImageFileItem.HEIGHT || 
                image.getHeight() > ImageFileItem.HEIGHT) ) {
            image = ImageScaler.getRatioPreservedScaledImage(image, ImageFileItem.WIDTH, 
                  ImageFileItem.HEIGHT, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
        } 
        if(image == null) {
            fileItem.setProperty("Image", errorIcon);
            return errorIcon;
        }
        ImageIcon imageIcon = new ImageIcon(image);
        fileItem.setProperty("Image", imageIcon);
        return imageIcon;
    }
    
    @Override
    protected void done() {
        try {
            list.revalidate();
        } catch(Exception ignore) {
            
        }
    }

}
