package org.limewire.core.impl.file;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Implementation of CategoryManager for the mock core.
 */
public class MockCategoryManager implements CategoryManager {

    @Override
    public Category getCategoryForExtension(String extension) {
        return Category.OTHER;
    }

    @Override
    public Category getCategoryForFile(File file) {
        return Category.OTHER;
    }

    @Override
    public Category getCategoryForFilename(String filename) {
        return Category.OTHER;
    }

    @Override
    public Predicate<String> getExtensionFilterForCategory(Category category) {
        return Predicates.alwaysFalse();
    }

    @Override
    public Collection<String> getExtensionsForCategory(Category category) {
        return Collections.emptySet();
    }

    @Override
    public Predicate<String> getOsxAndLinuxProgramsFilter() {
        return Predicates.alwaysFalse();
    }

    @Override
    public Predicate<String> getWindowsProgramsFilter() {
        return Predicates.alwaysFalse();
    }
}
