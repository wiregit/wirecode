package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

public class AudioLibraryTable<T extends FileItem> extends LibraryTable<T> {
    
    public AudioLibraryTable(EventList<T> libraryItems, AudioPlayer player, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        super(libraryItems, new AudioTableFormat<T>(), saveLocationExceptionHandler);
        
        getColumnModel().getColumn(AudioTableFormat.NAME_COL).setCellEditor(new PlayRendererEditor(this, player));
        getColumnModel().getColumn(AudioTableFormat.NAME_COL).setCellRenderer(new PlayRendererEditor(this, player));
    }
    
    @Override
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        super.setupCellRenderers(format);

        getColumnModel().getColumn(AudioTableFormat.LENGTH_COL).setCellRenderer(new TimeRenderer());
        getColumnModel().getColumn(AudioTableFormat.SIZE_COL).setCellRenderer(new FileSizeRenderer());
    }
}
