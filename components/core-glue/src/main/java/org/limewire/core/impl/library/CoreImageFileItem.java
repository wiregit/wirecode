package org.limewire.core.impl.library;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

import org.jdesktop.swingx.graphics.GraphicsUtilities;
import org.limewire.core.api.library.ImageFileItem;
import org.limewire.ui.swing.util.ImageScaler;

import com.limegroup.gnutella.FileDesc;

public class CoreImageFileItem extends CoreFileItem implements ImageFileItem {

    private BufferedImage image;
    
    public CoreImageFileItem(FileDesc fileDesc) {
        super(fileDesc);
        loadImage();
    }

    @Override
    public Image getThumbnail() {
        return image;
    }

    //TODO: this needs to be done off this thread!
    private void loadImage() {
        try { 
            image = GraphicsUtilities.loadCompatibleImage(getFile().toURI().toURL());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        
        // if the image is larger than our viewport, resize the image before saving
        if( image != null && (image.getWidth() > ImageFileItem.HEIGHT || 
                image.getHeight() > ImageFileItem.HEIGHT) ) {
            image = ImageScaler.getRatioPreservedScaledImage(image, ImageFileItem.WIDTH, 
                  ImageFileItem.HEIGHT, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
        } 
    }
    
}
