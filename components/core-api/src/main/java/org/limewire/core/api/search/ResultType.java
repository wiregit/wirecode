package org.limewire.core.api.search;

public enum ResultType {

    AUDIO(SearchCategory.AUDIO),
    VIDEO(SearchCategory.VIDEO),
    IMAGE(SearchCategory.IMAGE),
    DOCUMENT(SearchCategory.DOCUMENT),
    PROGRAM(SearchCategory.PROGRAM),
    OTHER(SearchCategory.OTHER),
    
    ;

    private final SearchCategory searchCategory;

    ResultType(SearchCategory category) {
        this.searchCategory = category;
    }

    public SearchCategory toSearchCategory() {
        return searchCategory;
    }
}