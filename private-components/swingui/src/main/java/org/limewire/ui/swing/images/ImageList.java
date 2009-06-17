package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.image.ImageCellRenderer;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;

import com.google.inject.Inject;

/**
 *  Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. Spaces between the
 *  images are injected with the inset values list below.
 */
public class ImageList extends JXList {// implements Disposable {//, SelectAllable<LocalFileItem> {

    @Resource
    private Color backgroundListcolor;
    
//    private ImageCellRenderer renderer;
//    private ImageListModel imageListModel;
//    private EventList<LocalFileItem> listSelection;
    
    @Inject
    public ImageList(final ImageCellRenderer imageCellRenderer) {

        GuiUtils.assignResources(this); 
        
        setBackground(backgroundListcolor);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);

        setCellRenderer(imageCellRenderer);
        //TODO: fix this, component dimensions not beign created yet without setting this
        imageCellRenderer.setBorder(BorderFactory.createEmptyBorder(15,7,0,7));
        // add in inset size when calculated fixed cell dimensions
        // inset spacing is the white space you will see between images
//        System.out.println("width " + imageCellRenderer.getWidth() + " " + imageCellRenderer.getBounds() + " " + imageCellRenderer.getBounds());
        Insets insets = imageCellRenderer.getBorder().getBorderInsets(imageCellRenderer);
        setFixedCellHeight(imageCellRenderer.getHeight() + insets.top + insets.bottom);
        setFixedCellWidth(imageCellRenderer.getWidth() + insets.left + insets.right);
        
        //enable double click launching of image files.
        addMouseListener(new ImageDoubleClickMouseListener());
//        EventSelectionModel<LocalFileItem> selectionModel = GlazedListsSwingFactory.eventSelectionModel(eventList);
//        this.listSelection = selectionModel.getSelected();
//        setSelectionModel(selectionModel);
//        selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
    }
    
    public void setModel(EventList<LocalFileItem> localFileList) {
        setModel(new EventListModel<LocalFileItem>(localFileList, false));//new ImageListModel(localFileList.getSwingModel(), localFileList));
    }
    
//    @Override
//    public ListSelectionModel getSelectionModel() {
//        return null;
//    }
    
//    /** Returns a copy of all selected items. */
//    @Override
//    public List<LocalFileItem> getSelectedItems() {
//        return new ArrayList<LocalFileItem>(listSelection);
//    }
//    
//    @Override
//    public List<LocalFileItem> getAllItems() {
//        return new ArrayList<LocalFileItem>( ((ImageListModel)getModel()).getFileList().getSwingModel());
//    }
//
//    @Override
//    public void selectAll() {
//        getSelectionModel().setSelectionInterval(0, getModel().getSize()-1);
//    }

//    @Override
//    public void dispose() {
//        ((ImageListModel)getModel()).dispose();
//        ((EventSelectionModel)getSelectionModel()).dispose();
//    }

//    @SuppressWarnings("unchecked")
//    public EventList<LocalFileItem> getListSelection() {
//        return ((EventSelectionModel<LocalFileItem>)getSelectionModel()).getSelected();
//    }
    
    /**
     * Sets the popup Handler for this List. 
     */
    public void setPopupHandler(final TablePopupHandler popupHandler) {
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
                    popupHandler.maybeShowPopup(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
//    /**
//     * Sets the renderer on this List. The renderer is wrapped in an empty
//     * border to create white space between the cells.
//     */
//    public void setImageCellRenderer(ImageCellRenderer renderer) {
//        this.renderer = renderer;
////        renderer.setBorder(BorderFactory.createEmptyBorder(insetTop,insetLeft,insetBottom,insetRight));
//        
//        super.setCellRenderer(renderer);
//    }
    
//    /**
//     * Returns the CellRenderer for this list.
//     */
//    public ImageCellRenderer getImageCellRenderer() {
//        return renderer;
//    }
    
    /**
     * This class listens for double clicks inside of the ImageList.
     * When a double click is detected, the relevant item in the list is launched.
     */
    private final class ImageDoubleClickMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                ImageList imageList = (ImageList)e.getComponent();
                int index = imageList.locationToIndex(e.getPoint());
                LocalFileItem val = (LocalFileItem) imageList.getElementAt(index);
                File file = val.getFile();
                NativeLaunchUtils.safeLaunchFile(file);
            }
        }
    }
}
