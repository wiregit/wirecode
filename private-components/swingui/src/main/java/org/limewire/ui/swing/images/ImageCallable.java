package org.limewire.ui.swing.images;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.ImageLocalFileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.GraphicsUtilities;
import org.limewire.ui.swing.util.SwingUtils;

/**
 * Loads an image, creates a thumbnail of it and stores it as a property
 * in a FileItem. This should all be executed on the ImageExecutorService.
 */
public class ImageCallable implements Callable<Void> {
    
    private final JList list;
    private LocalFileItem fileItem;
    private Icon errorIcon;
    
    public ImageCallable(JList list, LocalFileItem fileItem, Icon errorIcon) {
        this.list = list;
        this.fileItem = fileItem;
        this.errorIcon = errorIcon;
    }
    
    @Override
    public Void call() throws Exception {
        BufferedImage image = null;
        try {  
            URL url = fileItem.getFile().toURI().toURL();
            image = GraphicsUtilities.loadCompatibleImage(url);
        } catch (MalformedURLException e) {
            setProperty(errorIcon);
            return null;
        } catch(IOException exception) {
            setProperty(errorIcon);
            return null;
        } catch (Exception e) { 
            setProperty(errorIcon);
            return null;
        }  
        if(image == null) {
            setProperty(errorIcon);
            return null;
        } 
        // if the image is larger than our viewport, resize the image before saving
        if(image.getWidth() > ImageLocalFileItem.WIDTH || image.getHeight() > ImageLocalFileItem.HEIGHT) {
            //TODO: this can be optimized for pictures within one step away from the target size
            //TODO: this seems to fail regularly if width > 2 * height or height > 2 * width
            image = GraphicsUtilities.createThumbnail(image, ImageLocalFileItem.HEIGHT);
        }
        ImageIcon imageIcon = new ImageIcon(image);
        setProperty(imageIcon);
        
        SwingUtils.invokeLater(new Runnable(){
            public void run(){
                list.repaint();                
            }
        });
        
        return null;
    }

    /**
     * Saves the thumbnail in the fileItem as a property.
     * @param icon
     */
    private void setProperty(Icon icon) {
        fileItem.setProperty(FileItem.Keys.IMAGE, icon);
    }
}
