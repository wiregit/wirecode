package org.limewire.core.api.search;

import java.util.EnumMap;

import org.limewire.core.api.Category;

public enum SearchCategory {
    ALL(null), 
    AUDIO(Category.AUDIO), 
    VIDEO(Category.VIDEO), 
    IMAGE(Category.IMAGE),
    DOCUMENT(Category.DOCUMENT), 
    PROGRAM(Category.PROGRAM), 
    OTHER(Category.OTHER),
    
    ;
    
    private static final EnumMap<Category, SearchCategory> perCategory = new EnumMap<Category, SearchCategory>(Category.class);
    static {
        for(SearchCategory searchCategory : values()) {
            if(searchCategory.category != null) {
                perCategory.put(searchCategory.category, searchCategory);
            }
        }
    }
    
    private final Category category;
    
    private SearchCategory(Category category) {
        this.category = category;
    }
    
    public static SearchCategory forCategory(Category category) {
        return perCategory.get(category);
    }
}