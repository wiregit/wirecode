package org.limewire.ui.swing.library.table;

import java.util.Collections;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ImageTableFormat <T extends LocalFileItem> extends AbstractLibraryFormat<T> {

    static final int NAME_INDEX = 0;
    static final int SIZE_INDEX = 1;
    
    @Inject
    public ImageTableFormat() {
        super("LIBRARY_IMAGE_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_IMAGE_FILENAME", I18n.tr("Filename"), 100, true, true),
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_IMAGE_SIZE", I18n.tr("Size"), 278, true, true)
        });     
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject.getFileName();
        case SIZE_INDEX: return baseObject.getSize();
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        return Collections.emptyList();
    }
}
