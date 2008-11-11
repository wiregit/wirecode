package org.limewire.ui.swing.statusbar;

import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.painter.StatusBarPainter;
import org.limewire.ui.swing.player.MiniPlayerPanel;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatusPanel extends JXPanel {
    
    @Resource private int height;

    @Inject
    public StatusPanel(AudioPlayer player, FriendStatusPanel friendStatusPanel, SharedFileCountPanel sharedFiles) {
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill"));
   
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
        
        setBackgroundPainter(new StatusBarPainter());
 
        MiniPlayerPanel miniPlayerPanel = new MiniPlayerPanel(player);
        miniPlayerPanel.setVisible(false);
        
        add(sharedFiles, "gapafter push");
        
        add(miniPlayerPanel, "gapbefore push");        
        add(friendStatusPanel.getComponent(), "gapbefore push");
    }
}
