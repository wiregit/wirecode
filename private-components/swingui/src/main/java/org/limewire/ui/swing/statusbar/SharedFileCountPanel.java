package org.limewire.ui.swing.statusbar;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class SharedFileCountPanel extends JXButton {
        
    @Inject
    SharedFileCountPanel(SharedFileListManager shareListManager) {
        super(I18n.tr("Sharing {0} files", "????"));
        
        this.setName("SharedFileCountPanel");
        
//        shareListManager.getCombinedShareList().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
//            @Override
//            public void listChanged(ListEvent<LocalFileItem> listChanges) {
//                setText(I18n.trn("Sharing {0} file", "Sharing {0} files", listChanges.getSourceList().size()));
//            }
//        });
        
    }

}
