package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;

public class AudioLibraryTable<T extends FileItem> extends LibraryTable<T> {

    public AudioLibraryTable(EventList<T> libraryItems) {
        super(libraryItems, new AudioTableFormat<T>());
        PlayRendererEditor playEditor = new PlayRendererEditor();
        setRowHeight(playEditor.getPreferredSize().height);
        getColumnModel().getColumn(AudioTableFormat.PLAY_COL).setMaxWidth(playEditor.getPreferredSize().width);
        getColumnModel().getColumn(AudioTableFormat.PLAY_COL).setCellEditor(playEditor);
        getColumnModel().getColumn(AudioTableFormat.PLAY_COL).setCellRenderer(new PlayRendererEditor());
        
    }

}
