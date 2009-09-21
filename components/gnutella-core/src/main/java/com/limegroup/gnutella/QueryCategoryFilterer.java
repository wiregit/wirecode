package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.Inject;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Interacts with {@link QueryRequest} methods such as
 * {@link QueryRequest#desiresAudio()}, {@link QueryRequest#desiresDocuments()},
 * etc.. and translates them into {@link Category} and {@link Predicate
 * Predicates}. 
 */
public class QueryCategoryFilterer {
    
    private final CategoryManager categoryManager;
    
    @Inject public QueryCategoryFilterer(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }
    
    /** Returns a list of categories requested, or null if all are requested. */
    public List<Category> getRequestedCategories(QueryRequest query) {
        if(query.desiresAll()) {
            return null;
        }
        
        List<Category> categories = new ArrayList<Category>(); 
        if (query.desiresLinuxOSXPrograms() || query.desiresWindowsPrograms()) {
            categories.add(Category.PROGRAM);
        }
        if (query.desiresDocuments()) {
            categories.add(Category.DOCUMENT);
        }
        if (query.desiresAudio()) {
            categories.add(Category.AUDIO);
        }
        if (query.desiresVideo()) {
            categories.add(Category.VIDEO);
        }
        if (query.desiresImages()) {
            categories.add(Category.IMAGE);
        }
        return categories;
    }

    /** Returns a Predicate<String> to use for your query. */
    public Predicate<String> getPredicateForQuery(QueryRequest query) {
        if (query.desiresAll()) {
            return Predicates.alwaysTrue();
        }
    
        List<Predicate<String>> predicates = new ArrayList<Predicate<String>>();
        if (query.desiresLinuxOSXPrograms()) {
            predicates.add(categoryManager.getOsxAndLinuxProgramsFilter());
        }
        if (query.desiresWindowsPrograms()) {
            predicates.add(categoryManager.getWindowsProgramsFilter());
        }
        if (query.desiresDocuments()) {
            predicates.add(categoryManager.getFilterForCategory(Category.DOCUMENT));
        }
        if (query.desiresAudio()) {
            predicates.add(categoryManager.getFilterForCategory(Category.AUDIO));
        }
        if (query.desiresVideo()) {
            predicates.add(categoryManager.getFilterForCategory(Category.VIDEO));
        }
        if (query.desiresImages()) {
            predicates.add(categoryManager.getFilterForCategory(Category.IMAGE));
        }
        
        return Predicates.or(predicates);
    }

}
