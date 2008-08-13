package org.limewire.ui.swing.images;

import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Draws an image, centered in the component. When selected a border of a 
 * specified width is drawn around the edges of the image.
 */
 //TODO: this should extend JComponent and paint the image directly rather than having to wrap it
 //		in an ImageIcon
public class ImageLabel extends JLabel {
    
    private int selectionBorderWidth;
    
    public ImageLabel() {
        this(3);
    }
    
    public ImageLabel(int selectedBorderWidth) {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        this.selectionBorderWidth = selectedBorderWidth;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if(isVisible()) {
            
           int rectWidth = getWidth() - getInsets().left - getInsets().right + selectionBorderWidth + selectionBorderWidth;
           int rectHeight = getHeight() - getInsets().top - getInsets().bottom + selectionBorderWidth + selectionBorderWidth;
                     
           g.setColor(getBackground());
           g.fillRect(getInsets().left - selectionBorderWidth, 
                   getInsets().top - selectionBorderWidth, rectWidth, rectHeight);
           g.setColor(getForeground());
           g.fillRect(getInsets().left, 
                   getInsets().top, 
                   getWidth() - getInsets().left - getInsets().right, 
                   getHeight() - getInsets().top - getInsets().bottom);           
           
           if(getIcon() != null) {
               int width = getWidth() - getIcon().getIconWidth();
               int height = getHeight() - getIcon().getIconHeight();
               
               getIcon().paintIcon(this, g, width/2, height/2);
           }
        }
    }
}