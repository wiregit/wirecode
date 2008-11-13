package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class PlayerMenu extends JMenu {
    @Inject
    public PlayerMenu(final AudioPlayer audioPlayer, final Navigator navigator,
            final LibraryManager libraryManager) {
        super(I18n.tr("Player"));

        add(getPlayPause(audioPlayer));

        add(getNext(audioPlayer));
        add(getPrevious(audioPlayer));

        addSeparator();
        add(getShowCurrentFile(audioPlayer, navigator, libraryManager));
    }

    private Action getNext(final AudioPlayer audioPlayer) {
        //TODO need to have a notion of a playlist for this to work.
        Action action = new AbstractAction(I18n.tr("Next")) {;
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        };

        addAudioListener(audioPlayer, action);
        return action;
    }

    private Action getPrevious(final AudioPlayer audioPlayer) {
        Action action = new AbstractAction(I18n.tr("Previous")) {
          //TODO need to have a notion of a playlist for this to work.
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        };

        addAudioListener(audioPlayer, action);
        return action;
    }

    private Action getPlayPause(final AudioPlayer audioPlayer) {
        final String play = I18n.tr("Play");
        final String pause = I18n.tr("Pause");
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (audioPlayer.getStatus() == PlayerState.PAUSED) {
                    audioPlayer.playSong();
                } else {
                    //TODO handle more player states
                    audioPlayer.pause();
                }
            }
        };

        audioPlayer.addAudioPlayerListener(new AudioPlayerListener() {
            @Override
            public void stateChange(AudioPlayerEvent event) {
                if (event.getState() == PlayerState.PAUSED) {
                    action.putValue(Action.NAME, play);
                } else {
                    //TODO handle more player states
                    action.putValue(Action.NAME, pause);
                }
            }
            @Override
            public void songOpened(Map<String, Object> properties) {
    
            }
            @Override
            public void progressChange(int bytesread) {
                
            } 
        });
        
        if (audioPlayer.getStatus() == PlayerState.PAUSED) {
            action.putValue(Action.NAME, play);
        } else {
            //TODO handle more player states
            action.putValue(Action.NAME, pause);
        }
        
        //TODO disable depending on whether a playlist is loaded or not.. right now there is no notion of a play list
        return action;
    }

    private Action getShowCurrentFile(final AudioPlayer audioPlayer, final Navigator navigator,
            final LibraryManager libraryManager) {
        Action action = new AbstractAction(I18n.tr("Show current file")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                AudioSource currentSong = audioPlayer.getCurrentSong();
                if (currentSong != null) {
                    File currentFile = currentSong.getFile();
                    if (currentFile != null) {
                        final LocalFileItem localFileItem = libraryManager.getLibraryManagedList()
                                .getFileItem(currentFile);
                        if (localFileItem != null) {
                            Category category = localFileItem.getCategory();
                            NavItem navItem = navigator.getNavItem(NavCategory.LIBRARY,
                                    LibraryNavigator.NAME_PREFIX + category);
                            navItem.select(new NavSelectable<URN>() {
                                @Override
                                public URN getNavSelectionId() {
                                    return localFileItem.getUrn();
                                }
                            });
                        }
                    }
                }
            }
        };
        addAudioListener(audioPlayer, action);
        return action;
    }

    /**
     * Enable or disables MenuItem depending on players current state, and
     * attaches a listener to see state changes of the player.
     */
    private void addAudioListener(final AudioPlayer audioPlayer, final Action action) {
        //TODO remove and use playlist manager, next and previous should be available if there is a playlist.
        audioPlayer.addAudioPlayerListener(new AudioPlayerListener() {
            @Override
            public void stateChange(AudioPlayerEvent event) {
            }

            @Override
            public void progressChange(int bytesread) {
            }

            @Override
            public void songOpened(Map<String, Object> properties) {
            }
        });
    }
}
