package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTable;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.table.TableRendererEditor;

public class ButtonRendererEditor extends TableRendererEditor{
    private DownloadButtonPanel buttonPanel;
    private DownloadItem item;
    
    public ButtonRendererEditor(){        
        buttonPanel = new DownloadButtonPanel();
        
        setLayout(new BorderLayout());
        add(buttonPanel);
    }
    
    public void setActionHandler(final DownloadActionHandler actionHandler) {
        buttonPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionHandler.performAction(e.getActionCommand(), item);
                cancelCellEditing();
            }
        });
    }

    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        item =(DownloadItem)value;
        buttonPanel.updateButtons(item.getState());
        return this;
    }

    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        buttonPanel.updateButtons(((DownloadItem)value).getState());
        return this;
    }

}
