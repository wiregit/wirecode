package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;

public class VideoLibraryTable<T extends FileItem> extends LibraryTable<T> {

    public VideoLibraryTable(EventList<T> libraryItems) {
        super(libraryItems, new VideoTableFormat<T>());
//        PlayRendererEditor playEditor = new PlayRendererEditor();
//        setRowHeight(playEditor.getPreferredSize().height);
//        getColumnModel().getColumn(VideoTableFormat.PLAY_COL).setMaxWidth(playEditor.getPreferredSize().width);
//        getColumnModel().getColumn(VideoTableFormat.PLAY_COL).setCellEditor(playEditor);
//        getColumnModel().getColumn(VideoTableFormat.PLAY_COL).setCellRenderer(new PlayRendererEditor());
        getColumnModel().getColumn(VideoTableFormat.LENGTH_COL).setCellRenderer(new TimeRenderer());
        
    }

}
