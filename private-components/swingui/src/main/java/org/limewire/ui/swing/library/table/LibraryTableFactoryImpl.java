package org.limewire.ui.swing.library.table;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
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
        
        LibraryTable<T> libTable;
        
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
            
        }
        
        return libTable;
        
    }

}
