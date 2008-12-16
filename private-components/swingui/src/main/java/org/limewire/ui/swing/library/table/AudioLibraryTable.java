package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

public class AudioLibraryTable<T extends LocalFileItem> extends LibraryTable<T> {
    private final PlayRendererEditor playEditor;
    private final PlayRendererEditor playRenderer;

    public AudioLibraryTable(EventList<T> libraryItems, AudioPlayer player, SaveLocationExceptionHandler saveLocationExceptionHandler, ShareTableRendererEditorFactory shareTableRendererEditorFactory) {
        super(libraryItems, new AudioTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);

        playEditor = new PlayRendererEditor(this, player);
        getColumnModel().getColumn(AudioTableFormat.Columns.PLAY.ordinal()).setCellEditor(playEditor);
        playRenderer = new PlayRendererEditor(this, player);
        getColumnModel().getColumn(AudioTableFormat.Columns.PLAY.ordinal()).setCellRenderer(playRenderer);
        getColumnModel().getColumn(AudioTableFormat.Columns.PLAY.ordinal()).setMaxWidth(14);
        getColumnModel().getColumn(AudioTableFormat.Columns.PLAY.ordinal()).setMinWidth(14);
    }

    @Override
    public void dispose() {
        super.dispose();
        playEditor.dispose();
        playRenderer.dispose();
    }

    @Override
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        super.setupCellRenderers(format);

        getColumnModel().getColumn(AudioTableFormat.Columns.LENGTH.ordinal()).setCellRenderer(new TimeRenderer());
        getColumnModel().getColumn(AudioTableFormat.Columns.SIZE.ordinal()).setCellRenderer(new FileSizeRenderer());
    }
}
