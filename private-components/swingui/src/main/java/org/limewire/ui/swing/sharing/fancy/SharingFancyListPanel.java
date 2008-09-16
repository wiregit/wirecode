package org.limewire.ui.swing.sharing.fancy;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.sharing.actions.SharingRemoveAllAction;
import org.limewire.ui.swing.sharing.actions.SharingRemoveListAction;
import org.limewire.ui.swing.sharing.components.ConfirmationUnshareButton;
import org.limewire.ui.swing.sharing.components.UnshareButton;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 *  Display images in a list below a title and line
 */
//TODO: merge this with SharingFancyTablePanel during cleanup of Sharing package
public class SharingFancyListPanel extends JPanel implements ListEventListener<LocalFileItem> {
    
    private final ImageList imageList;
    
    private final ConfirmationUnshareButton unShareButton;
    
    private final UnshareButton layerButton;
    
    private SharingRemoveAllAction removeAction;
    private EventList<LocalFileItem> currentEventList;
    
    private static final String unshareAll = I18n.tr("Unshare All");
    
    public SharingFancyListPanel(String name, EventList<LocalFileItem> eventList, DropTarget dropTarget, LocalFileList fileList, Icon panelIcon) {       
        setBackground(Color.WHITE);
        
        this.currentEventList = eventList;
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        
        JLabel unShareButtonLabel = new JLabel(unshareAll);
        removeAction = new SharingRemoveAllAction(fileList, eventList);
        unShareButton = new ConfirmationUnshareButton(removeAction);
        unShareButton.setEnabled(false);
    
        // black seperator
        Line line = new Line(Color.BLACK, 3);
        
        final JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem("Item"));
        
        imageList = new ImageList(eventList, fileList);
        imageList.setDropTarget(dropTarget);  
        
        layerButton = new UnshareButton(new SharingRemoveListAction(imageList));
        layerButton.setSize(60, 30);
        layerButton.setVisible(false);
        layerButton.addMouseListener(new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent e) {
                e.getComponent().setVisible(false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
        });
        new MouseReaction(imageList, layerButton);
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remainign space
        setLayout(new MigLayout("insets 10 25 0 10",     //layout contraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow]" ));    // row contraints
        
        add(headerLabel, "push");       // first row
        add(unShareButtonLabel, "split 2");
        add(unShareButton, "wrap");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        JXLayer<JComponent> l = new JXLayer<JComponent>(scrollPane, new  AbstractLayerUI<JComponent>());
        l.getGlassPane().setLayout(null);
        l.getGlassPane().add(layerButton);
        //third row
        add(l, "span 2, grow");

        eventList.addListEventListener(this);
        
        setVisible(false);
    }
    
    public void setModel(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        currentEventList.removeListEventListener(this);
        currentEventList = eventList;
        currentEventList.addListEventListener(this);
        imageList.setModel(new ImageListModel(eventList, fileList));
        removeAction.setEventList(eventList);
        removeAction.setFileList(fileList);
        
        int size = eventList.size();
        if( size == 0 ) {
            unShareButton.setEnabled(false);
            SharingFancyListPanel.this.setVisible(false);
        } else {
            unShareButton.setEnabled(true);
            SharingFancyListPanel.this.setVisible(true);
        }
    }
    
    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        final int size = listChanges.getSourceList().size();
        SwingUtils.invokeLater(new Runnable(){
            public void run() {
                if(size == 0 ) {
                    unShareButton.setEnabled(false);
                    SharingFancyListPanel.this.setVisible(false);
                } else {
                    unShareButton.setEnabled(true);
                    SharingFancyListPanel.this.setVisible(true);
                }
            }
        });
    }
    
    public class MouseReaction implements MouseListener, MouseMotionListener {

        private ImageList imageList;
        private JButton ushareButton;
        
        public MouseReaction(ImageList imageList, JButton unShareButton) {
            this.imageList = imageList;
            this.ushareButton = unShareButton;
            
            imageList.addMouseListener(this);
            imageList.addMouseMotionListener(this);
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            if(imageList.getModel().getSize() > 0)
                ushareButton.setVisible(true);
        }

        @Override
        public void mouseExited(MouseEvent e) { 
            if(!ushareButton.getBounds().contains(e.getPoint())) {
                imageList.clearSelection();
                ushareButton.setVisible(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
        @Override
        public void mouseDragged(MouseEvent e) {}

        @Override
        public void mouseMoved(MouseEvent e) {
            int index = imageList.locationToIndex(e.getPoint());
            if( index > -1) {
                imageList.setSelectedIndex(index);
                
                Rectangle bounds = imageList.getCellBounds(index, index);
                ushareButton.setLocation(bounds.x + bounds.width - ushareButton.getWidth()-25, 
                       bounds.y + 25);
            }
        }
    }
}