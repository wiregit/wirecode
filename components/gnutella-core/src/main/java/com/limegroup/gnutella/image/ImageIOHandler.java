package com.limegroup.gnutella.image;

import java.awt.Image;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import javax.imageio.ImageIO;

import com.limegroup.gnutella.util.FileUtils;

/**
 * An ImageHandler that uses ImageIO methods.  Useful
 * for when using Java 1.4 or greater.
 */
class ImageIOHandler extends ImageHandler {
    
    /**
     * Reads an image from a file.
     */   
    public Image readImage(File f) throws IOException {
        return ImageIO.read(f);
    }
    
    /**
     * Writes the given image to an OutputStream, in JPG format.
     */
    public void write(Image i, OutputStream o) throws IOException {
        if(!ImageIO.write(getRenderedImage(i), "jpg", o))
            throw new IOException();
    }
    
    /**
     * Determines if this is an Image file that can be read.
     */
    public boolean isImageFile(File f) {
        String ext = FileUtils.getFileExtension(f);
        return ext != null && ImageIO.getImageReadersBySuffix(ext).hasNext();
    }
}