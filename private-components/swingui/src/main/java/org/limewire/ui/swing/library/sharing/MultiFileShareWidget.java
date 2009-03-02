package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.MultiFileShareModel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class MultiFileShareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel sharePanel;
    private LocalFileItem[] files;
    private ShareListManager shareListManager;
    
    public MultiFileShareWidget(ShareListManager shareListManager, 
            ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog, XMPPService xmppService) {
        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog, xmppService);
    }
    
    public MultiFileShareWidget(ShareListManager shareListManager, 
            ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog, XMPPService xmppService, 
            boolean canShowP2P) {
        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog, xmppService, canShowP2P);
    }
    
    @Override
    public void show(Component c) {
        sharePanel.show(null, new MultiFileShareModel(shareListManager, files));
    }
    
    @Override
    public void setShareable(LocalFileItem[] files){
        this.files = files;
        // {0} number of files, {1}: type of file (Image, Document...)
        sharePanel.setTitleLabel(I18n.trn("Share {0} {1} file", "Share {0} {1} files", files.length, files.length, files[0].getCategory().getSingularName()));
        // {0}: number of files 
        sharePanel.setTopLabel(I18n.trn("Shared {0} file with:", "Shared {0} files with:", files.length));
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
