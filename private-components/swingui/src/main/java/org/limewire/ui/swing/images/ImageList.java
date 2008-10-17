package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

/**
 *	Draws a list of images. Images are displayed in a horizontal left
 *  to right space before wrapping to a new line. Spaces between the
 *  images are injected with the inset values list below.
 */
public class ImageList extends JXList {

    @Resource
    private Color backgroundListcolor;
    @Resource
    private int imageBoxWidth;
    @Resource
    private int imageBoxHeight;
    @Resource
    private int insetTop;
    @Resource
    private int insetBottom;
    @Resource
    private int insetLeft;
    @Resource
    private int insetRight;
    
    private ImageCellRenderer renderer;
    
    public ImageList(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        super(new ImageListModel(eventList, fileList));

        GuiUtils.assignResources(this); 
        
        setBackground(backgroundListcolor);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        //this must be set to negative 1 to get horizontal line wrap
        setVisibleRowCount(-1);

        // add in inset size when calculated fixed cell dimensions
        // inset spacing is the white space you will see between images
        setFixedCellHeight(imageBoxHeight + insetTop + insetBottom);
        setFixedCellWidth(imageBoxWidth + insetRight + insetLeft);
    }
    
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
    
    /**
     * Sets the renderer on this List. The renderer is wrapped in an empty
     * border to create white space between the cells.
     */
    public void setImageCellRenderer(ImageCellRenderer renderer) {
        this.renderer = renderer;
        renderer.setInsets(BorderFactory.createEmptyBorder(insetTop,insetLeft,insetBottom,insetRight));
        
        super.setCellRenderer(renderer);
    }
    
    /**
     * Returns the CellRenderer for this list
     */
    public ImageCellRenderer getImageCellRenderer() {
        return renderer;
    }
}
