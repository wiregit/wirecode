package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.upload.UploadErrorState;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

public class UploadTableRendererEditor extends TableRendererEditor {
    
    private NumberFormat formatter = new DecimalFormat("0.00");
    private CategoryIconManager categoryIconManager;
    
    private JLabel statusLabel;
    private JLabel nameLabel;
    private JXButton cancelButton;
    private UploadItem editItem;
    private JProgressBar progressBar;
    private JLabel timeLabel;
    

    public UploadTableRendererEditor(CategoryIconManager categoryIconManager, LimeProgressBarFactory progressBarFactory){
        setLayout(new MigLayout("fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        this.categoryIconManager = categoryIconManager;
        
        nameLabel = new JLabel("Name");
        statusLabel = new JLabel("Status");
        cancelButton = new JXButton("X"); 
        

        timeLabel = new JLabel("Time");
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        progressBar = progressBarFactory.create();
        Dimension size = new Dimension(400, 16);
        progressBar.setPreferredSize(size);
        progressBar.setMinimumSize(size);
        progressBar.setMaximumSize(size);
        
        add(nameLabel, "aligny bottom");
        add(cancelButton, "alignx right, aligny 50%, spany 3, push, wrap");
        add(progressBar, "hidemode 3, wrap");
        add(statusLabel, "aligny top, split 2");
        add(timeLabel, "push, aligny top, alignx right, hidemode 3");
    }
    
    public void setActionHandler(final UploadActionHandler actionHandler){
        cancelButton.setActionCommand(UploadActionHandler.CANCEL_COMMAND);

        Action cancelAction = new AbstractAction(I18n.tr("Cancel")) {
            @Override
            public void actionPerformed(ActionEvent e) {
               actionHandler.performAction(cancelButton.getActionCommand(), editItem);
            }
        };
        
        cancelButton.addActionListener(cancelAction);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        update((UploadItem)value);
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        editItem = (UploadItem)value;
        update(editItem);
        return this;
    }
    
    private void update(UploadItem item){
        nameLabel.setText(item.getFileName());
        nameLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
        statusLabel.setText(getMessage(item));
        
        if(UploadItemType.GNUTELLA == item.getUploadItemType()) {
            
            progressBar.setVisible(item.getState() == UploadState.UPLOADING);
            if (progressBar.isVisible()) { 
                progressBar.setValue((int)(100 * item.getTotalAmountUploaded()/item.getFileSize()));
            }
            
            timeLabel.setVisible(item.getState() == UploadState.UPLOADING);
            if (timeLabel.isVisible()) {
                timeLabel.setText(CommonUtils.seconds2time(item.getRemainingUploadTime()));
            }        
        } else {
            progressBar.setVisible(false);
            timeLabel.setVisible(false);
        }
    }
    
    
    private String getMessage(UploadItem item){
        switch (item.getState()){
        case DONE:
            return I18n.tr("Done uploading");
        case UPLOADING:
            
            if(UploadItemType.BITTORRENT == item.getUploadItemType()) {
                int numConnections = item.getNumUploadConnections();
                
                long fileSize = item.getFileSize() == 0 ? 1 : item.getFileSize();
                String ratio = formatter.format(item.getTotalAmountUploaded()/(double)fileSize);
                if(numConnections == 1) {
                    return I18n.tr("Seeding to {0} person at {1} - Ratio ({2})", numConnections, GuiUtils.rate2speed(item.getUploadSpeed()), ratio);
                } else {
                    return I18n.tr("Seeding to {0} people at {1} - Ratio ({2})", numConnections, GuiUtils.rate2speed(item.getUploadSpeed()), ratio);
                }
            } else {
                return I18n.tr("Uploading - {0} of {1}({2}) to {3}", GuiUtils.toUnitbytes(item.getTotalAmountUploaded()), 
                        GuiUtils.toUnitbytes(item.getFileSize()), 
                        GuiUtils.rate2speed(item.getUploadSpeed()), item.getHost());
            }
        case QUEUED:
            return I18n.trn("Waiting for {0} upload to finish", "Waiting for {0} uploads to finish", item.getQueuePosition());
        case WAITING:
            return I18n.tr("Waiting for connections...");
        case UNABLE_TO_UPLOAD:
            return getErrorMessage(item.getErrorState());        
        }
        throw new IllegalArgumentException("Unknown UploadState " + item.getState());
    }
    
    private String getErrorMessage(UploadErrorState errorState){
        switch(errorState){
        case FILE_ERROR:
            return I18n.tr("Unable to upload: file error"); 
        case INTERRUPTED:
            return I18n.tr("Unable to upload: transfer interrupted"); 
        case LIMIT_REACHED:
            return I18n.tr("Unable to upload: upload limit reached"); 
        }
        throw new IllegalArgumentException("Unknown UploadErrorState " + errorState);
    }

}
