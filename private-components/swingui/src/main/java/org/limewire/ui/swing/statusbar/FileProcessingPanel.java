package org.limewire.ui.swing.statusbar;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.FileProcessingListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

class FileProcessingPanel extends JXButton {

    @Inject
    FileProcessingPanel() {
        super("");
        setName("FileProcessingPanel");
    }

    @Inject
    void register(LibraryManager libraryManager) {
        libraryManager.getLibraryManagedList().addFileProcessingListener(
                new FileProcessingListener() {
                    @Override
                    public void handleEvent(final FileProcessingEvent event) {

                        SwingUtils.invokeLater(new Runnable() {
                            public void run() {
                                if (event.getType() == FileProcessingEvent.Type.FILE_PROCESSED) {
                                    if (event.getSize() > 1) {
                                        setText(I18n.tr("Adding : {0}", event.getFile().getName()));
                                    } else {
                                        setText(I18n.tr("Adding {0} of {1} : {2}",
                                                event.getIndex(), event.getSize(), event.getFile()
                                                        .getName()));
                                    }
                                } else if (event.getType() == FileProcessingEvent.Type.FINISHED) {
                                    setText("");
                                }
                            };
                        });
                    }
                });
    }
}
