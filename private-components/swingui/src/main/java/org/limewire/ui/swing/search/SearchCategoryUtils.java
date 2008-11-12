package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

public class SearchCategoryUtils {
    
    private SearchCategoryUtils() {}
        
    public static String getName(SearchCategory category) {
        switch(category) {
        case ALL:      return I18n.tr("All");
        case AUDIO:    return I18n.tr("Audio"); 
        case DOCUMENT: return I18n.tr("Documents"); 
        case IMAGE:    return I18n.tr("Images"); 
        case PROGRAM:  return I18n.tr("Programs"); 
        case VIDEO:    return I18n.tr("Videos"); 
        case OTHER: 
        default:
            return I18n.tr("Other");
             
        }
    }

}
