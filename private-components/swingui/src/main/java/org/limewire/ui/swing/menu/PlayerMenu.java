package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.Action;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class PlayerMenu extends MnemonicMenu {
    @Inject
    public PlayerMenu(AudioPlayer audioPlayer,
            LibraryManager libraryManager) {
        super(I18n.tr("&Player"));

        add(getPlayPauseAction(audioPlayer));
    }

    private Action getPlayPauseAction(final AudioPlayer audioPlayer) {
        final String play = I18n.tr("Play");
        final String pause = I18n.tr("Pause");
        final Action action = new AbstractAction(play) {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (audioPlayer.getStatus()) {
                case PAUSED:
                case STOPPED:
                case OPENED:
                case SEEKING_PAUSED:
                    audioPlayer.unpause();
                    break;
                case PLAYING:
                case RESUMED:
                case SEEKING_PLAY:
                    audioPlayer.pause();
                    break;
                default:
                    //ignore
                    break;
                }
            }
        };
        
        action.setEnabled(false);

        audioPlayer.addAudioPlayerListener(new AudioPlayerListener() {
            @Override
            public void stateChange(AudioPlayerEvent event) {

                switch (audioPlayer.getStatus()) {
                case PAUSED:
                case STOPPED:
                case OPENED:
                case SEEKING_PAUSED:
                    action.setEnabled(true);
                    action.putValue(Action.NAME, play);
                    break;
                case PLAYING:
                case RESUMED:
                case SEEKING_PLAY:
                    action.setEnabled(true);
                    action.putValue(Action.NAME, pause);
                    break;
                default:
                    //ignore
                    break;
                }
            }
            @Override
            public void songOpened(Map<String, Object> properties) {
    
            }
            @Override
            public void progressChange(int bytesread) {
                
            } 
        });
        return action;
    }
}
