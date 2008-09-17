package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileTransferable;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;

import ca.odell.glazedlists.EventList;



public class LibraryTable<T extends FileItem> extends MouseableTable {
    
    private LibrarySharePanel librarySharePanel;
    private MultiButtonTableCellRendererEditor shareEditor;


    private TableColors tableColors;
    
    public LibraryTable(EventList<T> libraryItems) {
        super(new LibraryTableModel<T>(libraryItems));
                
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
                JXTable table = (JXTable) comp;
                int indices[] = table.getSelectedRows();
                List<File> files = new ArrayList<File>();
                for(int i = 0; i < indices.length; i++) {
                    LocalFileItem item = (LocalFileItem)((LibraryTableModel)table.getModel()).getFileItem(indices[i]);
                    files.add(item.getFile());
                }
                return new FileTransferable(files);
            }
        });
    }
    
    public void enableSharing(LibrarySharePanel librarySharePanel){
        this.librarySharePanel = librarySharePanel;
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(new ShareAction("+"));
        shareEditor = new MultiButtonTableCellRendererEditor(actionList);
        getColumnModel().getColumn(LibraryTableModel.SHARE_COL).setCellEditor(shareEditor);
        getColumnModel().getColumn(LibraryTableModel.SHARE_COL).setCellRenderer(new MultiButtonTableCellRendererEditor(actionList));
        getColumnModel().getColumn(LibraryTableModel.SHARE_COL).setPreferredWidth(shareEditor.getPreferredSize().width);
        getColumnModel().getColumn(LibraryTableModel.SHARE_COL).setWidth(shareEditor.getPreferredSize().width);
    }
    
    private class ShareAction extends AbstractAction {
        
        public ShareAction(String text){
            super(text);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            librarySharePanel.setFileItem((LocalFileItem) ((LibraryTableModel)getModel()).getElementAt(convertRowIndexToModel(getEditingRow())));
            //adjust y for header height
            int headerAdjust = 0;
            if(getTableHeader() != null && getTableHeader().isVisible()){
                headerAdjust = getTableHeader().getHeight();
                if(getParent() instanceof JViewport){
                    headerAdjust -= ((JViewport)getParent()).getViewPosition().y;
                }
            }
            librarySharePanel.show(shareEditor, 0, headerAdjust);       
            shareEditor.cancelCellEditing();
        }
        
    }
}
