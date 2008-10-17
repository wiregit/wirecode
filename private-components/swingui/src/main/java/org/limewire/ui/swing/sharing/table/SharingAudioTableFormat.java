package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for an audio file. If all the meta data is available then
 * the artist name, track title and album title are displayed. If not the 
 * filename is displayed instead. The last column is a list of actions that 
 * can operate on this fileItem.
 */
public class SharingAudioTableFormat extends AbstractTableFormat<LocalFileItem> {

    private static final int ARTIST_INDEX = 0;
    private static final int SONG_INDEX = 1;
    private static final int ALBUM_INDEX = 2;
    private static final int ACTIONS_INDEX = 3;
    
    public SharingAudioTableFormat() {
        super(I18n.tr("Artist"), I18n.tr("Song"), I18n.tr("Album"), "");
    }
    
    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        switch(column) {
            case ARTIST_INDEX:
                String name = (String) fileItem.getProperty(FileItem.Keys.AUTHOR);
                if(name != null)
                    return name;
                else 
                    return fileItem.getName();
            case SONG_INDEX: return fileItem.getProperty(FileItem.Keys.TITLE);
            case ALBUM_INDEX: return fileItem.getProperty(FileItem.Keys.ALBUM);
            case ACTIONS_INDEX: return fileItem;
        }
        throw new IllegalStateException("Unknown column:" + column);
    }
}
