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
public class DocumentTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private IconManager iconManager;

    @Inject
    public DocumentTableFormat(IconManager iconManager) {
        this.iconManager = iconManager;
        columnNames = new String[] {
            "Icon", "Name", "Type", "Size", "Date Created",
            "Actions", "Relevance", "People with File", "Owner", "Author"
        };

        actionColumnIndex = 5;
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
            case 4: return get(PropertyKey.DATE_CREATED);
            case 5: return vsr;
            case 6: return get(PropertyKey.RELEVANCE);
            case 7: return ""; // people with file
            case 8: return ""; // owner
            case 9: return get(PropertyKey.AUTHOR);
            default: return null;
        }
    }
}