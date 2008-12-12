package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Font;
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
import org.limewire.ui.swing.util.SwingUtils;

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
    @Resource private Icon weakPlus;
    @Resource private Icon medium;
    @Resource private Icon mediumPlus;
    @Resource private Icon full;
    @Resource private Icon turbo;
    @Resource private Font font;
    
    /**
     * Gaurds against hiding the status label by a sleeping hider thread 
     *  if the connection strength moves down
     */
    private boolean shouldStillHideStatusLabel;
    
    @Inject
    ConnectionStatusPanel(GnutellaConnectionManager connectionManager) {
        GuiUtils.assignResources(this);
       
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,3));
        
        this.connectionStrengthLabel = new JLabel();
        this.connectionStrengthLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        this.connectionStatusLabel = new JLabel();
        this.connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        this.connectionStatusLabel.setFont(font);
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

        shouldStillHideStatusLabel = false;
        
        String statusMessage = "";
        boolean shouldHideStatusLater = false;
        Icon strengthIcon = null;
        
        switch(strength) {
        case NO_INTERNET:
            statusMessage = I18n.tr("No Internet Connection");
            strengthIcon = noInternet;
            break;
        case DISCONNECTED:
            // TODO: disconnected visualisation and actions
            statusMessage = "";
            strengthIcon = disconnected;
            break;
        case CONNECTING:
            statusMessage = I18n.tr("Connecting...");
            strengthIcon = connecting;
            break;
        case WEAK:
            statusMessage = I18n.tr("Weak connection");
            strengthIcon = weak;
            break;
         case WEAK_PLUS:
            statusMessage = I18n.tr("Weak connection");
            strengthIcon = weakPlus;
            break;            
         case MEDIUM:
            statusMessage = I18n.tr("Medium connection");
            strengthIcon = medium; 
            break;
         case MEDIUM_PLUS:
             statusMessage = I18n.tr("Medium connection");
             strengthIcon = mediumPlus; 
             break;            
        case FULL:
            shouldHideStatusLater = true;
            statusMessage = I18n.tr("Full Connection");
            strengthIcon = full;
            break;
        case TURBO:
            shouldHideStatusLater = true;
            statusMessage = I18n.tr("Turbo charged Connection");
            strengthIcon = turbo; 
            break;
        }
        
       
        if (shouldHideStatusLater) {
            hideStatusLater();
        }
        
        connectionStatusLabel.setVisible(true);
        connectionStatusLabel.setText(statusMessage);
        connectionStrengthLabel.setIcon(strengthIcon);
    }
    
    private void hideStatusLater() {
        
        shouldStillHideStatusLabel = true;
        
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } 
                catch (InterruptedException e) {
                }
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (shouldStillHideStatusLabel) {
                            connectionStatusLabel.setVisible(false);
                        }
                    }
                });
            }
        }.start();
    }
}
