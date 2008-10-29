package org.limewire.core.api.search;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;

public enum SearchCategory {
    ALL(null, 0), 
    AUDIO(Category.AUDIO, 1), 
    VIDEO(Category.VIDEO, 2), 
    IMAGE(Category.IMAGE, 3),
    DOCUMENT(Category.DOCUMENT, 4), 
    PROGRAM(Category.PROGRAM, 5), 
    OTHER(Category.OTHER, 6),
    
    ;
    
    private static final EnumMap<Category, SearchCategory> perCategory = new EnumMap<Category, SearchCategory>(Category.class);
    private static final Map<Integer, SearchCategory> perId = new HashMap<Integer, SearchCategory>();

    static {
        for(SearchCategory searchCategory : values()) {
            if(searchCategory.category != null) {
                perCategory.put(searchCategory.category, searchCategory);
                perId.put(searchCategory.getId(), searchCategory);
            }
        }
    }
    
    private final Category category;
    private final int id;
 
    private SearchCategory(Category category, int id) {
        this.id = id;
        this.category = category;
    }
    
    public static SearchCategory forCategory(Category category) {
        return perCategory.get(category);
    }
    
    public static SearchCategory forId(Integer id) {
        SearchCategory category = perId.get(id);
        if(category == null) {
            return SearchCategory.ALL;
        } else {
            return category;
        }
    }
    
    public int getId() {
        return id;
    }
}