package com.limegroup.gnutella;

import com.limegroup.gnutella.gml.GMLDocument;
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
    private ID3Tag _id3Tag;

    /**
     * @param index index of the file
     * @param file the file
     */
    public FileDesc(int index, File file) {
        _index = index;
        _file = file;
        try {
            _id3Tag = ID3Tag.read(file);
        }
        catch(IOException e) {
            Assert.that(false, "IOException looking for ID3 tag");
        }
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

    public final String getMetadata() {
        if(_id3Tag != null)
            return _id3Tag.getGMLString();
        else
            return "";
    }

    /**
     * This method encapsulates the algorithm for matching a query string
     * to a file
     *
     * @return true iff textQuery matches this file.
     */
    public final boolean isTextQueryMatch(String textQuery) {
        // Match path
        if(StringUtils.contains(_file.getPath(), textQuery, true))
            return true;
        // Match metadata
        return ((_id3Tag != null) && (_id3Tag.isTextQueryMatch(textQuery)));
    }

    /**
     * This method encapsulates the algorithm for matching a GML Request
     * to a file
     *
     * @return true iff gmlDocument matches this file.
     */
    public final boolean isGMLDocumentMatch(GMLDocument gmlDocument) {
        return ((_id3Tag != null) && (_id3Tag.isGMLDocumentMatch(gmlDocument)));
    }

    /**
     * This method encapsulates decision of whether a GML Request is understood
     * by our FileDesc system.  Right now, this is just a pass-through to
     * the ID3Tag method of the same name, but the idea is, when we add
     * other sorts of metadata, we can just modify the body of this method, and
     * not touch FileManager.
     *
     * @return true iff gmlDocument is defined by a GML template understood
     *         by our system.
     */
    public static final boolean isGMLDocumentUnderstandable(
        GMLDocument gmlDocument)
    {
        return ID3Tag.isGMLDocumentUnderstandable(gmlDocument);
    }
}
