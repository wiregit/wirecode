package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileList;

/**
 * Table format for the Document Table when it is in Sharing View
 */
public class SharedDocumentTableFormat<T extends FileItem> extends DocumentTableFormat<T> {
    private final LocalFileList localFileList;
    
    public SharedDocumentTableFormat(LocalFileList localFileList) {
        this.localFileList = localFileList;
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
            case ACTION_COL:
                return new CheckBoxComparator();
        }
        return super.getColumnComparator(column);
    }
    
    /**
     * Creates a Comparator for sorting checkboxs.
     */
    private class CheckBoxComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {
            boolean isShared1 = localFileList.contains(o1.getUrn());
            boolean isShared2 = localFileList.contains(o2.getUrn());

            if(isShared1 && isShared2) {
                return 0;
            } else if(isShared1 && !isShared2) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}