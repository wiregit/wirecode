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

    private static final int ICON_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 6;
    private static final int RELEVANCE_INDEX = 5;
    private static final int SIZE_INDEX = 3;

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
    public Class getColumnClass(int index) {
        return index == ICON_INDEX ? Icon.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == SIZE_INDEX ? Long.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case 0: return getIcon(vsr);
            case 1: return getProperty(PropertyKey.NAME);
            case 2: return getMediaType(vsr);
            case 3: return vsr.getSize();
            case 4: return vsr;
            case 5: return getProperty(PropertyKey.RELEVANCE);
            case 6: return vsr.getSources().size();
            case 7: return getProperty(PropertyKey.OWNER);
            default: return null;
        }
    }

    private Object getIcon(VisualSearchResult vsr) {
        String ext = vsr.getFileExtension();
        Icon icon = iconManager.getIconForExtension(ext);
        return icon == null ? "none" : icon;
    }

    public static String getMediaType(VisualSearchResult vsr) {
        String ext = vsr.getFileExtension();
        MediaType mediaType = MediaType.getMediaTypeForExtension(ext);
        // TODO: RMV improve the text returned
        return mediaType == null ? ext : mediaType.toString();
    }
}