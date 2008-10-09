package org.limewire.core.impl.search;

import org.limewire.core.api.Category;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.MediaType;


public class MediaTypeConverter {

    private MediaTypeConverter() {}
    
    public static MediaType toMediaType(Category category) {
        return toMediaType(SearchCategory.forCategory(category));
    }

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
        case PROGRAM:
            return MediaType.getProgramMediaType();
        case OTHER:
            return MediaType.getOtherMediaType();
        default:
            throw new IllegalArgumentException(searchCategory.name());
        }
    }

    public static Category toCategory(MediaType type) {
        if (type == MediaType.getAudioMediaType()) {
            return Category.AUDIO;
        } else if (type == MediaType.getVideoMediaType()) {
            return Category.VIDEO;
        } else if (type == MediaType.getImageMediaType()) {
            return Category.IMAGE;
        } else if (type == MediaType.getDocumentMediaType()) {
            return Category.DOCUMENT;
        } else if (type == MediaType.getProgramMediaType()) {
            return Category.PROGRAM;
        }
        return Category.OTHER;
    }
    
}
