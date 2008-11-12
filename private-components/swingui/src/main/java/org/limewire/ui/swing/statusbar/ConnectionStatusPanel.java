package org.limewire.ui.swing.statusbar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;


class ConnectionStatusPanel extends JXPanel {

    private final SharedFileCountPanel sharedFileCountPanel;
    
    private final JLabel connectionStrengthPanel;
    
//    @Resource private Icon noInternet;
//    @Resource private Icon disconnected;
    @Resource private Icon connecting;
//    @Resource private Icon weak;
//    @Resource private Icon medium;
    @Resource private Icon full;
//    @Resource private Icon turbo;
    
    @Inject
    ConnectionStatusPanel(SharedFileCountPanel countPanel, GnutellaConnectionManager connectionManager) {
        GuiUtils.assignResources(this);
        
        this.sharedFileCountPanel = countPanel;
        
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        this.sharedFileCountPanel.setVisible(false);        
        this.connectionStrengthPanel = new JLabel();
        
        this.add(this.connectionStrengthPanel);
        this.add(this.sharedFileCountPanel);
        
        connectionManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    setConnectionStrength((ConnectionStrength)evt.getNewValue());
                }
            }
        });
        setConnectionStrength(connectionManager.getConnectionStrength());
    }
    
    private void setConnectionStrength(ConnectionStrength strength) {
        System.out.println("New Connection Strength: " + strength);
        boolean sharingVisible = false;
        Icon strengthIcon = null;
        switch(strength) {
        case NO_INTERNET:
            sharingVisible = false;
            strengthIcon = connecting; // CHANGEME.
            break;
        case DISCONNECTED:
            sharingVisible = false;
            strengthIcon = connecting; // CHANGEME.
            break;
        case CONNECTING:
            sharingVisible = true;
            strengthIcon = connecting;
            break;
        case WEAK:
            sharingVisible = true;
            strengthIcon = connecting; // CHANGEME.
            break;
        case MEDIUM:
            sharingVisible = true;
            strengthIcon = connecting; // CHANGEME.
            break;            
        case FULL:
            sharingVisible = true;
            strengthIcon = full;
            break;
        case TURBO:
            sharingVisible = true;
            strengthIcon = full; // CHANGEME.
            break;
        }
        
        sharedFileCountPanel.setVisible(sharingVisible);
        connectionStrengthPanel.setIcon(strengthIcon);
    }
}
