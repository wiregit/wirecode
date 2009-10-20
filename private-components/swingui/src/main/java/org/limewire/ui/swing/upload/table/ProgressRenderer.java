package org.limewire.ui.swing.upload.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.CommonUtils;

/**
 * Cell renderer for the progress column in the Uploads table.
 */
class ProgressRenderer extends JXPanel implements TableCellRenderer {

    @Resource(key="DownloadRendererProperties.font") private Font font;
    @Resource(key="DownloadRendererProperties.labelColor") private Color foreground;
    @Resource(key="DownloadProgressRenderer.progressBarWidth") private int progressBarWidth;
    @Resource(key="DownloadProgressRenderer.progressBarHeight") private int progressBarHeight;
    /**the progress bar disappears when the column width is less than this value*/
    @Resource(key="DownloadProgressRenderer.progressBarCutoffWidth") private int progressBarCutoffWidth;
    @Resource(key="DownloadProgressRenderer.progressBarBorder") private Color progressBarBorder;
    
    private LimeProgressBar progressBar;
    private JLabel timeLabel;
    
    /**
     * Constructs a ProgressRenderer.
     */
    public ProgressRenderer(ProgressBarDecorator progressBarDecorator) {
        super(new MigLayout("insets 0, gap 0, novisualpadding, nogrid, aligny center"));
        GuiUtils.assignResources(this);
        
        progressBar = new LimeProgressBar(0, 100);
        progressBarDecorator.decoratePlain(progressBar);  
        progressBar.setBorder(new LineBorder(progressBarBorder));
        Dimension size = new Dimension(progressBarWidth, progressBarHeight);
        progressBar.setMaximumSize(size);
        progressBar.setPreferredSize(size);
        
        timeLabel = new JLabel();
        timeLabel.setFont(font);
        timeLabel.setForeground(foreground);
        
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
            progressBar.setVisible((item.getState() == UploadState.UPLOADING) && (columnWidth > progressBarCutoffWidth));
            if (progressBar.isVisible()) {
                progressBar.setValue((int) (100 * item.getTotalAmountUploaded() / item.getFileSize()));
            }

            timeLabel.setVisible((item.getState() == UploadState.UPLOADING) && (item.getRemainingUploadTime() <= Long.MAX_VALUE - 1000));
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
