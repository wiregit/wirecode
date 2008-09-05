package org.limewire.ui.swing.sharing.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.sharing.menu.SharingActionHandler;
import org.limewire.ui.swing.sharing.menu.SharingPopupHandler;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;

public class SharingFancyTable extends JXTable {

    public SharingFancyTable(EventList<FileItem> sharedItems, FileList fileList, TableFormat<FileItem> tableFormat) {
        super(new SharingTableModel(sharedItems, fileList, tableFormat));
        
        setColumnControlVisible(false);
        setShowGrid(false, false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(false);
        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);
        
        final TableDoubleClickHandler doubleClickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                if( row >= 0 && row < getModel().getRowCount()) {
                    FileItem item = ((SharingTableModel) getModel()).getFileItem(row); System.out.println(item.getName() + " " + item.getCategory());
                    if(PlayerUtils.isPlayableFile(item.getFile())) {
                        PlayerUtils.play(item.getFile());
                    } else {
                        NativeLaunchUtils.launchFile(item.getFile());
                    }
                }
            }
        };
        
        final SharingPopupHandler handler = new SharingPopupHandler(this, new SharingActionHandler());
        
        addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 1) {
                    maybeShowPopup(e);
                } else if(e.getClickCount() > 1) {
                    int row = rowAtPoint(e.getPoint());
                    doubleClickHandler.handleDoubleClick(row);
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if(row != getSelectedRow()) {
                        setRowSelectionInterval(row, row);
                    }
                    if (row >= 0 && col >= 0) {
                        handler.maybeShowPopup(
                            e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        // as long as an editor has been installed, assuming we want to use it
        if(getColumnModel().getColumn(column).getCellEditor() != null)
            return true;
        return false;        
    }

    public void setModel(EventList<FileItem> sharedItems, FileList fileList, TableFormat<FileItem> tableFormat) {
        super.setModel(new SharingTableModel(sharedItems, fileList, tableFormat));
    }
}
