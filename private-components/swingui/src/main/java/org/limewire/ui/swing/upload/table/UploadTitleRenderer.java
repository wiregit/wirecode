package org.limewire.ui.swing.upload.table;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.BrowseType;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.downloads.table.renderer.DownloadRendererProperties;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer for the title column in the Uploads table.
 */
class UploadTitleRenderer extends JLabel implements TableCellRenderer {

    @Resource private Icon friendBrowseHostIcon;
    @Resource private Icon p2pBrowseHostIcon;
    
    private final CategoryIconManager iconManager;
    private final DownloadRendererProperties rendererProperties;
    
    /**
     * Constructs an UploadTitleRenderer.
     */
    public UploadTitleRenderer(CategoryIconManager iconManager) {
        this.iconManager = iconManager;
        this.rendererProperties = new DownloadRendererProperties();
        
        GuiUtils.assignResources(this);
        
        rendererProperties.decorateComponent(this);
        setBorder(BorderFactory.createEmptyBorder(0,4,0,0));
        setIconTextGap(6);
        // Row highlighters only work on opaque renderers
        setOpaque(true);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        if (value instanceof UploadItem) {
            UploadItem uploadItem = (UploadItem) value;
            updateIcon(uploadItem);
            updateText(uploadItem);
            
        } else {
            setIcon(null);
            setText("");
        }
        
        return this;
    }

    /**
     * Updates the renderer icon for the specified upload item.
     */
    private void updateIcon(UploadItem uploadItem) {
        switch (uploadItem.getState()) {
        case UPLOADING:
            setIcon(iconManager.getIcon(uploadItem.getCategory()));
            break;

        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            if (uploadItem.getBrowseType() == BrowseType.FRIEND) {
                setIcon(friendBrowseHostIcon);
            } else {
                setIcon(p2pBrowseHostIcon);
            }
            break;

        default:
            setIcon(iconManager.getIcon(uploadItem.getCategory()));
            break;
        }
    }

    /**
     * Updates the renderer text for the specified upload item.
     */
    private void updateText(UploadItem uploadItem) {
        switch (uploadItem.getState()) {
        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            // Name is hidden for browse host.
            setText("");
            break;
            
        default:
            if (uploadItem.getUploadItemType() == UploadItemType.BITTORRENT) {
                setText(I18n.tr("{0} (torrent)", uploadItem.getFileName()));
            } else {
                setText(uploadItem.getFileName());
            }
            break;
        }
    }
}
