package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.Point;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;

public class DownloadTable extends MouseableTable {

    
    private DownloadRendererEditor editor;
    
    private DownloadTableModel model;
    


	public DownloadTable(EventList<DownloadItem> downloadItems) {
		super();
		
		model = new DownloadTableModel(downloadItems);
		setModel(model);
		
		setShowGrid(false, false);

		editor = new DownloadRendererEditor();
        editor.initializeEditor(downloadItems);
        getColumnModel().getColumn(0).setCellEditor(editor);
        
		DownloadRendererEditor renderer = new DownloadRendererEditor();		
		getColumnModel().getColumn(0).setCellRenderer(renderer);
		
		setRowHeight(renderer.getPreferredSize().height);

        TablePopupHandler popupHandler = new TablePopupHandler() {
            private int popupRow = -1;

            @Override
            public boolean isPopupShowing(int row) {
                return editor.isMenuVisible() && row == popupRow;
            }

            @Override
            public void maybeShowPopup(Component component, int x, int y) {
                editor.showPopupMenu(component, x, y);
                popupRow = rowAtPoint(new Point(x, y));
            }
        };

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                DownloadItem item = getDownloadItem(row);
                if (item.isLaunchable()) {
                    NativeLaunchUtils.launchFile(item.getFile());
                }
            }
        };

        setDoubleClickHandler(clickHandler);
    }
	
	
	private DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}

}
