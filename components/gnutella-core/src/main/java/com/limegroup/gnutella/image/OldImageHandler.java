package com.limegroup.gnutella.image;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;

import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.image.codec.jpeg.JPEGCodec;

import com.limegroup.gnutella.util.FileUtils;

/**
 * An ImageHandler that uses Sun's internal JPEG codecs.
 * Useful for reading/writing/resizing images prior to Java 1.4.
 */
class OldImageHandler extends ImageHandler {
    
    /**
     * Reads an image from a file.
     */   
    public Image readImage(File f) throws IOException {
        Image i = Toolkit.getDefaultToolkit().createImage(f.getPath());
        if( i == null )
            throw new IOException();
        return i;
    }
    
    /**
     * Writes the given image to an OutputStream, in JPG format.
     */
    public void write(Image i, OutputStream o) throws IOException {
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(o);
        encoder.encode(getBufferedImage(i));
    }
    
    /**
     * Determines if this is an image file that can be read.
     */
    public boolean isImageFile(File f) {
        String ext = FileUtils.getFileExtension(f);
        if(ext == null)
            return false;
        ext = ext.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif");
    }
}