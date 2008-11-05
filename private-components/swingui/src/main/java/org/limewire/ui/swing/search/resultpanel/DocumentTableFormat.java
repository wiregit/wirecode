package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.util.Calendar;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class DocumentTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int ACTION_INDEX = 4;
    private static final int DATE_INDEX = 3;
    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 7;
    private static final int AUTHOR_INDEX = 8;
    private static final int RELEVANCE_INDEX = 5;
    private static final int SIZE_INDEX = 2;
    private static final int TYPE_INDEX = 1;

    public DocumentTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Type"), tr("Size"), tr("Date created"), "", tr("Relevance"),
                tr("Extension"), tr("People with File"), tr("Author"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == DATE_INDEX ? Calendar.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == SIZE_INDEX ? Integer.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case NAME_INDEX: return getIconLabel(vsr);
            case TYPE_INDEX: return fileExtension; // TODO: RMV improve
            case SIZE_INDEX: return vsr.getSize();
            case DATE_INDEX: return getProperty(FilePropertyKey.DATE_CREATED);
            case ACTION_INDEX: return vsr;
            case RELEVANCE_INDEX: return vsr.getRelevance();
            case 6: return fileExtension;
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case AUTHOR_INDEX: return getProperty(FilePropertyKey.AUTHOR);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 380;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 80;
            case DATE_INDEX: return 100;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}