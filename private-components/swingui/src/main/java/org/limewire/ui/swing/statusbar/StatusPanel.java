package org.limewire.ui.swing.statusbar;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.painter.StatusBarPainter;
import org.limewire.ui.swing.painter.StatusBarSectionPainter;
import org.limewire.ui.swing.player.MiniPlayerPanel;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatusPanel extends JXPanel {
    
    @Resource private int height;

    private final SharedFileCountPanel sharedFileCountPanel;
    
    @Inject
    public StatusPanel(GnutellaConnectionManager connectionManager, AudioPlayer player, 
            FriendStatusPanel friendStatusPanel, 
            ConnectionStatusPanel connectionStatus, SharedFileCountPanel sharedFileCountPanel) {
        
        GuiUtils.assignResources(this);
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill"));
        setMinimumSize(new Dimension(0, height + 2));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height + 2));
        setPreferredSize(new Dimension(Short.MAX_VALUE, height + 2));
        setBackgroundPainter(new StatusBarPainter());
 
        StatusBarSectionPainter<JComponent> sectionPainter = new StatusBarSectionPainter<JComponent>();
        connectionStatus.setBackgroundPainter(sectionPainter);
        sharedFileCountPanel.setBackgroundPainter(sectionPainter);
        
        MiniPlayerPanel miniPlayerPanel = new MiniPlayerPanel(player);
        miniPlayerPanel.setVisible(false);
        
        add(connectionStatus, "growy, gapafter 4");
        add(sharedFileCountPanel, "growy");
        add(miniPlayerPanel, "");
        
        add(new JLabel(), "gapafter push, growx");
        
        add(friendStatusPanel.getComponent(), "gapbefore push");
        
        connectionManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    updateComponents((ConnectionStrength)evt.getNewValue());
                }
            }
        });
        updateComponents(connectionManager.getConnectionStrength());
    }
    
    private void updateComponents(ConnectionStrength strength) {
        
        boolean sharingVisible = false;
        
        switch(strength) {
            
        case WEAK:
        case MEDIUM:
        case FULL:
        case TURBO:
            
            sharingVisible = true;
            break;
            
        default:

            sharingVisible = false;
            break;
        }
        
        this.sharedFileCountPanel.setVisible(sharingVisible);
        
    }
}
