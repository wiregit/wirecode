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
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

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
        @Named("audio")Provider<PlayerMediator> audioPlayerProvider,
        @Named("video")Provider<PlayerMediator> videoPlayerProvider, 
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
            //make sure nothing else is playing
            PlayerUtils.stop();
            
            // Get first selected item.
            LocalFileItem fileItem = localFileItems.get(0);
            //TODO:  this could be cleaner
            if (SwingUiSettings.PLAYER_ENABLED.getValue() && PlayerUtils.isPlayableAudioFile(fileItem.getFile())) {
                // Set active playlist and play file item.
                audioPlayerProvider.get().setActivePlaylist(libraryNavProvider.get().getSelectedNavItem());
                audioPlayerProvider.get().play(fileItem);
            } else if (SwingUiSettings.VIDEO_PLAYER_ENABLED.getValue() && PlayerUtils.isPlayableVideoFile(fileItem.getFile(), categoryManager)) {
                videoPlayerProvider.get().play(fileItem);
            } else {    
                NativeLaunchUtils.safeLaunchFile(fileItem.getFile(), categoryManager);
            }
        }
    }
}
