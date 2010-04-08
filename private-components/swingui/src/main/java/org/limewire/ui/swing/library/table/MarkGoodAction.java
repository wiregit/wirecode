package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.related.RelatedFiles;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class MarkGoodAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final RelatedFiles relatedFiles;

    @Inject
    public MarkGoodAction(
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            RelatedFiles relatedFiles) {
        super(I18n.tr("Mark as Good"));
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.relatedFiles = relatedFiles;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for(LocalFileItem item : selectedLocalFileItems.get()) {
            relatedFiles.markFileAsGood(item.getUrn());
        }
    }
}