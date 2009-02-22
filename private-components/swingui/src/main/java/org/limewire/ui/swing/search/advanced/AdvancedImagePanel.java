package org.limewire.ui.swing.search.advanced;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

/** The panel for advanced image search. */
class AdvancedImagePanel extends AdvancedPanel {
    
    public AdvancedImagePanel() {
        super(SearchCategory.IMAGE);
        addField(I18n.tr("Name"), FilePropertyKey.NAME);
    }

}
