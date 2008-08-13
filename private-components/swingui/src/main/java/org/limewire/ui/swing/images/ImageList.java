package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.ImageFileItem;

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

            if(value instanceof ImageFileItem) {
                ImageFileItem item = (ImageFileItem) value;
                if(item.getThumbnail() != null)
                    setIcon(new ImageIcon(item.getThumbnail()));
            } else 
                setText((String) value);
            
            this.setBackground(isSelected ? Color.BLUE : Color.WHITE);
            this.setForeground(isSelected ? Color.WHITE : Color.WHITE );
            
            return this;
        }
        
    }
}
