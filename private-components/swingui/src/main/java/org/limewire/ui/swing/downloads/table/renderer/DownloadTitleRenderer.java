package org.limewire.ui.swing.downloads.table.renderer;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.ui.swing.transfer.TransferTitleRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertiableFileUtils;

import com.google.inject.Inject;

/**
 * Cell renderer for the title column in the Downloads table.
 */
public class DownloadTitleRenderer extends TransferTitleRenderer {

    @Resource private Icon warningIcon;
    @Resource private Icon downloadingIcon;
    
    private CategoryIconManager categoryIconManager;
    
    @Inject
    public DownloadTitleRenderer(CategoryIconManager categoryIconManager) {
        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this);
    }
    
    @Override
    protected Icon getIcon(Object value) {
        if (!(value instanceof DownloadItem)) {
            return null;
        }
        DownloadItem item = (DownloadItem) value;
        DownloadState state = item.getState();
        
        switch (state) {
        case ERROR:
            return warningIcon;

        case FINISHING:
        case DONE:
            return categoryIconManager.getIcon(item.getCategory());
            
        default:
            return downloadingIcon;
        }
    }
    
    @Override
    protected String getText(Object value) {
        if (!(value instanceof DownloadItem)) {
            return "";
        }
        DownloadItem item = (DownloadItem) value;
        
        if (item.getDownloadItemType() == DownloadItemType.BITTORRENT) {
            return I18n.tr("{0} (torrent)", PropertiableFileUtils.getNameProperty(item, true));
        } else {
            return PropertiableFileUtils.getNameProperty(item, true);
        }
    }
}
