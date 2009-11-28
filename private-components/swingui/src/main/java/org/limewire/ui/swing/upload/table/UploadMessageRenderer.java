package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.transfer.TransferRendererResources;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer for the message column in the Uploads table.
 */
class UploadMessageRenderer extends DefaultTableCellRenderer {

    private final NumberFormat formatter = new DecimalFormat("0.00");
    
    /**
     * Constructs an UploadMessageRenderer.
     */
    public UploadMessageRenderer() {
        new TransferRendererResources().decorateComponent(this);
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
                return I18n.trn("{0} to {1} person - ratio: {2}",
                        "{0} to {1} people - ratio: {2}",
                        numConnections, GuiUtils.formatKilobytesPerSec(item.getUploadSpeed()), numConnections, ratio);
            } else {
                return I18n.tr("{0} of {1} ({2})", 
                        GuiUtils.formatUnitFromBytes(item.getTotalAmountUploaded()), 
                        GuiUtils.formatUnitFromBytes(item.getFileSize()), 
                        GuiUtils.formatKilobytesPerSec(item.getUploadSpeed()));
            }
            
        case PAUSED:
            return I18n.tr("Paused");
            
        case QUEUED:
            return I18n.tr("Waiting...");
            
        case REQUEST_ERROR:
            return I18n.tr("Unable to upload: invalid request");
            
        case LIMIT_REACHED:
            return I18n.tr("Unable to upload: upload limit reached");

        default:
            return "";
        }
    }
}
