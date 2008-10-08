package org.limewire.ui.swing.library.table;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;

public class LibraryTableFactory {
    
    public static enum Type {REMOTE, LOCAL};
    
    public static <T extends FileItem>LibraryTable<T> createTable(Category category,
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
            break;
        case IMAGE:
            libTable = new LibraryTable<T>(eventList, new ImageTableFormat<T>());
            break;
        case OTHER:
            libTable = new LibraryTable<T>(eventList, new OtherTableFormat<T>());
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(eventList, new ProgramTableFormat<T>());
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        
        return libTable;
        
    }
}
