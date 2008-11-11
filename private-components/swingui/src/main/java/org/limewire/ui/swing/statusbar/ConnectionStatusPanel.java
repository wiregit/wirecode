package org.limewire.ui.swing.statusbar;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionLifeCycleEventType;
import org.limewire.core.api.connection.ConnectionLifeCycleListener;
import org.limewire.core.api.connection.ConnectionManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;


class ConnectionStatusPanel extends JXPanel implements ConnectionLifeCycleListener {

    private final SharedFileCountPanel countPanel;
    private final ConnectionManager connectionManager;
    
    private final JLabel qualityIcon;
    
    @Resource private Icon noInternet;
    @Resource private Icon disconnected;
    @Resource private Icon poor;
    @Resource private Icon satifactory;
    @Resource private Icon good;
    @Resource private Icon veryGood;
    @Resource private Icon excellent;
    @Resource private Icon turbo;
    
    @Inject
    ConnectionStatusPanel(SharedFileCountPanel countPanel, ConnectionManager connectionManager) {
        GuiUtils.assignResources(this);
        
        this.countPanel = countPanel;
        this.connectionManager = connectionManager;
        
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        this.countPanel.setVisible(false);
        
        this.qualityIcon = new JLabel(excellent);
        
        this.add(this.qualityIcon);
        this.add(this.countPanel);
        
        this.connectionManager.addEventListener(this);
    }

    @Override
    public void handleEvent(ConnectionLifeCycleEventType type) {
        if (this.connectionManager.isConnected()) {
            this.countPanel.setVisible(true);
        }
        
        
    }    
}
