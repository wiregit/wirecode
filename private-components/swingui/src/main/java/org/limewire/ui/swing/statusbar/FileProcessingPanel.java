package org.limewire.ui.swing.statusbar;

import org.jdesktop.swingx.JXLabel;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class FileProcessingPanel extends JXLabel {
    
    private int total;
    private int finished;
    private String processing;

    @Inject
    FileProcessingPanel() {
        super("");
        setName("FileProcessingPanel");
    }

    @Inject
    void register(LibraryManager libraryManager) {
        libraryManager.getLibraryManagedList().addFileProcessingListener(new EventListener<FileProcessingEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(final FileProcessingEvent event) {
                switch (event.getType()) {
                case QUEUED:
                    total++;
                    setNewText();
                    break;
                case FINISHED:
                    finished++;
                    if (finished == total) {
                        finished = 0;
                        total = 0;
                        processing = null;
                    }
                    setNewText();
                    break;
                case PROCESSING:
                    processing = event.getSource().getName();
                    setNewText();
                    break;
                }
            }
        });
    }

    private void setNewText() {
        if(total == 0) {
            setVisible(false);
        } else {
            if(processing != null) {
                // TODO: Pop this up to a something else.  Tooltip?
                setText(I18n.tr("Adding {0} of {1} - Processing {2}", finished, total, processing));
            } else {
                setText(I18n.tr("Adding {0} of {1}", finished, total));
            }
            setVisible(true);
        }
    }
    
}
