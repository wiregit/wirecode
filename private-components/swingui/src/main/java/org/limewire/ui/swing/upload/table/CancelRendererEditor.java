package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Cell renderer/editor to display the cancel and remove buttons for the 
 * uploads table.
 */
class CancelRendererEditor extends TableRendererEditor {

    @Resource(key="DownloadCancelRendererEditor.cancelIcon")
    private Icon cancelIcon;
    @Resource(key="DownloadCancelRendererEditor.cancelIconPressed")
    private Icon cancelIconPressed;
    @Resource(key="DownloadCancelRendererEditor.cancelIconRollover")
    private Icon cancelIconRollover;
    
    private final JButton cancelButton;
    private final JButton removeButton;
    
    private UploadItem item;
    
    /**
     * Constructs a CancelRendererEditor.
     */
    public CancelRendererEditor(final UploadActionHandler actionHandler) {
        setLayout(new MigLayout("insets 0, gap 0, nogrid, novisualpadding, alignx center, aligny center"));
        
        GuiUtils.assignResources(this);

        cancelButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        cancelButton.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        cancelButton.setToolTipText(I18n.tr("Cancel upload"));

        removeButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        removeButton.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        removeButton.setToolTipText(I18n.tr("Remove upload"));

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionHandler.performAction(e.getActionCommand(), item);
                cancelCellEditing();
            }
        };
        cancelButton.addActionListener(listener);
        removeButton.addActionListener(listener);
        
        add(cancelButton, "hidemode 3");
        add(removeButton, "hidemode 3");
    }
    
    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, 
            boolean isSelected, int row, int column) {
        if (value instanceof UploadItem) {
            item = (UploadItem) value;
            updateButtons(item.getState());
            return this;
        } else {
            return emptyPanel;
        }
    }

    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof UploadItem) {
            updateButtons(((UploadItem) value).getState());
            return this;
        } else {
            return emptyPanel;
        }
    }
    
    /**
     * Updates the visibility of the buttons.
     */
    private void updateButtons(UploadState state) {
        cancelButton.setVisible(state != UploadState.DONE);
        removeButton.setVisible(state == UploadState.DONE);
    }
}
