package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.MediaType;


public class MediaTypeConverter {

    private MediaTypeConverter() {}


    public static MediaType toMediaType(SearchCategory searchCategory) {
        switch (searchCategory) {
        case ALL:
            return MediaType.getAnyTypeMediaType();
        case AUDIO:
            return MediaType.getAudioMediaType();
        case DOCUMENT:
            return MediaType.getDocumentMediaType();
        case IMAGE:
            return MediaType.getImageMediaType();
        case VIDEO:
            return MediaType.getVideoMediaType();
        default:
            throw new IllegalArgumentException(searchCategory.name());
        }
    }
    
}
