package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Renderers an image with a border.
 */
public class ImageCellRenderer extends ImageLabel implements ListCellRenderer {
    
    protected Border border;
    protected Border selectedBorder;
    
    @Resource
    protected Color cellBackgroundColor;
    @Resource
    protected Color cellBorderColor;
    @Resource
    protected Color cellBorderSelectionColor;
    @Resource
    protected Icon errorIcon;
    
    private ThumbnailManager thumbnailManager;
    
    public ImageCellRenderer(int width, int height, ThumbnailManager thumbnailManager) {
        super(width, height);
        
        this.thumbnailManager = thumbnailManager;
        
        GuiUtils.assignResources(this);
        
        setBackground(this.cellBackgroundColor);
        
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, cellBorderColor);
        selectedBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, cellBorderSelectionColor);
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        LocalFileItem item = (LocalFileItem)value;
        setIcon(thumbnailManager.getThumbnailForFile(item.getFile(), list, index));
        if(thumbnailManager.isErrorIcon(item.getFile()))
            setText(item.getFileName());
        else
            setText("");
        
        if(isSelected)
            this.setBorder(selectedBorder);
        else 
            this.setBorder(border);
        
        return this;
    }
}