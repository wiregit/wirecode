package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.downloads.table.renderer.DownloadRendererProperties;
import org.limewire.util.CommonUtils;

/**
 * Cell renderer for the progress column in the Uploads table.
 */
class UploadProgressRenderer extends JXPanel implements TableCellRenderer {

    private final DownloadRendererProperties rendererProperties;
    
    private LimeProgressBar progressBar;
    private JLabel timeLabel;
    
    /**
     * Constructs an UploadProgressRenderer.
     */
    public UploadProgressRenderer(ProgressBarDecorator progressBarDecorator) {
        super(new MigLayout("insets 0, gap 0, novisualpadding, nogrid, aligny center"));
        
        rendererProperties = new DownloadRendererProperties();
        
        progressBar = new LimeProgressBar(0, 100);
        progressBarDecorator.decoratePlain(progressBar);  
        progressBar.setBorder(new LineBorder(rendererProperties.getProgressBarBorderColor()));
        Dimension size = new Dimension(rendererProperties.getProgressBarWidth(), rendererProperties.getProgressBarHeight());
        progressBar.setMaximumSize(size);
        progressBar.setPreferredSize(size);
        
        timeLabel = new JLabel();
        rendererProperties.decorateComponent(timeLabel);
        
        add(progressBar);
        add(timeLabel);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof UploadItem) {
            UploadItem item = (UploadItem) value;
            updateProgress(item, table.getColumnModel().getColumn(column).getWidth());
        } else {
            progressBar.setValue(0);
            progressBar.setVisible(false);
            timeLabel.setVisible(false);
        }
        return this;
    }

    /**
     * Updates the progress bar and time for the specified upload item.
     */
    private void updateProgress(UploadItem item, int columnWidth) {
        if (UploadItemType.GNUTELLA == item.getUploadItemType()) {
            progressBar.setVisible((item.getState() == UploadState.UPLOADING) && 
                    (columnWidth > rendererProperties.getProgressBarCutoffWidth()));
            if (progressBar.isVisible()) {
                progressBar.setValue((int) (100 * item.getTotalAmountUploaded() / item.getFileSize()));
            }

            timeLabel.setVisible((item.getState() == UploadState.UPLOADING) && 
                    (item.getRemainingUploadTime() <= Long.MAX_VALUE - 1000));
            if (timeLabel.isVisible()) {
                timeLabel.setText(CommonUtils.seconds2time(item.getRemainingUploadTime()));
                timeLabel.setMinimumSize(timeLabel.getPreferredSize());
            }

        } else {
            progressBar.setVisible(false);
            timeLabel.setVisible(false);
        }
    }
}
