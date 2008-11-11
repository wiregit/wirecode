package org.limewire.ui.swing.downloads.table;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 * No renderers or editors are set by default.
 */
public class DownloadTable extends AbstractDownloadTable {   
    
    //TODO: Make resource
    private int rowHeight = 60;    
    
    private DownloadTableModel model;

    @AssistedInject
	public DownloadTable(DownloadTableCellFactory tableCellFactory, PropertiesFactory<DownloadItem> propertiesFactory,
	        @Assisted EventList<DownloadItem> downloadItems) {		

        initialise(downloadItems, propertiesFactory);
        
        setShowGrid(false, false);
        
        DownloadTableCell editorMutator   = tableCellFactory.create();
        DownloadTableCell rendererMutator = tableCellFactory.create();
        
        DownloadTableEditor editor = new DownloadTableEditor(editorMutator);
        editor.initialiseEditor(downloadItems, propertiesFactory);
        getColumnModel().getColumn(0).setCellEditor(editor);
        
        DownloadTableRenderer renderer = new DownloadTableRenderer(rendererMutator);
        renderer.initialiseRenderer(editor.getEditorListener());
        getColumnModel().getColumn(0).setCellRenderer(renderer);
        
        setRowHeight(this.rowHeight);
    }
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}

    private void initialise(EventList<DownloadItem> downloadItems, PropertiesFactory<DownloadItem> propertiesFactory) {
        model = new DownloadTableModel(downloadItems);
        setModel(model);

        TablePopupHandler popupHandler = new DownloadPopupHandler(new DownloadActionHandler(downloadItems, propertiesFactory), this);

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                DownloadItem item = getDownloadItem(row);
                if(item.isLaunchable()) {
                    DownloadItemUtils.launch(item);
                }
            }
        };

        setDoubleClickHandler(clickHandler);

    }
}