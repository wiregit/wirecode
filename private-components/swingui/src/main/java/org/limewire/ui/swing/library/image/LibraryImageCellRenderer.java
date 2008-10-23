package org.limewire.ui.swing.library.image;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.table.Configurable;
import org.limewire.ui.swing.util.GuiUtils;

public class LibraryImageCellRenderer extends ImageCellRenderer {
    private Configurable configurable;

    @Resource
    protected Color cellBorderColor;
    @Resource
    protected Color cellBorderSelectionColor;
    
    public LibraryImageCellRenderer(int width, int height, ThumbnailManager thumbnailManager) {
        super(width, height, thumbnailManager);
        
        GuiUtils.assignResources(this);
        
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, cellBorderColor);
        selectedBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, cellBorderSelectionColor);
    }
    
    
    public void setButtonComponent(JComponent buttonPanel) {
        super.setButtonComponent(buttonPanel);
        if(buttonPanel instanceof Configurable){
            configurable = (Configurable)buttonPanel;
        }
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        if (configurable != null){
            configurable.configure((LocalFileItem)value);
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }

}
