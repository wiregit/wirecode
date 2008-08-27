package org.limewire.ui.swing.search.resultpanel;

import com.google.inject.Inject;
import javax.swing.Icon;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class OtherTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private IconManager iconManager;

    @Inject
    public OtherTableFormat(IconManager iconManager) {
        this.iconManager = iconManager;
        columnNames = new String[] {
        "Icon", "Name", "Type", "Size", "Actions",
        "Relevance", "People with File", "Number of Files"
        };

        actionColumnIndex = 4;
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();
        Icon icon = iconManager.getIconForExtension(fileExtension);

        switch (index) {
            case 0: return icon;
            case 1: return get(PropertyKey.NAME);
            case 2: return fileExtension; // TODO: RMV improve
            case 3: return vsr.getSize();
            case 4: return vsr;
            case 5: return get(PropertyKey.RELEVANCE);
            case 6: return ""; // people with file
            case 7: return ""; // owner
            case 8: return vsr.getCoreSearchResults().size();
            default: return null;
        }
    }
}