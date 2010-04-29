package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced document search. */
class AdvancedDocumentPanel extends AdvancedPanel {
    
    public AdvancedDocumentPanel(Action enterKeyAction) {
        super(SearchCategory.DOCUMENT, enterKeyAction);
        addField(FilePropertyKey.NAME);
    }

}
