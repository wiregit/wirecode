package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.graphics.GraphicsUtilities;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.ImageFileItem;
import org.limewire.ui.swing.util.ImageScaler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;

/**
 *	Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. 
 */
public class ImageList extends JXList {
   
    public ImageList(EventList<FileItem> eventList) {
        super(new EventListModel<FileItem>(eventList));
        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);
        setCellRenderer(new ImageCellRenderer());
    }
    
    private class ImageCellRenderer extends ImageLabel implements ListCellRenderer {

        public ImageCellRenderer() {
            super(4);
            setOpaque(true);
            setBorder(new EmptyBorder(20,20,20,20));
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

             FileItem item = (FileItem)value;
             ImageIcon imageIcon = (ImageIcon) item.getProperty("Image");
             if(imageIcon != null)
                 setIcon(imageIcon);
             else {
                 //TODO: this is really bad, but doing this to fix the build
                 //         will move to this on a background thread 
                 Image image = createImage(item.getFile());
                 ImageIcon icon = new ImageIcon(image);
                 item.setProperty("Image", icon);
                 setIcon(icon);
             }
            
            this.setBackground(isSelected ? Color.BLUE : Color.WHITE);
            this.setForeground(isSelected ? Color.WHITE : Color.WHITE );
            
            return this;
        }
        
        private Image createImage(File file) {
            BufferedImage image = null;
            try { 
                image = GraphicsUtilities.loadCompatibleImage(file.toURI().toURL());
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
            return image;
        }
        
    }
}
