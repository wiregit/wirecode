package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

public class LibraryDownloadRendererEditor extends TableRendererEditor{
  
    @Resource
    private Icon downloadButtonIcon;
    @Resource
    private Icon downloadButtonPressedIcon;
    @Resource
    private Icon downloadButtonPressedRollover;

    private JButton downloadButton;
    private ArrayList<RemoteFileItem> downloadList;
    
    /**
     * 
     * @param downloadList list of files being download from the friend's library
     */
    public LibraryDownloadRendererEditor(Action downloadAction, ArrayList<RemoteFileItem> downloadList){
        GuiUtils.assignResources(this);
        this.downloadList = downloadList;

        downloadButton = new IconButton(downloadButtonIcon, downloadButtonPressedRollover, downloadButtonPressedIcon);
        downloadButton.addActionListener(downloadAction);
        
        add(downloadButton);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        downloadButton.setEnabled(!downloadList.contains(value));
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        downloadButton.setEnabled(!downloadList.contains(value));
        return this;
    }

}
