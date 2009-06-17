package org.limewire.ui.swing.library.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

public class LibraryImagePanel extends JPanel implements Scrollable {

    @Resource
    private Color backgroundColor;
    
    private final ImageList imageList;
    
    private  JXLayer<JComponent> layer;

    private EventList<LocalFileItem> model;
    
    @Inject
    public LibraryImagePanel(ImageList imageList) {
        super(new MigLayout("insets 0 0 0 0, fill"));
        GuiUtils.assignResources(this); 
        
        setBackground(backgroundColor);
        
        this.imageList = imageList;
        imageList.setBorder(BorderFactory.createEmptyBorder(0,7,0,7));
        JScrollPane imageScrollPane = new JScrollPane(imageList);
        imageScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        layer = new JXLayer<JComponent>(imageScrollPane, new  AbstractLayerUI<JComponent>());
        layer.getGlassPane().setLayout(null);
        
        add(layer, "grow");
    }
    
    public void setEventList(EventList<LocalFileItem> localFileList) {
        imageList.setModel(localFileList);
    }
//    public void setEventList(LocalFileList localFileList) {
//        imageList.setModel(localFileList);
//    }
    
    public void setPopupHandler(TablePopupHandler popupHandler) {
        imageList.setPopupHandler(popupHandler);
    }
    
    public void setImageEditor(TableRendererEditor editor) {
//        new MouseReaction(imageList, editor);
        layer.getGlassPane().add(editor);
    }
    
    /**
     * Overrides getPreferredSize. getPreferredSize is used by scrollable to 
     * determine how big to make the scrollableViewPort. Uses the parent 
     * component to set the appropriate size thats currently visible. This
     * prevents the image list from growing too wide and never shrinking again.
     */
    @Override
    public Dimension getPreferredSize() {
        //ensure viewport is filled so dnd will work
        Dimension dimension = super.getPreferredSize();
        if (getParent() == null)
            return dimension;
        if (dimension.height > getParent().getSize().height){
            return new Dimension(getParent().getWidth(), dimension.height);
        } else {
            return getParent().getSize(); 
        }
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }
    
    /**
     * Scrolls to the appropriate location based on the height of a thumbnail image.
     */
    private int getPosition(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL)
            currentPosition = visibleRect.x;
        else
            currentPosition = visibleRect.y;
    
        //TODO: read this from ImageList again
        int height = 134;//imageList.getList().getFixedCellHeight();
        
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / height) * height;
            return (newPosition == 0) ? height : newPosition;
        } else {
            return ((currentPosition / height) + 1) * height - currentPosition;
        }
    }
    
    public class MouseReaction implements MouseListener, MouseMotionListener {

        private ImageList imageList;
        private TableRendererEditor hoverComponent;
        
        public MouseReaction(ImageList imageList, TableRendererEditor hoverComponent) {
            this.imageList = imageList;          
            this.hoverComponent = hoverComponent;

            imageList.addMouseListener(this);
            imageList.addMouseMotionListener(this);
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            update(e.getPoint());
        }

        @Override
        public void mouseExited(MouseEvent e) { 
            if(!hoverComponent.getBounds().contains(e.getPoint())) {
                hoverComponent.setVisible(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int index = imageList.locationToIndex(e.getPoint());
            if(!e.isPopupTrigger() && !e.isShiftDown() && !e.isControlDown() && !e.isMetaDown() && index > -1)
                imageList.setSelectedIndex(index);
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}

        @Override
        public void mouseMoved(MouseEvent e) {
            update(e.getPoint());
        }
        
        private void update(Point point){
//            if (imageList.getModel().getSize() > 0) {
//                int index = imageList.locationToIndex(point);
//                if (index > -1) {
//                    hoverComponent.setVisible(true);
//                    ((Configurable)hoverComponent).configure((LocalFileItem) imageList.getModel().getElementAt(index), true);
//                    Rectangle bounds = imageList.getCellBounds(index, index);
//                    ImageCellRenderer renderer = imageList.getImageCellRenderer();
//                    hoverComponent.setLocation(bounds.x + renderer.getSubComponentLocation().x,
//                            bounds.y + renderer.getSubComponentLocation().y);
//                }
//            }
        }
    }
}
