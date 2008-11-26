package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 */
public class ProgramTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int NUM_SOURCES_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int SIZE_INDEX = 2;
    public static final int PLATFORM_INDEX = 3;
    public static final int COMPANY_INDEX = 4;
    public static final int FROM_INDEX = 5;
    public static final int FILE_EXTENSION_INDEX = 6;
    public static final int AUTHOR_INDEX = 7;

    
    public ProgramTableFormat() {
        super(FROM_INDEX,
                tr("People with File"), 
                tr("Name"), 
                tr("Size"), 
                tr("Platform"), 
                tr("From"),
                tr("Company"), 
                tr("Extension"),
                tr("Author"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == SIZE_INDEX ? Integer.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case NAME_INDEX: return getIconLabel(vsr);
            case SIZE_INDEX: return vsr.getSize();
            case PLATFORM_INDEX: return getProperty(FilePropertyKey.PLATFORM);
            case COMPANY_INDEX: return getProperty(FilePropertyKey.COMPANY);
            case FILE_EXTENSION_INDEX: return fileExtension;
            case FROM_INDEX: return vsr;
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case AUTHOR_INDEX: return getProperty(FilePropertyKey.AUTHOR);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 360;
            case SIZE_INDEX: return 80;
            case PLATFORM_INDEX: return 80;
            case COMPANY_INDEX: return 120;
            case FROM_INDEX: return 200;
            default: return 100;
        }
    }

    @Override
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == FROM_INDEX;
    }
}