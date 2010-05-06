package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;

public class StatusPanel extends JXPanel {
    
    @Resource private int height;

    private final SharedFileCountPanel sharedFileCountPanel;
    
    @Inject
    public StatusPanel(GnutellaConnectionManager connectionManager,
            ConnectionStatusPanel connectionStatus, 
            ProStatusPanel proStatusPanel, SharedFileCountPanel sharedFileCountPanel, 
            BarPainterFactory barPainterFactory, FileProcessingPanel fileProcessingPanel) {
        
        GuiUtils.assignResources(this);
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        
        setLayout(new BorderLayout());
        ResizeUtils.forceHeight(this, height);
        
        setBackgroundPainter(barPainterFactory.createStatusBarPainter());
 
        JPanel leftPanel = new JPanel(new MigLayout("insets 0, gap 0, filly, nogrid, hidemode 3"));
        JPanel centerPanel = new JPanel(new MigLayout("insets 0, gap 0, filly, nogrid, alignx 40%, hidemode 3"));
        
        leftPanel.setOpaque(false);
        centerPanel.setOpaque(false);
        
        leftPanel.add(connectionStatus, "growy, gapbefore 2, gaptop 2, gapbottom 2");
        leftPanel.add(sharedFileCountPanel, "growy, gaptop 0, gapbottom 0");
        leftPanel.add(fileProcessingPanel, "growy, gaptop 0, gapbottom 0, pad 0 -7 0 0");
        
        centerPanel.add(proStatusPanel, "growy, gaptop 2");
        
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        
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
}
