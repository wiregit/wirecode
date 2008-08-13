package org.limewire.ui.swing.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.jdesktop.swingx.graphics.GraphicsUtilities;

/**
 * Progressively scales up or down and image creating a scaled compatible image
 * with the graphics card. 
 */
public class ImageScaler {
    
    /**
     * Scales an image but preserves the aspect ration of the image so as to
     * not distort it.
     */
    public static BufferedImage getRatioPreservedScaledImage(BufferedImage image,
            int targetWidth, int targetHeight, Object hint, boolean scaleProgressively) {
        
        if(image.getWidth() > image.getHeight()) {
            targetHeight = (image.getHeight() * targetWidth)/image.getWidth();
        } else if(image.getHeight() > image.getWidth()) {
            targetWidth = (image.getWidth() * targetHeight)/image.getHeight();
        } 
        
        return getScaledImage(image, targetWidth, targetHeight, hint, scaleProgressively);
    }
    
    /**
     * Creates a scaled image that is compatible with the graphics card. The
     * image may be scaled in one step or progressively. By progressively scaling 
     * an image you can typically create a better quality image, especially when
     * the target size is dramatically different than the original image size.
     * 
     * @param image - the image to scale
     * @param targetWidth - width of new image
     * @param targetHeight - height of new image
     * @param hint - the method by which to scale. Typically there is trade
     * off on quality versus speed when using a rendering option:
     *           RenderingHints.VALUE_INTERPOLATION_BILINEAR
     *           RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
     *           RenderingHints.VALUE_INTERPOLATION_BICUBIC
     * @param scaleProgressively - if true, scales progressively over a number of
     *          steps, if false, scales the image in one step using the rendering hints provided.
     */
    public static BufferedImage getScaledImage(BufferedImage image,
            int targetWidth, int targetHeight, Object hint, boolean scaleProgressively) {

        BufferedImage currentImage = image;
        BufferedImage scratchImage = null;
        Graphics2D g2d = null;
        int width;
        int height;
        
        int prevWidth = currentImage.getWidth();
        int prevHeight = currentImage.getHeight();
        
        if(scaleProgressively) {
            width = image.getWidth();
            height = image.getHeight();
        } else {
            width = targetWidth;
            height = targetHeight;
        }
        
        do {
            if(scaleProgressively && width > targetWidth) {
                width /= 2;
                if( width < targetWidth) 
                    width = targetWidth;
            }
            
            if(scaleProgressively && height > targetHeight) {
                height /= 2;
                if(height < targetHeight)
                    height = targetHeight;
            }
            
            if(scratchImage == null)
                scratchImage = GraphicsUtilities.createCompatibleImage(currentImage, width, height);

            if( g2d == null)
                g2d = scratchImage.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2d.drawImage(currentImage, 0, 0, width, height, 
                    0, 0, prevWidth, prevHeight, null);
            
            prevWidth  = width;
            prevHeight = height;
        
            currentImage = scratchImage;
        } while(width != targetWidth && height != targetHeight);
        
        g2d.dispose();
        
        if(targetWidth != currentImage.getWidth() || targetHeight != currentImage.getHeight()) {
            scratchImage = GraphicsUtilities.createCompatibleImage(currentImage, targetWidth, targetHeight);
        
            g2d = scratchImage.createGraphics();
            g2d.drawImage(currentImage, 0, 0, null);
            g2d.dispose();
            currentImage = scratchImage;
        }
        return currentImage;
    }
}