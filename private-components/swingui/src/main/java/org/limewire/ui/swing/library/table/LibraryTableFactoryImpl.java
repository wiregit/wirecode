package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileTransferable;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.EventList;

@Singleton
public class LibraryTableFactoryImpl implements LibraryTableFactory{

    private IconManager iconManager;

    @Inject
    public LibraryTableFactoryImpl(IconManager iconManager){
        this.iconManager = iconManager;
    }
    
    public <T extends FileItem>LibraryTable<T> createTable(Category category,
            EventList<T> eventList, Type type) {
        
        final LibraryTable<T> libTable;
        
        switch (category) {
        case AUDIO:
            if (type == Type.REMOTE) {
                libTable = new LibraryTable<T>(eventList, new RemoteAudioTableFormat<T>());
            } else {
                libTable = new AudioLibraryTable<T>(eventList);
            }
            break;
        case VIDEO:
            libTable = new VideoLibraryTable<T>(eventList);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(eventList, new DocumentTableFormat<T>());
            libTable.getColumnModel().getColumn(DocumentTableFormat.NAME_COL).setCellRenderer(new IconLabelRenderer(iconManager));
            break;
        case IMAGE:
            libTable = new LibraryTable<T>(eventList, new ImageTableFormat<T>());
            break;
        case OTHER:
            libTable = new LibraryTable<T>(eventList, new OtherTableFormat<T>());
            libTable.getColumnModel().getColumn(OtherTableFormat.NAME_COL).setCellRenderer(new IconLabelRenderer(iconManager));
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(eventList, new ProgramTableFormat<T>());
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        
        if(type == Type.REMOTE){
            
        } else {//Local            
            libTable.setTransferHandler(new TransferHandler(){
                @Override
                public int getSourceActions(JComponent comp) {
                    return COPY;
                }
                
                @Override
                public Transferable createTransferable(JComponent comp) {
                    int indices[] = libTable.getSelectedRows();
                    List<File> files = new ArrayList<File>();
                    for(int i = 0; i < indices.length; i++) {
                        LocalFileItem item = (LocalFileItem)((LibraryTableModel)libTable.getModel()).getFileItem(indices[i]);
                        files.add(item.getFile());
                    }
                    return new FileTransferable(files);
                }
            });
            libTable.setDropMode(DropMode.ON);
        }
        
        return libTable;
        
    }

}
