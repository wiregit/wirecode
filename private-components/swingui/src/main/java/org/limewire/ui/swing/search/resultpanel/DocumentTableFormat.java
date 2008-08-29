package org.limewire.ui.swing.search.resultpanel;

import com.google.inject.Inject;
import java.util.Calendar;
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

    private static final int DATE_CREATED_INDEX = 4;
    private static final int ICON_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 7;
    private static final int RELEVANCE_INDEX = 6;
    private static final int SIZE_INDEX = 3;

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
    public Class getColumnClass(int index) {
        return index == DATE_CREATED_INDEX ? Calendar.class :
            index == ICON_INDEX ? Icon.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == SIZE_INDEX ? Integer.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();
        Icon icon = iconManager.getIconForExtension(fileExtension);

        switch (index) {
            case 0: return icon;
            case 1: return getProperty(PropertyKey.NAME);
            case 2: return fileExtension; // TODO: RMV improve
            case 3: return vsr.getSize();
            case 4: return getProperty(PropertyKey.DATE_CREATED);
            case 5: return vsr;
            case 6: return getProperty(PropertyKey.RELEVANCE);
            case 7: return vsr.getSources().size();
            case 8: return getProperty(PropertyKey.OWNER);
            case 9: return getProperty(PropertyKey.AUTHOR);
            default: return null;
        }
    }
}