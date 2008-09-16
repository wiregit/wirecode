package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Decides what data to display in the Audio Format
 */
public class SharingFancyAudioTableFormat implements TableFormat<LocalFileItem> {

    public static final String[] columnLabels = new String[] {I18n.tr("Artist"), I18n.tr("Song"), I18n.tr("Album"),""};
    
    @Override
    public int getColumnCount() {
        return columnLabels.length;
    }

    @Override
    public String getColumnName(int column) {
        if(column < 0 || column >= columnLabels.length)
            throw new IllegalStateException("Unknown column:" + column);

        return columnLabels[column];
    }

    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        if(column == 0) {
            String name = (String) fileItem.getProperty(FileItem.Keys.AUTHOR);
            if(name != null)
                return name;
            else 
                return fileItem.getName();
        }else if(column == 1) return fileItem.getProperty(FileItem.Keys.TITLE);
        else if(column == 2) return fileItem.getProperty(FileItem.Keys.ALBUM);
        else if(column == 3) return fileItem;
        
        throw new IllegalStateException("Unknown column:" + column);
    }

}
