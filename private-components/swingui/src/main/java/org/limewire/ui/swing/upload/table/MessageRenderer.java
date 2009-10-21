package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.upload.UploadErrorState;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.downloads.table.renderer.DownloadRendererProperties;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer for the message column in the Uploads table.
 */
class MessageRenderer extends DefaultTableCellRenderer {

    private final NumberFormat formatter = new DecimalFormat("0.00");
    
    /**
     * Constructs a MessageRenderer.
     */
    public MessageRenderer() {
        new DownloadRendererProperties().decorateComponent(this);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof UploadItem) {
            UploadItem item = (UploadItem) value;
            return super.getTableCellRendererComponent(table, getMessage(item), isSelected, false, row, column);
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
    
    /**
     * Returns the display message for the specified upload item.
     */
    private String getMessage(UploadItem item) {
        switch (item.getState()) {
        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            return I18n.tr("Library was browsed");
            
        case DONE:
            return I18n.tr("Done uploading");
            
        case UPLOADING:
            if (UploadItemType.BITTORRENT == item.getUploadItemType()) {
                int numConnections = item.getNumUploadConnections();
                String ratio = formatter.format(item.getSeedRatio());
                if (numConnections == 1) {
                    return I18n.tr("Connected to {0} P2P user, uploading at {1} - Ratio {2}", numConnections, GuiUtils.rate2speed(item.getUploadSpeed()), ratio);
                } else {
                    return I18n.tr("Connected to {0} P2P users, uploading at {1} - Ratio {2}", numConnections, GuiUtils.rate2speed(item.getUploadSpeed()), ratio);
                }
            } else {
                return I18n.tr("Uploading - {0} of {1}({2})", 
                        GuiUtils.toUnitbytes(item.getTotalAmountUploaded()), 
                        GuiUtils.toUnitbytes(item.getFileSize()), 
                        GuiUtils.rate2speed(item.getUploadSpeed()));
            }
            
        case WAITING:
        case QUEUED:
            return I18n.tr("Waiting...");
            
        case UNABLE_TO_UPLOAD:
            return getErrorMessage(item.getErrorState());

        default:
            return null;
        }
    }
    
    /**
     * Returns the display message for the specified upload error state.
     */
    private String getErrorMessage(UploadErrorState errorState) {
        switch (errorState) {
        case FILE_ERROR:
            return I18n.tr("Unable to upload: file error"); 
        case INTERRUPTED:
            return I18n.tr("Unable to upload: transfer interrupted"); 
        case LIMIT_REACHED:
            return I18n.tr("Unable to upload: upload limit reached");
        default:
            return null;
        }
    }
}
