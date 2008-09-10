package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.sharing.menu.SharingActionHandler;
import org.limewire.ui.swing.sharing.menu.SharingPopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

/**
 *	Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. 
 */
public class ImageList extends JXList {
   
    @Resource
    private Icon loadIcon;
   
    @Resource
    private Icon errorIcon; 
    
    public ImageList(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        super(new ImageListModel(eventList, fileList));

        GuiUtils.assignResources(this); 
        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);
        setCellRenderer(new ImageCellRenderer());
        setFixedCellHeight(190);
        setFixedCellWidth(190);
        setRolloverEnabled(true);
        
        final SharingPopupHandler handler = new SharingPopupHandler(this, new SharingActionHandler());
        
        addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handler.maybeShowPopup(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
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

            LocalFileItem item = (LocalFileItem)value;
            ImageIcon imageIcon = (ImageIcon) item.getProperty(FileItem.Keys.IMAGE);
            if(imageIcon != null) {
                setIcon(imageIcon);
            } else {
                setIcon(loadIcon);
                item.setProperty(FileItem.Keys.IMAGE, loadIcon);
                (new ImageLoader(list, item, errorIcon)).execute();
            }
            
            this.setBackground(isSelected ? Color.BLUE : Color.WHITE);
            this.setForeground(isSelected ? Color.WHITE : Color.WHITE );
            
            return this;
        }
    }
}
