package org.limewire.ui.swing.statusbar;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.painter.BarPainterFactory;
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
            ConnectionStatusPanel connectionStatus, SharedFileCountPanel sharedFileCountPanel,
            BarPainterFactory barPainterFactory, MinimizedDownloadSummaryPanel minDownloadPanel) {
        
        GuiUtils.assignResources(this);
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, fill, nogrid"));
        forceDefaultHeight(this);
        
        setBackgroundPainter(barPainterFactory.createStatusBarPainter());
 
        StatusBarSectionPainter<JComponent> sectionPainter = new StatusBarSectionPainter<JComponent>();
        sharedFileCountPanel.setBackgroundPainter(sectionPainter);
        
        MiniPlayerPanel miniPlayerPanel = new MiniPlayerPanel(player);
        miniPlayerPanel.setVisible(false);
        
        minDownloadPanel.setVisible(false);
        
        Component friendPanel = friendStatusPanel.getComponent();
        friendPanel.setVisible(false);
        forceDefaultHeight(friendPanel);
        
        add(connectionStatus, "growy, gapbefore 2, gaptop 2");
        add(sharedFileCountPanel, "growy, gaptop 2");
        add(minDownloadPanel, "growy, gapafter 4, hidemode 2, push");
        add(miniPlayerPanel, "gapafter 4");
        add(friendPanel, "gapbefore push, hidemode 2, dock east");
        
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
            
        case DISCONNECTED:
        case CONNECTING:
        case NO_INTERNET:
            
            sharingVisible = false;
            break;
            
        default:

            sharingVisible = true;
            break;
        }
        
        this.sharedFileCountPanel.setVisible(sharingVisible);
        
    }
    
    private void forceDefaultHeight(Component comp) {
        comp.setMinimumSize(new Dimension(0, height));
        comp.setMaximumSize(new Dimension((int)comp.getMaximumSize().getWidth(), height));
        comp.setPreferredSize(new Dimension((int)comp.getPreferredSize().getWidth(), height));
    }
}
