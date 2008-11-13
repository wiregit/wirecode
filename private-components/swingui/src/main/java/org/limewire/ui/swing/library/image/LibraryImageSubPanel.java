package org.limewire.ui.swing.library.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.Sharable;
import org.limewire.ui.swing.library.sharing.FileShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.ShareTableRendererEditor;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryImageSubPanel extends JPanel implements ListEventListener<LocalFileItem>, Sharable {
    
    @Resource
    private Color lineColor = Color.BLACK;
    @Resource
    private int lineSize = 2;
    @Resource
    private Color backgroundColor = Color.WHITE;
    @Resource
    private Color mainLabelColor = Color.BLACK;
    @Resource
    private int mainLabelFontSize = 12;
    
    private final ImageList imageList;    
    
    private ShareTableRendererEditor shareEditor;
    
    private  JXLayer<JComponent> layer;
    
    private Dimension buttonPanelDimension = new Dimension(ThumbnailManager.WIDTH,28);
    

    private EventList<LocalFileItem> currentEventList;
    private EventList<LocalFileItem> listSelection;    
    
    
    
    public LibraryImageSubPanel(String name, EventList<LocalFileItem> eventList, LocalFileList fileList, Icon panelIcon, 
            ThumbnailManager thumbnailManager, ImageLibraryPopupParams params) {       
        GuiUtils.assignResources(this); 
        
        setBackground(backgroundColor);
        
        this.currentEventList = eventList;
        JLabel headerLabel = new JLabel(name, panelIcon, JLabel.CENTER);
        headerLabel.setForeground(mainLabelColor);
        FontUtils.setSize(headerLabel, mainLabelFontSize);
        FontUtils.bold(headerLabel);
        
        // black separator
        Line line = Line.createHorizontalLine(lineColor, lineSize);
        
        imageList = new ImageList(currentEventList, fileList);
        EventSelectionModel<LocalFileItem> model = new EventSelectionModel<LocalFileItem>(currentEventList);
        imageList.setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.listSelection = model.getSelected();
        //imageList.setTransferHandler(transferHandler);
        imageList.setPopupHandler(new MyImageLibraryPopupHandler(this, params));
        
        ImageCellRenderer renderer = new LibraryImageCellRenderer(imageList.getFixedCellWidth(), imageList.getFixedCellHeight() - 2, thumbnailManager);
        renderer.setOpaque(false);
        JComponent buttonRenderer = new ShareTableRendererEditor(null);
        buttonRenderer.setOpaque(false);
        buttonRenderer.setPreferredSize(buttonPanelDimension);
        buttonRenderer.setSize(buttonPanelDimension);
        renderer.setButtonComponent(buttonRenderer);
        imageList.setImageCellRenderer(renderer);
        
        // top row should never be tall than 30pixels, the bottom row(table, should fill any remaining space
        setLayout(new MigLayout("gap 0, insets 18 4 10 4",     //layout constraints
                "[] [] ",                       // column constraints
                "[::30] [] [grow][grow]" ));    // row constraints
        
        add(headerLabel, "gapbottom 4, push, wrap");       // first row
       // add(unShareButtonLabel, "gapbottom 2, split 2");
        
        // second row
        add(line, "span 2, growx 100, height :: 3, wrap");
        
        JScrollPane scrollPane = new JScrollPane(imageList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layer = new JXLayer<JComponent>(scrollPane, new  AbstractLayerUI<JComponent>());
        layer.getGlassPane().setLayout(null);
        //third row
        add(layer, "span 2, grow");

        eventList.addListEventListener(this);
        
      //  setVisible(false);
    }
    
    public void enableSharing(LibrarySharePanel sharePanel){
        shareEditor = new ShareTableRendererEditor(new ShareAction(I18n.tr("Sharing"), sharePanel));
        shareEditor.setPreferredSize(buttonPanelDimension);
        shareEditor.setSize(buttonPanelDimension);
        shareEditor.setOpaque(false);
        shareEditor.setVisible(false);
                
        new MouseReaction(imageList, shareEditor);
        
        layer.getGlassPane().add(shareEditor);
    }
    
    public JList getList() {
        return imageList;
    }   
    
    public ImageListModel getModel(){
        return (ImageListModel)imageList.getModel();
    }
    
    public EventList<LocalFileItem> getSelectedItems(){
        return listSelection;
    }

    
    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        final int size = listChanges.getSourceList().size();
        SwingUtils.invokeLater(new Runnable(){
            public void run() {
                setVisible(size > 0);
            }
        });
    }
    
    public class MouseReaction implements MouseListener, MouseMotionListener {

        private ImageList imageList;
        private ShareTableRendererEditor hoverComponent;
        
        public MouseReaction(ImageList imageList, ShareTableRendererEditor hoverComponent) {
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
            if( index > -1)
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
            if (imageList.getModel().getSize() > 0) {
                int index = imageList.locationToIndex(point);
                if (index > -1) {
                    hoverComponent.setVisible(true);
                    hoverComponent.configure((LocalFileItem) imageList.getModel().getElementAt(index), true);
                    Rectangle bounds = imageList.getCellBounds(index, index);
                    ImageCellRenderer renderer = imageList.getImageCellRenderer();
                    hoverComponent.setLocation(bounds.x + renderer.getSubComponentLocation().x,
                            bounds.y + renderer.getSubComponentLocation().y);
                }
            }
        }
    }

  
    public void dispose() {
        // TODO Auto-generated method stub
        
    }
    
    private class ShareAction extends AbstractAction {

        
        private LibrarySharePanel librarySharePanel;

        public ShareAction(String text, LibrarySharePanel librarySharePanel){
            super(text);
            this.librarySharePanel = librarySharePanel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((FileShareModel)librarySharePanel.getShareModel()).setFileItem(shareEditor.getLocalFileItem());
          
            Rectangle bounds = shareEditor.getShareButton().getBounds();
            Point convertedLocation = SwingUtilities.convertPoint(shareEditor, shareEditor.getShareButton().getLocation(), LibraryImageSubPanel.this.getParent());
            bounds.x = convertedLocation.x;
            bounds.y = convertedLocation.y;
            librarySharePanel.show(shareEditor.getShareButton());
            shareEditor.cancelCellEditing();
        }
        
    }
}
