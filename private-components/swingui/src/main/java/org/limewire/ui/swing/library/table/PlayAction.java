package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Plays the given file in the limewire player, 
 * or launches it depending on if it is supported.
 */
class PlayAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final Provider<PlayerMediator> playerProvider;
    
    @Inject
    public PlayAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems,
            Provider<PlayerMediator> playerProvider) {
        super(I18n.tr("Play Once"));

        this.selectedLocalFileItems = selectedLocalFileItems;
        this.playerProvider = playerProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        if (localFileItems.size() > 0) {
            // Get first selected item.
            LocalFileItem fileItem = localFileItems.get(0);
            
            if (SwingUiSettings.PLAYER_ENABLED.getValue() && PlayerUtils.isPlayableFile(fileItem.getFile())) {
                // Clear active playlist and play file item.
                playerProvider.get().setActivePlaylist(null);
                playerProvider.get().play(fileItem);
            } else {    
                NativeLaunchUtils.safeLaunchFile(fileItem.getFile());
            }
        }
    }
}