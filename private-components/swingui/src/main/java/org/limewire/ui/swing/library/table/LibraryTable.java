package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

public class LibraryTable<T extends FileItem> extends MouseableTable {
    
    private LibrarySharePanel librarySharePanel;
    private ShareTableRendererEditor shareEditor;
    private LibraryTableFormat<T> format;



    private TableColors tableColors;
    public LibraryTable(EventList<T> libraryItems, LibraryTableFormat<T> format) {
        super(new LibraryTableModel<T>(libraryItems, format));
        
        this.format = format;

        tableColors = new TableColors();
        setStripesPainted(true);
        setColumnControlVisible(true);
        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setHighlighters(tableColors.getEvenHighLighter(), tableColors.getOddHighLighter());
        setFillsViewportHeight(true);
        setDragEnabled(true);

    }
    
    public void enableSharing(LibrarySharePanel librarySharePanel) {
        this.librarySharePanel = librarySharePanel;
        shareEditor = new ShareTableRendererEditor(new ShareAction(I18n.tr("Share")));
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(shareEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new ShareTableRendererEditor(null));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(shareEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(shareEditor.getPreferredSize().width);
        setRowHeight(shareEditor.getPreferredSize().height);
        hideColumns();
    }
    
    public void enableDownloading(DownloadListManager downloadListManager){
        ArrayList<RemoteFileItem> downloadingList = new ArrayList<RemoteFileItem>();
        
        DownloadAction downloadAction = new DownloadAction(I18n.tr("download"), downloadListManager, this, downloadingList);
        
        setDoubleClickHandler(new LibraryDownloadDoubleClickHandler(downloadAction));
        
        LibraryDownloadRendererEditor downloadEditor = new LibraryDownloadRendererEditor(downloadAction, downloadingList);
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(downloadEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new LibraryDownloadRendererEditor(null, downloadingList));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(downloadEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(downloadEditor.getPreferredSize().width);
        setRowHeight(downloadEditor.getPreferredSize().height);
        hideColumns();        
    }
    
    public LibraryTableModel getLibraryTableModel(){
        return (LibraryTableModel)getModel();
    }
    //TODO: this is quite brittle.  only works if column indexes are in descending order - need to do it a different way
    protected void hideColumns(){
        int[] hiddenColumns = format.getDefaultHiddenColums();
        for (int i = 0; i < hiddenColumns.length; i++) {
            ((TableColumnExt) getColumnModel().getColumn(hiddenColumns[i])).setVisible(false);
        }
    }
    
    private class ShareAction extends AbstractAction {
        
        public ShareAction(String text){
            super(text);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            librarySharePanel.setFileItem((LocalFileItem) ((LibraryTableModel)getModel()).getElementAt(convertRowIndexToModel(getEditingRow())));
            librarySharePanel.show(shareEditor, getVisibleRect());
            shareEditor.cancelCellEditing();
        }
        
    }
    
    
    private static class LibraryDownloadDoubleClickHandler implements TableDoubleClickHandler {

        private DownloadAction action;

        public LibraryDownloadDoubleClickHandler(DownloadAction action) {
            this.action = action;
        }

        @Override
        public void handleDoubleClick(int row) {
            action.download(row);
        }

    }

    private static class DownloadAction extends AbstractAction {
        private DownloadListManager downloadListManager;
        private LibraryTable table;
        private ArrayList<RemoteFileItem> downloadingList;
        public DownloadAction(String text, DownloadListManager downloadListManager, LibraryTable table, ArrayList<RemoteFileItem> downloadingList){
            super(text);
            this.downloadingList = downloadingList;
            this.downloadListManager = downloadListManager;
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            download(table.convertRowIndexToModel(table.getEditingRow()));
        }
        
        public void download(int row) {
            RemoteFileItem file = (RemoteFileItem) ((LibraryTableModel) table.getModel()).getElementAt(row);
            try {
                downloadListManager.addDownload(file);
                downloadingList.add(file);
                TableCellEditor editor = table.getCellEditor();
                if (editor != null) {          
                   editor.cancelCellEditing();
                }
            } catch (SaveLocationException e) {
                throw new RuntimeException(e);
            }
        }
        
    }
}
