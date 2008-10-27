package org.limewire.ui.swing.util;

import org.limewire.core.api.Category;
import org.limewire.util.MediaType;

public class CategoryUtils {
    public static Category getCategory(MediaType mt) {
        if (mt == MediaType.getAnyTypeMediaType()) return null;
        
        if (mt == MediaType.getAudioMediaType()) return Category.AUDIO;
        
        if (mt == MediaType.getVideoMediaType()) return Category.VIDEO;
        
        if (mt == MediaType.getDocumentMediaType()) return Category.DOCUMENT;
        
        if (mt == MediaType.getProgramMediaType()) return Category.PROGRAM;
        
        return Category.OTHER;
    }
}
