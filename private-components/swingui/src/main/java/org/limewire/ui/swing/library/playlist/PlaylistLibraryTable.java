package org.limewire.ui.swing.library.playlist;

import javax.swing.ListSelectionModel;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFormat;
import org.limewire.ui.swing.library.table.PlayRendererEditor;
import org.limewire.ui.swing.library.table.ShareTableRendererEditorFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

/**
 * Defines a library table used by playlists.
 */
public class PlaylistLibraryTable<T extends LocalFileItem> extends LibraryTable<T> {
    private final PlayRendererEditor playEditor;
    private final PlayRendererEditor playRenderer;

    /**
     * Constructs a PlaylistLibraryTable with the specified list of items, 
     * table format, audio player, exception handler, and editor factory. 
     */
    public PlaylistLibraryTable(EventList<T> libraryItems,
            LibraryTableFormat<T> format, AudioPlayer player, 
            SaveLocationExceptionHandler saveLocationExceptionHandler, 
            ShareTableRendererEditorFactory shareTableRendererEditorFactory) {
        super(libraryItems, format, saveLocationExceptionHandler, shareTableRendererEditorFactory);
        
        // Change to single selection mode so that mouse press and drag starts
        // a drag operation.  For other selection modes, mouse press and drag
        // selects multiple cells, so a second mouse press would be needed to
        // start a drag operation (unless we install a workaround).
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        playEditor = new PlayRendererEditor(this, player);
        playRenderer = new PlayRendererEditor(this, player);
        
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setCellEditor(playEditor);
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setCellRenderer(playRenderer);
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setMaxWidth(14);
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setMinWidth(14);
    }

    @Override
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        super.setupCellRenderers(format);

        getColumnModel().getColumn(PlaylistTableFormat.LENGTH_INDEX).setCellRenderer(new TimeRenderer());
        getColumnModel().getColumn(PlaylistTableFormat.SIZE_INDEX).setCellRenderer(new FileSizeRenderer());
        getColumnModel().getColumn(PlaylistTableFormat.TITLE_INDEX).setCellRenderer(new NameRenderer());
    }

    @Override
    public void dispose() {
        super.dispose();
        playEditor.dispose();
        playRenderer.dispose();
    }
}
