package org.limewire.ui.swing.library.table;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.StripedJXTable;

import ca.odell.glazedlists.EventList;


public class LibraryTable<T extends FileItem> extends StripedJXTable {

    public LibraryTable(EventList<T> libraryItems) {
        super(new LibraryTableModel<T>(libraryItems));
        
        setColumnControlVisible(true);
        
        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setHighlighters(HighlighterFactory.createSimpleStriping());
        setFillsViewportHeight(true);
        setDragEnabled(true);
        
//        setTransferHandler(new TransferHandler(){
//            @Override
//            public int getSourceActions(JComponent comp) {
//                return COPY;
//            }
//            
//            @Override
//            public Transferable createTransferable(JComponent comp) {
//                JXTable table = (JXTable) comp;
//                int indices[] = table.getSelectedRows();
//                List<File> files = new ArrayList<File>();
//                for(int i = 0; i < indices.length; i++) {
//                    FileItem item = ((LibraryTableModel)table.getModel()).getFileItem(indices[i]);
//                    files.add(item.getFile());
//                }
//                return new FileTransferable(files);
//            }
//        });
    }
}
