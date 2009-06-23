package org.limewire.ui.swing.warnings;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.util.CategoryUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Has logic one whether or not to display a warning to the user that they are
 * sharing documents with a public shared list. If the user shares a document
 * with a public shared list, then a DocumentWarningPanel will be constructed
 * and attached to the GlobalLayeredPanel. 
 */
@Singleton
public class DocumentWarningController implements ComponentListener {
    private final AtomicBoolean showing = new AtomicBoolean(false);

    private final Provider<DocumentWarningPanel> documentWarningPanel;

    @Inject
    public DocumentWarningController(Provider<DocumentWarningPanel> documentWarningPanel) {
        this.documentWarningPanel = documentWarningPanel;
    }

    @Inject
    public void register(SharedFileListManager sharedFileListManager) {
        if (SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.get()) {
            sharedFileListManager.getModel().getReadWriteLock().readLock().lock();
            try {
                for (SharedFileList shareList : sharedFileListManager.getModel()) {
                    if (shareList.isPublic()) {
                        shareList.getSwingModel().addListEventListener(
                                new ListEventListener<LocalFileItem>() {
                                    @Override
                                    public void listChanged(ListEvent<LocalFileItem> listChanges) {
                                        while (listChanges.next()) {
                                            if (listChanges.getType() == ListEvent.INSERT
                                                    || listChanges.getType() == ListEvent.UPDATE) {
                                                LocalFileItem localFileItem = listChanges
                                                        .getSourceList()
                                                        .get(listChanges.getIndex());
                                                if (CategoryUtils.getCategory(localFileItem
                                                        .getFile()) == Category.DOCUMENT
                                                        && SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD
                                                                .getValue()) {
                                                    showDocumentSharingWarning();
                                                }
                                            }
                                        }
                                    }
                                });
                    }
                }
            } finally {
                sharedFileListManager.getModel().getReadWriteLock().readLock().unlock();
            }
        }
    }

    private void showDocumentSharingWarning() {
        if (!showing.getAndSet(true)) {
            DocumentWarningPanel panel = documentWarningPanel.get();
            // component hidden event comes in to tell us we can show more
            // warnings.
            panel.addComponentListener(this);
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        showing.set(false);
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentResized(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

}
