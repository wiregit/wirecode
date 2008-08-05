package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.library.FileItem;

import com.limegroup.gnutella.gui.dnd.FileTransferable;

import ca.odell.glazedlists.EventList;

public class LibraryTable extends JXTable {

    public LibraryTable(EventList<FileItem> libraryItems) {
        super(new LibraryTableModel(libraryItems));
        
        setColumnControlVisible(true);
        
        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setHighlighters(HighlighterFactory.createSimpleStriping());
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
                    FileItem item = ((LibraryTableModel)table.getModel()).getFileItem(indices[i]);
                    files.add(item.getFile());
                }
                return new FileTransferable(files);
            }
        });
    }
}
