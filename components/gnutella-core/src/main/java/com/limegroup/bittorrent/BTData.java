package com.limegroup.bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.bittorrent.bencoding.BEncoder;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.StringUtils;

/**
 * A struct-like class which contains typesafe representations
 * of everything we understand in in a .torrent file.
 * 
 * This will throw a ValueException if the data is malformed or
 * not what we expect it to be.  UTF-8 versions of Strings are
 * preferred over ASCII versions, wherever possible.
 */
public class BTData {
    
    /** The URL of the tracker. */
    // TODO: add support for UDP & multiple trackers.
    private final String announce;
    
    /** All the pieces as one big array.  Non-final 'cause it's big & we want to clear it. */
    private /*final*/ byte[] pieces;
    
    /** The length of a single piece. */
    private final Long pieceLength;
    
    /** The SHA1 of the info object. */
    private byte[] infoHash;
    
    /** The name of the torrent file (if one file) or parent folder (if multiple files). */
    private final String name;
    
    /** The length of the torrent if one file.  null if multiple. */
    private final Long length;
    
    /** A list of subfiles of this torrent is multiple files.  null if a single file. */
    private final List<BTFileData> files;
    
    /** A list of all subfolders this torrent uses.  null if a single file. */
    private final Set<String> folders;
    
    /** Constructs a new BTData out of the map of properties. */
    public BTData(Map<?, ?> torrentFileMap) throws ValueException {
        Object tmp;
        
        tmp = torrentFileMap.get("announce");
        if(tmp instanceof byte[])
            announce = StringUtils.getASCIIString((byte[])tmp);
        else
            throw new ValueException("announce missing or invalid!");
        
        tmp = torrentFileMap.get("info");
        if(tmp == null || !(tmp instanceof Map))
            throw new ValueException("info missing or invalid!");
        
        Map infoMap = (Map)tmp;
        infoHash = calculateInfoHash(infoMap);
        
        tmp = infoMap.get("pieces");
        if(tmp instanceof byte[])
            pieces = (byte[])tmp;
        else
            throw new ValueException("info->piece missing!");
        
        tmp = infoMap.get("piece length");
        if(tmp instanceof Long)
            pieceLength = (Long)tmp;
        else
            throw new ValueException("info->'piece length' missing!");
        
        // get name, prefer utf8
        tmp = infoMap.get("name.utf-8");
        name = getPreferredString(infoMap, "name");
        if(name == null)
            throw new ValueException("no valid name!");
        
        if(infoMap.containsKey("length") == infoMap.containsKey("files"))
            throw new ValueException("info->length & info.files can't both exist or not exist!");
        
        tmp = infoMap.get("length");
        if(tmp instanceof Long)
            length = (Long)tmp;
        else if(tmp != null)
            throw new ValueException("info->length is non-null, but not a Long!");
        else
            length = null;
        
        tmp = infoMap.get("files");        
        if(tmp instanceof List) {
            List<?> fileData = (List)tmp;
            files = new ArrayList<BTFileData>(fileData.size());
            folders = new HashSet<String>();
            
            for(Object o : fileData) {
                if(!(o instanceof Map))
                    throw new ValueException("info->files[x] not a Map!");
                Map<?, ?> fileMap = (Map)o;
                
                tmp = fileMap.get("length");
                if(!(tmp instanceof Long))
                    throw new ValueException("info->files[x].length not a Long!");
                Long ln = (Long)tmp;
                
                boolean doASCII = true;
                
                //Don't try ASCII if UTF-8 succeeds.
                try {
                    parseFiles(fileMap, ln, files, folders, true);
                    doASCII = false;
                } catch(ValueException ignored) {}
                
                if(doASCII)
                    parseFiles(fileMap, ln, files, folders, false);
            }
        } else if(tmp != null) {
            throw new ValueException("info->files is non-null, but not a list!");
        } else {
            files = null;
            folders = null;
        }
    }
    
    /** Parses the List of Maps of file data. */
    private void parseFiles(Map<?, ?> fileMap, Long ln, List<BTFileData> fileData,
                            Set<String> folderData, boolean utf8) throws ValueException {
        
        Object tmp = fileMap.get("path" + (utf8 ? ".utf-8" : ""));
        if(!(tmp instanceof List))
            throw new ValueException("info->files[x].path[.utf-8] not a List!");
        
        Set<String> newFolders = new HashSet<String>();
        String path = parseFileList((List)tmp, newFolders, true);
        if(path == null)
            throw new ValueException("info->files[x].path[-utf-8] not valid!");
        
        folderData.addAll(newFolders);
        fileData.add(new BTFileData(ln, path));
    }
    
    /**
     * Parses a list of paths into a single string, adding the intermediate
     * folders into the Set of folders.  The paths are parsed either as
     * UTF or ASCII.
     */
    private String parseFileList(List<?> paths, Set<String> folders, boolean utf8) throws ValueException {
        StringBuilder sb = new StringBuilder();
        for(Iterator<?> i = paths.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if(!(o instanceof byte[]))
                throw new ValueException("info->files[x]->path[.utf-8][x] not a byte[]!");
            
            String current;
            if(utf8)
                current = StringUtils.getUTF8String((byte[])o);
            else
                current = StringUtils.getASCIIString((byte[])o);
            
            sb.append(File.separator);
            sb.append(CommonUtils.convertFileName(current));
            // if another path, this is a subfolder, so add it to folders
            if(i.hasNext())
                folders.add(sb.toString());
        }
        return sb.toString();
    }
    
    /** Returns either the UTF-8 version (if it exists) or the ASCII version of a String. */
    private String getPreferredString(Map<?, ?> info, String key) {
        String str = null;
        
        Object data = info.get(key + ".utf-8");
        if(data instanceof byte[]) {
            try {
                str = new String((byte[])data, Constants.UTF_8_ENCODING);
            } catch(Throwable t) {} // could throw any error if input bytes are invalid
        }
        
        if(str == null) {
            data = info.get(key);
            if(data instanceof byte[])
                str = StringUtils.getASCIIString((byte[])data);
        }
        
        return str;
    }
    
    /**
     * Calculates the infoHash of the map.  Because BT maps are stored
     * as String -> Object, and the keys are stored alphabetically, 
     * it is guaranteed that any two maps with identical keys & values
     * will have the same info hash when decoded & recoded.
     * 
     * @param infoMap
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
            BEncoder.encodeDict(baos, infoMap);
        } catch (IOException ioe) {
            ErrorService.error(ioe);
        }
        
        MessageDigest md = new SHA1();
        return md.digest(baos.toByteArray());
    }

    public String getAnnounce() {
        return announce;
    }

    public List<BTFileData> getFiles() {
        return files;
    }
    
    public Set<String> getFolders() {
        return folders;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public Long getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public Long getPieceLength() {
        return pieceLength;
    }

    public byte[] getPieces() {
        return pieces;
    }
    
    public void clearPieces() {
        pieces = null;
    }
    
    public static class BTFileData {
        private final Long length;
        private final String path;
        
        BTFileData(Long length, String path) {
            this.length = length;
            this.path = path;
        }

        public Long getLength() {
            return length;
        }

        public String getPath() {
            return path;
        }
    }

    
}
