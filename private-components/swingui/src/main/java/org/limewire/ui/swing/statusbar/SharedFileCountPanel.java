package org.limewire.ui.swing.statusbar;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class SharedFileCountPanel extends JXLabel {
    
    // TODO: These resources should not be necessary, however?
    @Resource private Font font;
    @Resource private Color foreground;
    
    @Inject
    SharedFileCountPanel(GnutellaConnectionManager connectionManager, ShareListManager shareListManager) {
        super(I18n.tr("Sharing {0} files", 0));
        
        GuiUtils.assignResources(this);
        
        this.setFont(this.font);
        this.setForeground(this.foreground);
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        
        shareListManager.getCombinedShareList().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                setText(I18n.tr("Sharing {0} files", listChanges.getSourceList().size()));
            }
        });
        
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
        
        this.setVisible(sharingVisible);
        
    }

}
