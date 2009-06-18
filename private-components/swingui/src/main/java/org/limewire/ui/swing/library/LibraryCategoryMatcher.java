package org.limewire.ui.swing.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;

import com.google.inject.Inject;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher for the LibraryTable. This allows the same eventlist to be used
 * in different table views without having to create a new filter. A category
 * may be selected and the eventList will be filtered on that category.
 */
class LibraryCategoryMatcher implements Matcher<LocalFileItem> {

    private Category categoryFilteredOn;
    
    @Inject
    public LibraryCategoryMatcher(){
    }
    
    Category getCategory() {
        return categoryFilteredOn;
    }
    
    /**
     * Category to filter this list on. Only Items within this
     * category will be displayed. If all categories are to be 
     * shown, category may be set to null.
     */
    public void setCategoryFilter(Category category) {
        this.categoryFilteredOn = category;
    }
    
    @Override
    public boolean matches(LocalFileItem item) {
        if(categoryFilteredOn == null)
            return true;
        else
            return categoryFilteredOn.equals(item.getCategory());
    }
}
