package com.limegroup.gnutella.filters.response;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;

class ProgramsFilter implements ResponseFilter {
    
    private final CategoryManager categoryManager;
    
    @Inject ProgramsFilter(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    @Override
    public boolean allow(QueryReply qr, Response response) {
        if(categoryManager.getCategoryForFilename(response.getName()) == Category.PROGRAM) {
            return LibrarySettings.ALLOW_PROGRAMS.getValue();
        } else {
            return true;
        }
    }

}
