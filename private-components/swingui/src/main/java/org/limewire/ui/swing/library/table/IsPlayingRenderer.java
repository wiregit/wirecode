package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class IsPlayingRenderer extends DefaultLibraryRenderer {

    @Resource Icon playingIcon;
    
    private final Provider<PlayerMediator> playerMediator;
    
    @Inject
    public IsPlayingRenderer(Provider<PlayerMediator> playerMediator) {
        GuiUtils.assignResources(this);
        
        this.playerMediator = playerMediator;
        

    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
        setText("");
        
        if(value instanceof LocalFileItem) {
            if(playerMediator.get().isPlaying(((LocalFileItem)value).getFile()))
                setIcon(playingIcon);
            else
                setIcon(null);
        } else {
            setIcon(null);
        }
        
        return this;
    }
}
