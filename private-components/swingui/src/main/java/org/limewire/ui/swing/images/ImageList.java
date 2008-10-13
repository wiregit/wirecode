package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
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
    private Color selectionCellColor;
    @Resource
    private Color nonSelectionCellColor;   
    @Resource
    private Color backgroundListcolor;
    
    private static final int selectionBorderWidth = 4;
    private static final int insetCellSize = 20;
    
    private ThumbnailManager thumbnailManager;
    
    public ImageList(EventList<LocalFileItem> eventList, LocalFileList fileList, ThumbnailManager thumbnailManager) {
        super(new ImageListModel(eventList, fileList));

        GuiUtils.assignResources(this); 
        
        this.thumbnailManager = thumbnailManager;
        
        setBackground(backgroundListcolor);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);
        setCellRenderer(new ImageCellRenderer());
        setFixedCellHeight(ThumbnailManager.HEIGHT + insetCellSize + insetCellSize);
        setFixedCellWidth(ThumbnailManager.WIDTH + insetCellSize + insetCellSize);
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
            setIcon(thumbnailManager.getThumbnailForFile(item.getFile(), list, index));
            
            this.setBackground(isSelected ? selectionCellColor : nonSelectionCellColor);
            this.setForeground(list.getBackground());
            
            return this;
        }
    }
}
