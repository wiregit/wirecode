package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int ACTION_INDEX = 3;
    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 6;
    private static final int OWNER_INDEX = 7;
    private static final int RELEVANCE_INDEX = 4;
    private static final int SIZE_INDEX = 2;
    private static final int TYPE_INDEX = 1;

    public AllTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX, tr("Name"), tr("Type"), tr("Size"), "",
                tr("Relevance"), tr("Extension"), tr("People with file"), tr("Owner"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == SIZE_INDEX ? Long.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case NAME_INDEX: return getIconLabel(vsr);
            case TYPE_INDEX: return vsr.getCategory();
            case SIZE_INDEX: return vsr.getSize();
            case ACTION_INDEX: return vsr;
            case RELEVANCE_INDEX: return getProperty(FilePropertyKey.RELEVANCE);
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case OWNER_INDEX: return getProperty(FilePropertyKey.OWNER);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 500;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 60;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}