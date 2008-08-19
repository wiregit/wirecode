package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;

/**
 *	Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. 
 */
public class ImageList extends JXList {
   
    @Resource
    private Icon loadIcon;
   
    @Resource
    private Icon errorIcon;
    
    public ImageList(EventList<FileItem> eventList) { 
        super(new EventListModel<FileItem>(eventList)); 

        GuiUtils.assignResources(this); 
        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);
        setCellRenderer(new ImageCellRenderer());
        setFixedCellHeight(190);
        setFixedCellWidth(190);
        setRolloverEnabled(true);
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
            if(imageIcon != null) {
                setIcon(imageIcon);
            } else {
                setIcon(loadIcon);
                item.setProperty("Image", loadIcon);
                (new ImageLoader(list, item, errorIcon)).execute();
            }
            
            this.setBackground(isSelected ? Color.BLUE : Color.WHITE);
            this.setForeground(isSelected ? Color.WHITE : Color.WHITE );
            
            return this;
        }
    }
}
