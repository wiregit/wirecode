package com.limegroup.gnutella;

import com.limegroup.gnutella.gml.GMLDocument;
import com.limegroup.gnutella.gml.GMLReplyCollection;
import com.limegroup.gnutella.gml.GMLReplyRepository;
import com.limegroup.gnutella.gml.GMLTemplateRepository;
import com.limegroup.gnutella.gml.TemplateNotFoundException;
import com.limegroup.gnutella.util.StringUtils;
import java.io.File;
import java.io.IOException;

/**
 * A wrapper for file information.
 *
 * @author rsoule, reworked by rvogl to add encapsulation
 */
public final class FileDesc {
    private int _index;
    private File _file;

    /**
     * @param index index of the file
     * @param file the file
     */
    public FileDesc(int index, File file) {
        _index = index;
        _file = file;
    }

    public final File getFile() {
        return _file;
    }

    public final int getIndex() {
        return _index;
    }

    public final String getPath() {
        return _file.getPath();
    }

    public final String getName() {
        return _file.getName();
    }

    public final int getSize() {
        return (int)_file.length();
    }

    /**
     * This method encapsulates the algorithm for matching a query string
     * to a file
     *
     * @return true iff textQuery matches this file.
     */
    public final boolean isTextQueryMatch(String textQuery) {
        // Match path
        return StringUtils.contains(_file.getPath(), textQuery, true);
    }
}
