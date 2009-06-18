package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * Renderers an image with a border.
 */
class ImageCellRenderer extends JPanel implements ListCellRenderer {
    
    protected Border border;
    
    private final ThumbnailManager thumbnailManager;
    private final ImageRenderer imageRenderer;
    
    @Resource protected Color cellBackgroundColor;
    @Resource protected Color cellBorderColor;
    @Resource protected Color cellSelectedBackground;
    @Resource protected Icon errorIcon;
    @Resource private int height;
    @Resource private int width;
    @Resource private int insetTop;
    @Resource private int insetBottom;
    @Resource private int insetLeft;
    @Resource private int insetRight;
    
    @Inject
    public ImageCellRenderer(ThumbnailManager thumbnailManager, ImageRenderer imageRenderer) {
        this.thumbnailManager = thumbnailManager;
        this.imageRenderer = imageRenderer;
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        
        setPreferredSize(new Dimension(width, height));
        setSize(getPreferredSize());
        setBackground(cellBackgroundColor);
        setLayout(new MigLayout("insets 0, gap 0, fill"));
        add(imageRenderer, "align 50%");
        
        border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(insetTop, insetLeft, insetBottom, insetRight),
                BorderFactory.createMatteBorder(1, 1, 1, 1, cellBorderColor));
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        LocalFileItem item = (LocalFileItem)value;
        imageRenderer.setIcon(thumbnailManager.getThumbnailForFile(item.getFile(), list, index));
        if(thumbnailManager.isErrorIcon(item.getFile()))
            imageRenderer.setText(item.getFileName());
        else
            imageRenderer.setText("");
        
        setToolTipText(item.getFileName());
        
        if(isSelected)
            setBackground(cellSelectedBackground);
        else 
            setBackground(cellBackgroundColor);
        this.setBorder(border);
        
        return this;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(insetLeft, insetTop, getWidth() - insetLeft - insetRight, getHeight() - insetTop - insetBottom);
        super.paintComponent(g);
    }
}