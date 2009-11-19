package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;

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

    @Resource private Icon antivirusIcon;
    @Resource private Icon warningIcon;
    @Resource private Icon downloadingIcon;
    
    private CategoryIconManager categoryIconManager;
    
    @Inject
    public DownloadTitleRenderer(CategoryIconManager categoryIconManager) {
        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component renderer = super.getTableCellRendererComponent(table, value, 
                isSelected, hasFocus, row, column);
        
        // Adjust foreground color when threat detected.
        // TODO research why this doesn't work
        if (value instanceof DownloadItem) {
            if (((DownloadItem) value).getState() == DownloadState.THREAT_FOUND) {
                renderer.setForeground(resources.getDisabledForeground());
            }
        }
        
        return renderer;
    }
    
    @Override
    protected Icon getIcon(Object value) {
        if (!(value instanceof DownloadItem)) {
            return null;
        }
        DownloadItem item = (DownloadItem) value;
        
        if (item.getDownloadItemType() == DownloadItemType.ANTIVIRUS) {
            return antivirusIcon;
        }
        
        switch (item.getState()) {
        case ERROR:
        case THREAT_FOUND:
            return warningIcon;

        case NOT_SCANNED:
        case FINISHING:
        case DONE:
            return categoryIconManager.getIcon(item.getCategory());
            
        case SCANNING:
        case SCANNING_FRAGMENT:
            return antivirusIcon;
            
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
        
        switch (item.getDownloadItemType()) {
        case ANTIVIRUS:
            return I18n.tr("Updating AVG Anti-Virus definitions...");
        case BITTORRENT:
            return I18n.tr("{0} (torrent)", PropertiableFileUtils.getNameProperty(item, true));
        case GNUTELLA:
        default:
            return PropertiableFileUtils.getNameProperty(item, true);
        }
    }
}
