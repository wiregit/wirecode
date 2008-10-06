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
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileTransferable;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.table.MouseableTable;
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
    
    public void enableDownloading(){
        //TODO:set action column for friend tables
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
}
