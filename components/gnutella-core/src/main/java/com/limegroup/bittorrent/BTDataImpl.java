package com.limegroup.bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.SHA1;
import org.limewire.service.ErrorService;
import org.limewire.util.BEncoder;
import org.limewire.util.CommonUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.limegroup.gnutella.Constants;

/**
 * Contains type safe representations of all understand information in in a
 * .torrent file.
 * <p>
 * This will throw a <code>ValueException</code> if the data is malformed or not
 * what we expect it to be. UTF-8 versions of Strings are preferred over ASCII
 * versions, wherever possible.
 */
public class BTDataImpl implements BTData {

    private static final Log LOG = LogFactory.getLog(BTDataImpl.class);

    /** The URL of the tracker. */
    // TODO: add support for UDP & multiple trackers.
    private final String announce;

    /** The webseed addresses */
    private final URI[] webSeeds;

    /**
     * All the pieces as one big array. Non-final 'cause it's big & we want to
     * clear it.
     */
    private/* final */byte[] pieces;

    /** The length of a single piece. */
    private final Long pieceLength;

    /** The SHA1 of the info object. */
    private byte[] infoHash;

    /**
     * The name of the torrent file (if one file) or parent folder (if multiple
     * files).
     */
    private final String name;

    /** The length of the torrent if one file. null if multiple. */
    private final Long length;

    /**
     * A list of subfiles of this torrent is multiple files. null if a single
     * file.
     */
    private final List<BTData.BTFileData> files;

    /** A list of all subfolders this torrent uses. null if a single file. */
    private final Set<String> folders;

    /** Whether the private flag is set */
    private final boolean isPrivate;

    /** Constructs a new BTData out of the map of properties. */
    // See http://wiki.theory.org/BitTorrentSpecification#Info_Dictionary
    // for more information
    public BTDataImpl(Map<?, ?> torrentFileMap) throws ValueException {
        Object tmp;

        tmp = torrentFileMap.get("announce");
        if (tmp instanceof byte[])
            announce = StringUtils.getASCIIString((byte[]) tmp);
        else
            throw new ValueException("announce missing or invalid!");

        webSeeds = parseWebSeeds(torrentFileMap);
        tmp = torrentFileMap.get("info");
        if (tmp == null || !(tmp instanceof Map))
            throw new ValueException("info missing or invalid!");

        Map infoMap = (Map) tmp;
        infoHash = calculateInfoHash(infoMap);

        tmp = infoMap.get("private");
        if (tmp instanceof Long) {
            isPrivate = ((Long) tmp).intValue() == 1;
        } else
            isPrivate = false;

        tmp = infoMap.get("pieces");
        if (tmp instanceof byte[])
            pieces = (byte[]) tmp;
        else
            throw new ValueException("info->piece missing!");

        tmp = infoMap.get("piece length");
        if (tmp instanceof Long)
            pieceLength = (Long) tmp;
        else
            throw new ValueException("info->'piece length' missing!");

        // get name, prefer utf8
        tmp = infoMap.get("name.utf-8");
        name = getPreferredString(infoMap, "name");
        if (name == null || name.length() == 0)
            throw new ValueException("no valid name!");

        if (infoMap.containsKey("length") == infoMap.containsKey("files"))
            throw new ValueException("info->length & info.files can't both exist or not exist!");

        tmp = infoMap.get("length");
        if (tmp instanceof Long) {
            length = (Long) tmp;
            if (length < 0)
                throw new ValueException("invalid length value");
        } else if (tmp != null)
            throw new ValueException("info->length is non-null, but not a Long!");
        else
            length = null;

        tmp = infoMap.get("files");
        if (tmp instanceof List) {
            List<?> fileData = (List) tmp;
            if (fileData.isEmpty())
                throw new ValueException("empty file list");

            files = new ArrayList<BTData.BTFileData>(fileData.size());
            folders = new HashSet<String>();

            for (Object o : fileData) {
                if (!(o instanceof Map))
                    throw new ValueException("info->files[x] not a Map!");
                Map<?, ?> fileMap = (Map) o;

                tmp = fileMap.get("length");
                if (!(tmp instanceof Long))
                    throw new ValueException("info->files[x].length not a Long!");
                Long ln = (Long) tmp;
                if (ln < 0)
                    throw new ValueException("invalid length");

                boolean doASCII = true;

                // Don't try ASCII if UTF-8 succeeds.
                try {
                    parseFiles(fileMap, ln, files, folders, true);
                    doASCII = false;
                } catch (ValueException ignored) {
                }

                if (doASCII)
                    parseFiles(fileMap, ln, files, folders, false);
            }
        } else if (tmp != null) {
            throw new ValueException("info->files is non-null, but not a list!");
        } else {
            files = null;
            folders = null;
        }
    }

    /**
     * Parses the webseed addresses from the torrent file. The web seed
     * addresses should be in a parameter "url-list". url-list can either be a
     * list or a single webseed address.
     */
    @SuppressWarnings("unchecked")
    private URI[] parseWebSeeds(Map<?, ?> torrentFileMap) {
        List<URI> webSeedsArray = new ArrayList<URI>();
        URI[] webSeeds = null;
        Object tmp = torrentFileMap.get("url-list");
        if (tmp != null) {
            if (tmp instanceof List) {
                List<byte[]> uris = (List<byte[]>) tmp;
                if (uris.size() > 0) {
                    webSeeds = new URI[uris.size()];
                    for (byte[] uri : uris) {
                        addURI(webSeedsArray, StringUtils.getASCIIString(uri));
                    }
                }
            } else if (tmp instanceof byte[]) {
                String uri = StringUtils.getASCIIString((byte[]) tmp);
                addURI(webSeedsArray, uri);
            }
        }
        webSeeds = webSeedsArray.toArray(new URI[webSeedsArray.size()]);
        return webSeeds;
    }

    private void addURI(List<URI> uris, String uriString) {
        try {
            URI uri = URIUtils.toURI(uriString);
            uris.add(uri);
        } catch (URISyntaxException e) {
            LOG.warn("Error parsing uri: " + uriString, e);
        }
    }

    /** Parses the List of Maps of file data. */
    private void parseFiles(Map<?, ?> fileMap, Long ln, List<BTData.BTFileData> fileData,
            Set<String> folderData, boolean utf8) throws ValueException {

        Object tmp = fileMap.get("path" + (utf8 ? ".utf-8" : ""));
        if (!(tmp instanceof List))
            throw new ValueException("info->files[x].path[.utf-8] not a List!");

        Set<String> newFolders = new HashSet<String>();
        String path = parseFileList((List) tmp, newFolders, true);
        if (path == null)
            throw new ValueException("info->files[x].path[-utf-8] not valid!");

        folderData.addAll(newFolders);
        fileData.add(new BTData.BTFileData(ln, path));
    }

    /**
     * Parses a list of paths into a single string, adding the intermediate
     * folders into the Set of folders. The paths are parsed either as UTF or
     * ASCII.
     */
    private String parseFileList(List<?> paths, Set<String> folders, boolean utf8)
            throws ValueException {
        if (paths.isEmpty())
            throw new ValueException("empty paths list");
        StringBuilder sb = new StringBuilder();
        for (Iterator<?> i = paths.iterator(); i.hasNext();) {
            Object o = i.next();
            if (!(o instanceof byte[]))
                throw new ValueException("info->files[x]->path[.utf-8][x] not a byte[]!");

            String current;
            if (utf8)
                current = StringUtils.getUTF8String((byte[]) o);
            else
                current = StringUtils.getASCIIString((byte[]) o);

            if (current.length() == 0)
                throw new ValueException("empty path element");

            // using unix path style so path can be appended to urls
            sb.append("/");
            sb.append(CommonUtils.convertFileName(current));
            // if another path, this is a subfolder, so add it to folders
            if (i.hasNext())
                folders.add(sb.toString());
        }
        return sb.toString();
    }

    /**
     * Returns either the UTF-8 version (if it exists) or the ASCII version of a
     * String.
     */
    private String getPreferredString(Map<?, ?> info, String key) {
        String str = null;

        Object data = info.get(key + ".utf-8");
        if (data instanceof byte[]) {
            try {
                str = new String((byte[]) data, Constants.UTF_8_ENCODING);
            } catch (Throwable t) {
            } // could throw any error if input bytes are invalid
        }

        if (str == null) {
            data = info.get(key);
            if (data instanceof byte[])
                str = StringUtils.getASCIIString((byte[]) data);
        }

        return str;
    }

    /**
     * Calculates the infoHash of the map. Because BT maps are stored as String
     * -> Object, and the keys are stored alphabetically, it is guaranteed that
     * any two maps with identical keys & values will have the same info hash
     * when decoded & recoded.
     * 
     * @return the infoHash of the infoMap
     */
    private byte[] calculateInfoHash(Map<?, ?> infoMap) {
        // create the info hash, we could create the info hash while reading it
        // but that would make the code a lot more complex. This works well too,
        // because the order of a list is not changed during the process of
        // decoding or encoding it and Maps are always sorted alphanumerically
        // when encoded.
        // So the data we encoded is always exactly the same as the data before
        // we decoded it. This is intended that way by the protocol.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BEncoder.getEncoder(baos, true, false, "UTF-8").encodeDict(infoMap);
        } catch (IOException ioe) {
            ErrorService.error(ioe);
        }

        MessageDigest md = new SHA1();
        return md.digest(baos.toByteArray());
    }

    @Override
    public String getAnnounce() {
        return announce;
    }

    @Override
    public List<BTData.BTFileData> getFiles() {
        return files;
    }

    @Override
    public Set<String> getFolders() {
        return folders;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public byte[] getInfoHash() {
        return infoHash;
    }

    @Override
    public Long getLength() {
        return length;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getPieceLength() {
        return pieceLength;
    }

    @Override
    public byte[] getPieces() {
        return pieces;
    }

    @Override
    public void clearPieces() {
        pieces = null;
    }

    @Override
    public URI[] getWebSeeds() {
        return webSeeds;
    }
}
