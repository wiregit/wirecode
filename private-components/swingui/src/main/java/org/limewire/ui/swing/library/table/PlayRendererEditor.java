package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class PlayRendererEditor extends TableRendererEditor implements AudioPlayerListener{

    private JToggleButton playButton;

    @Resource
    private Icon playIcon;

    @Resource
    private Icon pauseIcon;

    private File file;
    
    private JTable table;

    public PlayRendererEditor(JTable table, final AudioPlayer player) {
        GuiUtils.assignResources(this);
        
        this.table = table;
        
        
        playButton = new JToggleButton();
        playButton.setIcon(playIcon);
        playButton.setSelectedIcon(pauseIcon);
        playButton.setMargin(new Insets(0, 0, 0, 0));
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.setFocusPainted(false);
        playButton.setRolloverEnabled(true);
        playButton.setHideActionText(true);
        playButton.setBorder(null);
        playButton.setOpaque(false);
        playButton.addMouseListener(new ActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (file == null) {
                    throw new IllegalStateException("Can not play null file");
                } else if (PlayerUtils.isPlayableFile(file)){
                    if(player.isPlaying(file)){
                        player.pause();
                    } else if(player.isPaused(file)){
                        player.unpause();
                    } else {                        
                        player.loadSong(file);
                        player.playSong();
                    }
                } else {                
                    NativeLaunchUtils.launchFile(file);
                }
                update(file);
            }
        }));
        add(playButton);
        
        player.addAudioPlayerListener(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        update(value);
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        file = update(value);
        return this;
    }
    
    private File update(Object value){
        if(value instanceof LocalFileItem){
            File file = ((LocalFileItem)value).getFile();
            update(file);
            return file;
        } else {
            throw new IllegalStateException(value + " must be LocalFileItem");
        }
    }
    
    private void update(File file){
        playButton.setSelected(PlayerUtils.isPlaying(file));
    }

    @Override
    public void progressChange(int bytesread) {
        // do nothing        
    }

    @Override
    public void songOpened(Map<String, Object> properties) {
        if(table.isVisible()){
            table.repaint();
        }       
    }

    @Override
    public void stateChange(AudioPlayerEvent event) {
       if(table.isVisible()){
           table.repaint();
       }
    }

}
