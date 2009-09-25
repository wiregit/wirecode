package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LibraryData;

import com.google.inject.Inject;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryUtils;

class LibraryDataImpl implements LibraryData {

    private final Library library;
    private final CategoryManager categoryManager;
    
    @Inject
    public LibraryDataImpl(Library fileList, CategoryManager categoryManager) {
        this.library = fileList;
        this.categoryManager = categoryManager;
    }
    
    @Override
    public boolean isFileManageable(File f) {
        return LibraryUtils.isFileManagable(f, categoryManager);
    }
    
    @Override
    public boolean isProgramManagingAllowed() {
        return library.isProgramManagingAllowed();
    }
    
    @Override
    public Collection<Category> getManagedCategories() {
        return library.getManagedCategories();
    }        

    @Override
    public boolean isDirectoryAllowed(File folder) {
        return library.isDirectoryAllowed(folder);
    }
    
    @Override
    public Collection<String> getDefaultExtensions() {
        return library.getDefaultManagedExtensions();
    }
    
    @Override
    public Map<Category, Collection<String>> getExtensionsPerCategory() {
        return library.getExtensionsPerCategory();
    }
    
    @Override
    public void setManagedExtensions(Collection<String> extensions) {
        library.setManagedExtensions(extensions);
    }
    
    @Override
    public void setCategoriesToIncludeWhenAddingFolders(Collection<Category> managedCategories) {
        library.setCategoriesToIncludeWhenAddingFolders(managedCategories);
    }
}