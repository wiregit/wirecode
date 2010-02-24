package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryInspectionUtils;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.player.Audio;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.player.Video;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Tries to safely launch the given file.
 * If it is an allowed file type it will be launched, 
 * otherwise explorer will be opened to the files location
 */
class LaunchFileAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final Provider<LibraryNavigatorPanel> libraryNavProvider;
    private final Provider<PlayerMediator> audioPlayerProvider;
    private final Provider<PlayerMediator> videoPlayerProvider;
    private final CategoryManager categoryManager;
    
    @Inject
    public LaunchFileAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems,
        Provider<LibraryNavigatorPanel> libraryNavProvider,
        @Audio Provider<PlayerMediator> audioPlayerProvider,
        @Video Provider<PlayerMediator> videoPlayerProvider, 
        CategoryManager categoryManager) {
        super(I18n.tr("Play/Open"));

        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryNavProvider = libraryNavProvider;
        this.audioPlayerProvider = audioPlayerProvider;
        this.videoPlayerProvider = videoPlayerProvider;
        this.categoryManager = categoryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        if (localFileItems.size() > 0) {
            LibraryInspectionUtils.fileLaunched();
            
            // Get first selected item.
            LocalFileItem fileItem = localFileItems.get(0);
            //TODO:  this could be cleaner
            if (SwingUiSettings.PLAYER_ENABLED.getValue() && PlayerUtils.isPlayableAudioFile(fileItem.getFile())) {
                // Set active playlist and play file item.
                audioPlayerProvider.get().setActivePlaylist(libraryNavProvider.get().getSelectedNavItem());
                audioPlayerProvider.get().play(fileItem);
            } else if (SwingUiSettings.PLAYER_ENABLED.getValue() && PlayerUtils.isPlayableVideoFile(fileItem.getFile(), categoryManager)) {
                //make sure nothing else is playing
                PlayerUtils.stop();
                videoPlayerProvider.get().play(fileItem);
            } else {    
                NativeLaunchUtils.safeLaunchFile(fileItem.getFile(), categoryManager);
            }
        }
    }
}
