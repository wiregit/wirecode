package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class PlayRendererEditor extends TableRendererEditor {

    private JToggleButton playButton;

    @Resource
    private Icon playIcon;

    @Resource
    private Icon pauseIcon;

    private File file;

    public PlayRendererEditor() {
        GuiUtils.assignResources(this);
        
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
                    PlayerUtils.play(file);
                } else {                
                    NativeLaunchUtils.launchFile(file);
                }
            }}
        ));
        add(playButton);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value instanceof LocalFileItem){
            file = ((LocalFileItem)value).getFile();
        } else {
            throw new IllegalStateException(value + " must be LocalFileItem");
        }
        //TODO: set button state based on whether or not file is playing
        return this;
    }

}
