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

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.ImageLocalFileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
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
    @Resource
    private Color selectionCellColor;
    @Resource
    private Color nonSelectionCellColor;   
    @Resource
    private Color backgroundListcolor;
    
    private static final int selectionBorderWidth = 4;
    private static final int insetCellSize = 20;
    
    public ImageList(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        super(new ImageListModel(eventList, fileList));

        GuiUtils.assignResources(this); 
        
        setBackground(backgroundListcolor);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);
        setCellRenderer(new ImageCellRenderer());
        setFixedCellHeight(ImageLocalFileItem.HEIGHT + insetCellSize + insetCellSize);
        setFixedCellWidth(ImageLocalFileItem.WIDTH + insetCellSize + insetCellSize);
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
            super(selectionBorderWidth, insetCellSize);
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            LocalFileItem item = (LocalFileItem)value;
            ImageIcon imageIcon = (ImageIcon) item.getProperty(FileItem.Keys.IMAGE);
            //TODO: there should be an image handler to lookup these values rather than
            // save them in a each FileItem.
            if(imageIcon != null) {
                setIcon(imageIcon);
            } else {
                setIcon(loadIcon);
                item.setProperty(FileItem.Keys.IMAGE, loadIcon);
                ImageExecutorService.submit(new ImageCallable(list,item,errorIcon));
            }
            
            this.setBackground(isSelected ? selectionCellColor : nonSelectionCellColor);
            this.setForeground(list.getBackground());
            
            return this;
        }
    }
}
