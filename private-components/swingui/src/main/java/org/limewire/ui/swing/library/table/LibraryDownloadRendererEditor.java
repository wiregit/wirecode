package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

public class LibraryDownloadRendererEditor extends TableRendererEditor{
  
    @Resource
    private Icon downloadButtonIcon;
    @Resource
    private Icon downloadButtonPressedIcon;

    private JButton downloadButton;
    
    public LibraryDownloadRendererEditor(Action downloadAction){
        GuiUtils.assignResources(this);

        downloadButton = new IconButton(downloadButtonIcon, downloadButtonIcon, downloadButtonPressedIcon);
        downloadButton.addActionListener(downloadAction);

        
        add(downloadButton);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        // TODO Auto-generated method stub
        return this;
    }

}
