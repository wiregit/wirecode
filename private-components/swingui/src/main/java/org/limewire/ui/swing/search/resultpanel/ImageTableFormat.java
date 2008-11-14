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

    public static final int NAME_INDEX = 0;
    public static final int EXTENSION_INDEX = 1;
    public static final int DATE_INDEX = 2;
    public static final int ACTION_INDEX = 3;
    public static final int NUM_SOURCES_INDEX = 4;
    public static final int SIZE_INDEX = 5;

    public ImageTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Extension"), tr("Date created"), "",
                tr("People with File"), tr("Size"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == DATE_INDEX ? Calendar.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
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
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case SIZE_INDEX: return vsr.getSize();
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