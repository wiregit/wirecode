package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

public class DownloadMessageRenderer extends DefaultTableCellRenderer {
    
    public DownloadMessageRenderer(){
        new DownloadRendererProperties().decorateComponent(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value != null && value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem)value;
            return super.getTableCellRendererComponent(table, getPercentMessage(item) + getMessage(item.getState(), item), isSelected, false, row, column);
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
    
    private String getPercentMessage(DownloadItem item){
        int percent = item.getPercentComplete();
        DownloadState state = item.getState();
        if (percent == 0 || state == DownloadState.DONE ||  state == DownloadState.DOWNLOADING ||  state == DownloadState.ERROR){
            return "";
        }
        return percent + "% - ";    
    }
    
    /**
     * @return the message displayed after the percentage. Can be null for non-covered states.
     */
    private String getMessage(DownloadState state, DownloadItem item) {
        switch (state) {
        case RESUMING:
            return I18n.tr("Resuming");
        case CANCELLED:
            return I18n.tr("Canceled");
        case FINISHING:
            return I18n.tr("Finishing download...");
        case DONE:
            return I18n.tr("Done");
        case CONNECTING:
            return I18n.tr("Connecting...");
        case DOWNLOADING:
            // {0}: current size
            // {1}: total size
            // {2}: download speed
            return I18n.tr("Downloading {0} of {1} ({2})",
                    GuiUtils.toUnitbytes(item.getCurrentSize()), 
                    GuiUtils.toUnitbytes(item.getTotalSize()),
                    GuiUtils.rate2speed(item.getDownloadSpeed()), 
                    item.getDownloadSourceCount());
        case TRYING_AGAIN:
            return getTryAgainMessage(item.getRemainingTimeInState());
        case STALLED:
            return I18n.tr("Stalled - {0} of {1}", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()),
                    GuiUtils.toUnitbytes(item.getTotalSize()));
        case ERROR:         
            return I18n.tr("Unable to download: ") + I18n.tr(item.getErrorState().getMessage());
        case PAUSED:
            // {0}: current size, {1} total size
            return I18n.tr("Paused - {0} of {1}", 
                    GuiUtils.toUnitbytes(item.getCurrentSize()), GuiUtils.toUnitbytes(item.getTotalSize()));
        case LOCAL_QUEUED:
            return getQueueTimeMessage(item.getRemainingTimeInState());
        case REMOTE_QUEUED:
            if(item.getRemoteQueuePosition() == -1 || item.getRemoteQueuePosition() == Integer.MAX_VALUE){
                return getQueueTimeMessage(item.getRemainingTimeInState());
            }
            return I18n.trn("Waiting - Next in line",
                    "Waiting - {0} in line",
                    item.getRemoteQueuePosition(), item.getRemoteQueuePosition());
        default:
            return null;
        }
        
    }
    
    private String getTryAgainMessage(long tryingAgainTime) {
        if(tryingAgainTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Searching for people with this file...");                
        } else {
            return I18n.tr("Searching for people with this file... ({0} left)", CommonUtils.seconds2time(tryingAgainTime));
        }
    }
    
    private String getQueueTimeMessage(long queueTime){
        if(queueTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Waiting - remaining time unknown");                
        } else {
            return I18n.tr("Waiting - Starting in {0}", CommonUtils.seconds2time(queueTime));
        }
    }

}
