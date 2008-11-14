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

    public static final int NAME_INDEX = 0;
    public static final int TYPE_INDEX = 1;
    public static final int SIZE_INDEX = 2;
    public static final int DATE_INDEX = 3;
    public static final int ACTION_INDEX = 4;
    public static final int FILE_EXTENSION_INDEX = 5;
    public static final int NUM_SOURCES_INDEX = 6;
    public static final int AUTHOR_INDEX = 7;
    
    public DocumentTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Type"), tr("Size"), tr("Date created"), "",
                tr("Extension"), tr("People with File"), tr("Author"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == DATE_INDEX ? Calendar.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
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
            case FILE_EXTENSION_INDEX: return fileExtension;
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