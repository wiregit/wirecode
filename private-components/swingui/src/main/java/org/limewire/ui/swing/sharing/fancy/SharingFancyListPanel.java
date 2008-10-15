package org.limewire.ui.swing.sharing.fancy;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.sharing.actions.SharingRemoveAllAction;
import org.limewire.ui.swing.sharing.actions.SharingRemoveListAction;
import org.limewire.ui.swing.sharing.components.ConfirmationUnshareButton;
import org.limewire.ui.swing.sharing.components.UnshareButton;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
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
    
    @Resource
    private Color lineColor;
    @Resource
    private int lineSize;
    @Resource
    private Color backgroundColor;
    @Resource
    private Color mainLabelColor;
    @Resource
    private int mainLabelFontSize;
    
    private final ImageList imageList;
    
    private final ConfirmationUnshareButton unShareButton;
    
    private final UnshareButton layerButton;
    
    private SharingRemoveAllAction removeAction;
    private EventList<LocalFileItem> currentEventList;
    
    private static final String unshareAll = I18n.tr("Unshare All");
    
    public SharingFancyListPanel(String name, EventList<LocalFileItem> eventList, TransferHandler transferHandler, LocalFileList fileList, Icon panelIcon, ThumbnailManager thumbnailManager) {       
        GuiUtils.assignResources(this); 
        
        setBackground(backgroundColor);
        
        this.currentEventList = eventList;
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        headerLabel.setForeground(mainLabelColor);
        FontUtils.setSize(headerLabel, mainLabelFontSize);
        FontUtils.bold(headerLabel);
        
        JLabel unShareButtonLabel = new JLabel(unshareAll);
        removeAction = new SharingRemoveAllAction(fileList, eventList);
        unShareButton = new ConfirmationUnshareButton(removeAction);
        unShareButton.setEnabled(false);
    
        // black seperator
        Line line = Line.createHorizontalLine(lineColor, lineSize);
        
        imageList = new ImageList(eventList, fileList, thumbnailManager);
        imageList.setTransferHandler(transferHandler);
        
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
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remaining space
        setLayout(new MigLayout("gap 0, insets 18 6 10 6",     //layout constraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow][grow]" ));    // row constraints
        
        add(headerLabel, "gapbottom 4, push");       // first row
        add(unShareButtonLabel, "gapbottom 2, split 2");
        add(unShareButton, "gapbottom 2, wrap");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
            this.ushareButton.setBorder(null);
            this.ushareButton.setContentAreaFilled(false);
            
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