package com.limegroup.gnutella.filters.response;

import java.util.List;

import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.impl.XMLTorrent;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class ProgramsFilter implements ResponseFilter {
    
    private final CategoryManager categoryManager;
    
    @Inject ProgramsFilter(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    @Override
    public boolean allow(QueryReply qr, Response response) {
        Category category = categoryManager.getCategoryForFilename(response.getName());
        
        if (category == Category.PROGRAM) {
            return LibrarySettings.ALLOW_PROGRAMS.getValue();
        } else if (category == Category.TORRENT) {
            
            // If programs are permitted then there is no condition where 
            //  the response would not be allowed.  Return true.
            if (LibrarySettings.ALLOW_PROGRAMS.getValue()) {
                return true;
            }
            
            List<TorrentFileEntry> paths = null;
            try {
                LimeXMLDocument document = response.getDocument();
                if (document != null) {
                    paths = XMLTorrent.parsePathEntries(document);
                }
            } catch (InvalidDataException e) {
                // No files found in xml
                return true;
            }
            
            // If there is path metadata check if those paths do not lead to 
            //  executable programs.
            return !categoryManager.containsCategory(Category.PROGRAM, paths);
        }
        else {
            return true;
        }
    }

}
