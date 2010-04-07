package org.limewire.core.api.file;

import java.io.File;
import java.util.Collection;

import org.limewire.core.api.Category;

import com.google.common.base.Predicate;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/** Utility methods for dealing with file categories. */
public interface CategoryManager {
    
    /**
     * Returns a filter that allows only Windows programs files. The filter
     * matches against extension only, not on the full filename.
     */
    Predicate<String> getWindowsProgramsFilter();
    
    /**
     * Returns a filter that allows only OSX & Linux programs files. The filter
     * matches against extension only, not on the full filename.
     */
    Predicate<String> getOsxAndLinuxProgramsFilter();
    
    /**
     * Returns a filter that allows only files for the given category. The
     * filter matches against extension only, not on the full filename.
     */
    Predicate<String> getExtensionFilterForCategory(Category category);
    
    /** Returns the category that best matches the given extension. */
    Category getCategoryForExtension(String extension);
    
    /** Returns the category that best matches the given filename. */
    Category getCategoryForFilename(String filename);
    
    /** Returns the category that best matches the given file. */
    Category getCategoryForFile(File file);
    
    boolean containsCategory(Category category, LimeXMLDocument document);
    
    /** Gets all extensions for a given category. */
    Collection<String> getExtensionsForCategory(Category category);

}
