package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.Calendar;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * image descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ImageTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int ACTION_INDEX = 3;
    private static final int DATE_INDEX = 2;
    private static final int EXTENSION_INDEX = 1;
    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 5;
    private static final int RELEVANCE_INDEX = 4;

    public ImageTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Extension"), tr("Date created"), "", tr("Relevance"),
                tr("People with File"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == DATE_INDEX ? Calendar.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case NAME_INDEX: return getProperty(FilePropertyKey.NAME);
            case EXTENSION_INDEX: return fileExtension; // TODO: RMV improve
            case DATE_INDEX: return getProperty(FilePropertyKey.DATE_CREATED);
            case ACTION_INDEX: return vsr;
            case RELEVANCE_INDEX: return vsr.getRelevance();
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 460;
            case EXTENSION_INDEX: return 80;
            case DATE_INDEX: return 100;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}