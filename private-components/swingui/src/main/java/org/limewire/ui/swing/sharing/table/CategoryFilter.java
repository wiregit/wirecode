package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Category;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter based on a category. Only displays FileItems of 
 * one category type.
 */
public class CategoryFilter implements Matcher<FileItem>{
    private Category category;
    
    public CategoryFilter(Category category) {
        this.category = category;
    }

    @Override
    public boolean matches(FileItem item) {
        if(item == null) return false;
        if(category == null) return true;
        
        return item.getCategory().equals(category);
    }
}