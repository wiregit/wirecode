package org.limewire.ui.swing.sharing.table;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.sharing.menu.SharingActionHandler;
import org.limewire.ui.swing.sharing.menu.SharingPopupHandler;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;

/**
 * A table for displaying shared files.
 */
public class SharingTable extends MouseableTable {

    private SharingTableModel sharingTableModel;
    
    private TableColors tableColors;
    
    public SharingTable(EventList<LocalFileItem> sharedItems, LocalFileList fileList, TableFormat<LocalFileItem> tableFormat) {

        initialize(sharedItems, fileList, tableFormat);
        
        tableColors = new TableColors();
        
        setColumnControlVisible(false);
        setShowGrid(false, false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(false);
        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);
        setHighlighters( new ColorHighlighter(HighlightPredicate.ALWAYS, tableColors.evenColor, tableColors.evenForeground, tableColors.selectionColor, tableColors.selectionForeground));        
    }
    
    public LocalFileItem getFileItem(int row) {
        return sharingTableModel.getFileItem(row);
    }
    
    private void initialize(EventList<LocalFileItem> sharedItems, LocalFileList fileList, TableFormat<LocalFileItem> tableFormat) {
        sharingTableModel = new SharingTableModel(sharedItems, fileList, tableFormat);
        setModel(sharingTableModel);
        
        final TableDoubleClickHandler doubleClickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                if( row >= 0 && row < getModel().getRowCount()) {
                    LocalFileItem item = getFileItem(row);
                    if(PlayerUtils.isPlayableFile(item.getFile())) {
                        PlayerUtils.play(item.getFile());
                    } else {
                        NativeLaunchUtils.launchFile(item.getFile());
                    }
                }
            }
        };

        setDoubleClickHandler(doubleClickHandler);
        
        final SharingPopupHandler popupHandler = new SharingPopupHandler(this, new SharingActionHandler());
        
        setPopupHandler(popupHandler);
    }
}
