package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.player.api.AudioPlayer;

import ca.odell.glazedlists.EventList;

public class AudioLibraryTable<T extends FileItem> extends LibraryTable<T> {

    public AudioLibraryTable(EventList<T> libraryItems, AudioPlayer player) {
        super(libraryItems, new AudioTableFormat<T>());
        PlayRendererEditor playEditor = new PlayRendererEditor(this, player);
        setRowHeight(playEditor.getPreferredSize().height);
        getColumnModel().getColumn(AudioTableFormat.PLAY_COL).setMaxWidth(playEditor.getPreferredSize().width);
        getColumnModel().getColumn(AudioTableFormat.PLAY_COL).setCellEditor(playEditor);
        getColumnModel().getColumn(AudioTableFormat.PLAY_COL).setCellRenderer(new PlayRendererEditor(this, player));
        getColumnModel().getColumn(AudioTableFormat.LENGTH_COL).setCellRenderer(new TimeRenderer());
        
    }

}
