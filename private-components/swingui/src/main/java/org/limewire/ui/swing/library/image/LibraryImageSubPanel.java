package org.limewire.ui.swing.library.image;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.library.table.Configurable;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryImageSubPanel extends JPanel implements ListEventListener<LocalFileItem> {
    
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
    
    private  JXLayer<JComponent> layer;

    private EventList<LocalFileItem> currentEventList;
    private EventList<LocalFileItem> listSelection;    
    
    public LibraryImageSubPanel(String name, EventList<LocalFileItem> eventList, LocalFileList fileList, Icon panelIcon, 
            ImageLibraryPopupParams params) {       
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
    }
    
    public void setImageEditor(TableRendererEditor editor) {
        new MouseReaction(imageList, editor);
        layer.getGlassPane().add(editor);
    }
    
    public ImageList getImageList() {
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
                    ((Configurable)hoverComponent).configure((LocalFileItem) imageList.getModel().getElementAt(index), true);
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
}
