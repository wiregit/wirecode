package com.limegroup.gnutella.image;

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

/**
 * An abstract implementation of an ImageHandler.
 */
public abstract class ImageHandler implements ImageObserver {
 
    /**
     * Reads an image from a file.
     */   
    public abstract Image readImage(File f) throws IOException;
    
    /**
     * Writes the given image to an OutputStream, in JPG format.
     */
    public abstract void write(Image i, OutputStream o) throws IOException;
    
    /**
     * Determines if this is an image file that can be read.
     */
    public abstract boolean isImageFile(File f);
    
    /**
     * Gets a RenderedImage from a given Image.
     */
    public RenderedImage getRenderedImage(Image i) {
        if(i instanceof RenderedImage) {
            return (RenderedImage)i;
        } else {
            int width = i.getWidth(this);
            int height = i.getHeight(this);
			BufferedImage bi =
			    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bi.createGraphics();
			g.drawImage(i, 0, 0, null);
			g.dispose();
			return bi;
        }
    }

    /**
     * Gets a BufferedImage from the given image
     */
    public BufferedImage getBufferedImage(Image i) {
        if(i instanceof BufferedImage) {
            return (BufferedImage)i;
        } else {
            int width = i.getWidth(this);
            int height = i.getHeight(this);
			BufferedImage bi =
			    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D biContext = bi.createGraphics();
			biContext.drawImage(i, 0, 0, null);
			biContext.dispose();
			return bi;
        }
    }
                
    
    /**
     * Resizes the given image, as a RenderedImage, to the given
     * width & height.
     */
    public Image resize(Image i, int width, int height) {
        return i.getScaledInstance(width, height ,Image.SCALE_FAST);
    }   
    
    /**
     * Resizes the given image, as a RenderedImage, to a percentage
     * of the original width & height.
     */
    public Image resize(Image f, double pw, double ph) {
        int width = f.getWidth(this);
        int height = f.getHeight(this);
        return resize(f, width * pw, height * ph);
    }
    
    /**
     * Resizes the given image, as a RenderedImage, to the given percentage
     * of the original width & height.
     */
    public Image resize(Image i, double percent) {
        return resize(i, percent, percent);
    }
    
    /**
     * Writes the given image to a byte[], in JPG format.
     */
    public byte[] write(Image i) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            write(i, bo);
            return bo.toByteArray();
        } catch(IOException errorWriting) {
            return null;
        }
    }
    
    /**
     * Writes the given image to a file, in JPG format.
     */
    public void write(Image i, File f) throws IOException {
        OutputStream o = null;
        try {
            o = new BufferedOutputStream(new FileOutputStream(f));
            write(i, o);
        } finally {
            if(o != null) {
                try {
                    o.close();
                } catch(IOException ignored) {}
            }
        }
    }
    
    /**
     * ImageObserver callback.
     */
    public boolean imageUpdate(Image img,
                               int infoflags,
                               int x,
                               int y,
                               int width,
                               int height) { return false; }
    
}