package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class ConnectionStatusPanel extends JXPanel {
   
    private final JLabel connectionStrengthLabel;
    private final JLabel connectionStatusLabel;
    
    @Resource private Icon noInternet;
    @Resource private Icon disconnected;
    @Resource private Icon connecting;
    @Resource private Icon weak;
    @Resource private Icon medium;
    @Resource private Icon full;
    @Resource private Icon turbo;
    
    @Inject
    ConnectionStatusPanel(GnutellaConnectionManager connectionManager) {
        GuiUtils.assignResources(this);
       
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        this.connectionStrengthLabel = new JLabel();
        this.connectionStrengthLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        this.connectionStatusLabel = new JLabel(I18n.tr("Connecting..."));
        this.connectionStatusLabel.setVisible(false);
        this.connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        this.connectionStatusLabel.setFont(this.getFont());
        this.connectionStatusLabel.setForeground(this.getForeground());
        
        this.add(this.connectionStrengthLabel,BorderLayout.WEST);
        this.add(this.connectionStatusLabel,BorderLayout.CENTER);
                
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

        boolean connectingVisible = false;
        Icon strengthIcon = null;
        
        switch(strength) {
        case NO_INTERNET:
            connectingVisible = false;
            strengthIcon = noInternet;
            break;
        case DISCONNECTED:
            connectingVisible = false;
            strengthIcon = disconnected;
            break;
        case CONNECTING:
            connectingVisible = true;
            strengthIcon = connecting;
            break;
        case WEAK:
            connectingVisible = false;
            strengthIcon = weak;
            break;
        case MEDIUM:
            connectingVisible = false;
            strengthIcon = medium; 
            break;            
        case FULL:
            connectingVisible = false;
            strengthIcon = full;
            break;
        case TURBO:
            connectingVisible = false;
            strengthIcon = turbo; 
            break;
        }
        
        connectionStatusLabel.setVisible(connectingVisible);
        connectionStrengthLabel.setIcon(strengthIcon);
    }
}
