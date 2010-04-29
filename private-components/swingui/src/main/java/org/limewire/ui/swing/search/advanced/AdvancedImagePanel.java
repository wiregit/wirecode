package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced image search. */
class AdvancedImagePanel extends AdvancedPanel {
    
    public AdvancedImagePanel(Action enterKeyAction) {
        super(SearchCategory.IMAGE, enterKeyAction);
        addField(FilePropertyKey.NAME);
    }

}
