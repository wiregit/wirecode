package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.io.InvalidDataException;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

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
            
            List<String> paths = null;
            try {
                LimeXMLDocument document = response.getDocument();
                if (document != null) {
                    paths = parsePathEntries(document);
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
    
    // TODO: Code is copied+modified from XMLTorrent... might want share 
    private static List<String> parsePathEntries(LimeXMLDocument xmlDocument) throws InvalidDataException {
        String encodedPath = xmlDocument.getValue(LimeXMLNames.TORRENT_FILE_PATHS);
        String encodedSizes = xmlDocument.getValue(LimeXMLNames.TORRENT_FILE_SIZES);
        if (encodedPath == null || encodedSizes == null) {
            String name = xmlDocument.getValue(LimeXMLNames.TORRENT_NAME);
            String length = xmlDocument.getValue(LimeXMLNames.TORRENT_LENGTH);
            if (name != null && length != null) {
                try {
                    return Collections.<String>singletonList(name);
                } catch (NumberFormatException nfe) {
                }
            }
            return Collections.<String>emptyList();
        }
        String[] paths = encodedPath.split("//");
        String[] sizes = encodedSizes.split(" ");
        if (paths.length != sizes.length) {
            return Collections.<String>emptyList();
        }
        List<String> entries = new ArrayList<String>(paths.length);
        for (int i = 0; i < paths.length; i++) {
            try {
                String path = paths[i];
                if (StringUtils.isEmpty(path)) {
                    return Collections.<String>emptyList();
                }
                entries.add(paths[i].substring(1));
            } catch (NumberFormatException nfe){
                return Collections.<String>emptyList();
            }
        }
        return Collections.unmodifiableList(entries);
    }

}
