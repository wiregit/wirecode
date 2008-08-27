package org.limewire.ui.swing.search.resultpanel;

import com.google.inject.Inject;
import javax.swing.Icon;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.MediaType;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private IconManager iconManager;

    @Inject
    public AllTableFormat(IconManager iconManager) {
        this.iconManager = iconManager;

        columnNames = new String[] {
            "Icon", "Name", "Type", "Size", "Actions",
            "Relevance", "People with File", "Owner"
        };

        actionColumnIndex = 4;
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        Icon icon = null;
        String fileExtension = vsr.getFileExtension();
        String type = null;
        
        if (index == 0) {
            icon = iconManager.getIconForExtension(fileExtension);
            if (icon == null) {
                System.out.println(
                    "AllTableFormat.getColumnValue: no icon for extension " +
                    fileExtension);
            }
        } else if (index == 2) {
            MediaType mediaType =
                mediaType = MediaType.getMediaTypeForExtension(fileExtension);
            type = mediaType == null ? fileExtension : mediaType.toString();
            // TODO: RMV improve type text
        }

        switch (index) {
            case 0: return icon == null ? "none" : icon;
            case 1: return get(PropertyKey.NAME);
            case 2: return type;
            case 3: return vsr.getSize();
            case 4: return vsr;
            case 5: return get(PropertyKey.RELEVANCE);
            case 6: return ""; // people with file
            case 7: return ""; // owner
            default: return null;
        }
    }
}