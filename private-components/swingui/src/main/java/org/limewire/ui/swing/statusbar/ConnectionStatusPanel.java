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

    private final SharedFileCountPanel countPanel;
    private final GnutellaConnectionManager connectionManager;
    
    private final JLabel qualityIcon;
    
//    @Resource private Icon noInternet;
//    @Resource private Icon disconnected;
//    @Resource private Icon poor;
//    @Resource private Icon satifactory;
//    @Resource private Icon good;
//    @Resource private Icon veryGood;
    @Resource private Icon excellent;
//    @Resource private Icon turbo;
    
    @Inject
    ConnectionStatusPanel(SharedFileCountPanel countPanel, GnutellaConnectionManager connectionManager) {
        GuiUtils.assignResources(this);
        
        this.countPanel = countPanel;
        this.connectionManager = connectionManager;
        
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        this.countPanel.setVisible(false);
        
        this.qualityIcon = new JLabel(excellent);
        
        this.add(this.qualityIcon);
        this.add(this.countPanel);
        
        this.connectionManager.addPropertyChangeListener(new PropertyChangeListener() {
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
        switch(strength) {
        case NO_INTERNET:
        case DISCONNECTED:
            ConnectionStatusPanel.this.countPanel.setVisible(false);
            break;
        case CONNECTING:
        case WEAK:
        case MEDIUM:
        case FULL:
        case TURBO:
            ConnectionStatusPanel.this.countPanel.setVisible(true);
            break;
        }
    }
}
