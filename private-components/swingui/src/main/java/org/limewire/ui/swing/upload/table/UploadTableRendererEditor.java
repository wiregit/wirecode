package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

public class UploadTableRendererEditor extends TableRendererEditor {
    

    private CategoryIconManager categoryIconManager;
    
    private JLabel statusLabel;
    private JLabel nameLabel;
    private JXButton cancelButton;
    private UploadItem editItem;
    private JProgressBar progressBar;
    
    private final Action cancelAction = new AbstractAction(I18n.tr("Cancel")) {
        @Override
        public void actionPerformed(ActionEvent e) {
           cancelUpload();
           cancelCellEditing();
        }
    };
    
    public UploadTableRendererEditor(CategoryIconManager categoryIconManager, LimeProgressBarFactory progressBarFactory){
        setLayout(new MigLayout("debug, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        this.categoryIconManager = categoryIconManager;
        
        nameLabel = new JLabel("Name");
        statusLabel = new JLabel("Status");
        cancelButton = new JXButton(cancelAction);  
        
        progressBar = progressBarFactory.create();
        Dimension size = new Dimension(400, 16);
        progressBar.setPreferredSize(size);
        progressBar.setMinimumSize(size);
        progressBar.setMaximumSize(size);
        
        add(nameLabel, "aligny bottom");
        add(cancelButton, "alignx right, aligny 50%, spany 3, wrap");
        add(progressBar, "hidemode 3, wrap");
        add(statusLabel, "aligny top,");
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
        statusLabel.setText(item.toString());
        
        progressBar.setVisible(item.getState() == UploadState.UPLOADING);
        if (progressBar.isVisible()) { 
            progressBar.setValue((int)(100 * item.getTotalAmountUploaded()/item.getFileSize()));
        }
    }
    
    private void cancelUpload() {
        editItem.cancel();
    }

}
