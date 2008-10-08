package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileTransferable;
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
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setHighlighters(tableColors.getEvenHighLighter(), tableColors.getOddHighLighter());
        setFillsViewportHeight(true);
        setDragEnabled(true);
        
        setTransferHandler(new TransferHandler(){
            @Override
            public int getSourceActions(JComponent comp) {
                return COPY;
            }
            
            @Override
            public Transferable createTransferable(JComponent comp) {
                int indices[] = getSelectedRows();
                List<File> files = new ArrayList<File>();
                for(int i = 0; i < indices.length; i++) {
                    LocalFileItem item = (LocalFileItem)((LibraryTableModel)getModel()).getFileItem(indices[i]);
                    files.add(item.getFile());
                }
                return new FileTransferable(files);
            }
        });
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
        DownloadAction downloadAction = new DownloadAction(I18n.tr("download"), downloadListManager, this);
        
        setDoubleClickHandler(new LibraryDownloadDoubleClickHandler(downloadAction));
        
        LibraryDownloadRendererEditor downloadEditor = new LibraryDownloadRendererEditor(downloadAction);
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(downloadEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new LibraryDownloadRendererEditor(null));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(downloadEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(downloadEditor.getPreferredSize().width);
        setRowHeight(downloadEditor.getPreferredSize().height);
        hideColumns();        
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
        //TODO: remove @SuppressWarnings and use this when patch is ready
        @SuppressWarnings("unused")
        private DownloadListManager downloadListManager;
        private LibraryTable table;
        public DownloadAction(String text, DownloadListManager downloadListManager, LibraryTable table){
            super(text);
            this.downloadListManager = downloadListManager;
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            download(table.convertRowIndexToModel(table.getEditingRow()));
        }
        
        public void download(int row){
            RemoteFileItem file = (RemoteFileItem) ((LibraryTableModel)table.getModel()).getElementAt(row);
//          downloadListManager.addDownload(file.getRfd());
          throw new RuntimeException("download me! " + file.getName());
        }
        
    }
}
