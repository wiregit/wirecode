package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import org.limewire.core.api.Category;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 */
@Singleton
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int EXTENSION_INDEX = 2;
    static final int TYPE_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    
    private final IconManager iconManager;
    
    @Inject
    public AllTableFormat(IconManager iconManager) {
        super(NAME_INDEX, FROM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_ALL_FROM", I18n.tr("From"), 55, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_ALL_NAME", I18n.tr("Name"), 550, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_ALL_EXTENSION", I18n.tr("Extension"), 80, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "CLASSIC_SEARCH_ALL_TYPE", I18n.tr("Type"), 60, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_ALL_SIZE", I18n.tr("Size"), 60, true, true)
        });
        
        this.iconManager = iconManager;
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case NAME_INDEX: return Component.class;
        case SIZE_INDEX: return Long.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        switch(column) {
            case FROM_INDEX: return vsr;
            case NAME_INDEX: return vsr;
            case TYPE_INDEX: 
                if( vsr.getCategory() == Category.DOCUMENT || vsr.getCategory() == Category.PROGRAM || vsr.getCategory() == Category.OTHER) {
                    String mime = iconManager.getMIMEDescription(vsr.getFileExtension());
                    if(mime != null)
                        return I18n.tr(vsr.getCategory().getSingularName()) + " (" + mime + ")";
                    else
                        return I18n.tr(vsr.getCategory().getSingularName());
                } else
                    return I18n.tr(vsr.getCategory().getSingularName());
            case SIZE_INDEX: return vsr.getSize();
            case EXTENSION_INDEX: return vsr.getFileExtension();
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
}