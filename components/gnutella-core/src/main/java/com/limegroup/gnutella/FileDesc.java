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
    private GMLReplyCollection _replyCollection;

    /**
     * @param index index of the file
     * @param file the file
     */
    public FileDesc(int index, File file, String fileIdentifier,
                    GMLTemplateRepository templateRepository,
                    GMLReplyRepository replyRepository) {
        _index = index;
        _file = file;

        // Construct the reply collection from the repository metadata and
        // the ID3 metadata
        _replyCollection = new GMLReplyCollection();
        _replyCollection.merge(
            replyRepository.getReplyCollection(fileIdentifier));
        try {
            GMLDocument id3Reply = ID3DocumentFactory.createID3Document(
                file, templateRepository);
            if(id3Reply != null)
                _replyCollection.addReply(id3Reply);
        } catch(IOException e) {
            // If we encounter an error reading the file, just leave
            // out the ID3 Reply, as if we couldn't find any ID3 metadata
        } catch(TemplateNotFoundException e) {
            // If we can't find the template, just out the ID3 Reply,
            // as if we couldn't find any ID3 metadata
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
        return _replyCollection.getXMLString();
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
        return _replyCollection.hasMatch(textQuery);
    }

    /**
     * This method encapsulates the algorithm for matching a GML Request
     * to a file
     *
     * @return true iff gmlDocument matches this file.
     */
    public final boolean isGMLDocumentMatch(GMLDocument gmlDocument) {
        // Match metadata
        return _replyCollection.hasMatch(gmlDocument);
    }
}
