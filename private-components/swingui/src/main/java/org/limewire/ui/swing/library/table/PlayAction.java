package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Plays the given file in the limewire player, 
 * or launches it depending on if it is supported.
 */
class PlayAction extends AbstractAction {
    private final LibraryPanel libraryPanel;
    
    @Inject
    public PlayAction(LibraryPanel libraryPanel) {
        super(I18n.tr("Play"));

        this.libraryPanel = libraryPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = libraryPanel.getSelectedItems();
        if(localFileItems.size() > 0)
            PlayerUtils.playOrLaunch(localFileItems.get(0).getFile());
        //TODO: this should load the selected list as the current playlist
    }
}