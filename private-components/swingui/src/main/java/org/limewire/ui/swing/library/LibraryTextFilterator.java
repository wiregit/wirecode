package org.limewire.ui.swing.library;

import java.util.List;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Keys;

import ca.odell.glazedlists.TextFilterator;

    public class LibraryTextFilterator<T extends FileItem> implements TextFilterator<T> {
        @Override
        public void getFilterStrings(
                List<String> list, T fileItem) {
            list.add(fileItem.getName());
            list.add(String.valueOf(fileItem.getSize()));            

         
            for (FileItem.Keys key : Keys.values()) {
                Object value = fileItem.getProperty(key);
                if(value != null) {
                    list.add(value.toString());
                }
            }
        }
    }