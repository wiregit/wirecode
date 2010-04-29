package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced program search. */
class AdvancedProgramPanel extends AdvancedPanel {
    
    public AdvancedProgramPanel(Action enterKeyAction) {
        super(SearchCategory.PROGRAM, enterKeyAction);
        addField(FilePropertyKey.NAME);
    }

}
