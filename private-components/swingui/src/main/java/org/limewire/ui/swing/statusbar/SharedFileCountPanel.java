package org.limewire.ui.swing.statusbar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class SharedFileCountPanel extends JXButton {
        
    @Inject
    SharedFileCountPanel(SharedFileListManager shareListManager) {
        super(I18n.tr("Sharing {0} files", shareListManager.getSharedFileCount()));
        
        setName("SharedFileCountPanel");
        
        shareListManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(SharedFileListManager.SHARED_FILE_COUNT)) {
                    setText(I18n.trn("Sharing {0} file", "Sharing {0} files", (Integer)evt.getNewValue()));
                    
                }
            }
        });
    }

}
