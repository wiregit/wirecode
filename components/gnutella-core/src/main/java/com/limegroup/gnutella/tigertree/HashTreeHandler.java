package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.dime.*;
import com.limegroup.gnutella.util.UUID;
import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Gregorio Roper
 * 
 * Class handling all the reading and writing of HashTrees to the network
 */
class HashTreeHandler {
    private static final Log LOG = LogFactory.getLog(HashTreeHandler.class);

    private static final String SERIALIZED_TREE_TYPE =
        "http://open-content.net/spec/thex/breadthfirst";
    private static final String XML_TYPE = "text/xml";
    private static final byte[] TREE_TYPE_BYTES;
    private static final byte[] XML_TYPE_BYTES;
    
    static {
        byte[] tree;
        byte[] xml;
        try {
            tree = SERIALIZED_TREE_TYPE.getBytes("UTF-8");
            xml = XML_TYPE.getBytes("UTF-8");
        } catch(UnsupportedEncodingException uee) {
            tree = SERIALIZED_TREE_TYPE.getBytes();
            xml = XML_TYPE.getBytes();
        }
        TREE_TYPE_BYTES = tree;
        XML_TYPE_BYTES = xml;
    }

    private static final String DIGEST =
        "http://open-content.net/spec/digest/tiger";    
    private static final String SYSTEM_ID =
        "http://open-content.net/spec/thex/thex.dtd";
    private static final String SYSTEM_STRING = "SYSTEM";
    
    private static final String XML_TREE_DESC_START =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<!DOCTYPE hashtree " + SYSTEM_STRING + " \"" + SYSTEM_ID + "\">"
        + "<hashtree>";
    private static final String XML_TREE_DESC_END = "</hashtree>";

    private static int HASH_SIZE = 24;

    private final HashTree TREE;
    private final List NODES;
    private final byte[] ROOT_HASH;
    private final UUID URI;

    /**
     * Constructs a new handler for sending
     * @param tree
     *            the <tt>HashTree</tt> to construct this message from
     */
    public HashTreeHandler(HashTree tree) {
        LOG.trace("creating HashTreeDIMEMessage for sending");
        TREE = tree;
        NODES = TREE.getNodes();
        ROOT_HASH = Base32.decode(TREE.getRootHash());
        URI = UUID.nextUUID();
    }
    
    /**
     * Creates a HashTreeHandler with data read from the network
     * 
     * @param fileSize
     * @param xml
     * @param hashtree
     * @param root32
     * @throws IOException
     */
    private HashTreeHandler(long fileSize, String xml, byte[] hashtree,
                            String root32) throws IOException {
        XMLTreeDescription xtd = new XMLTreeDescription(xml);
        if (!xtd.isValid())
            throw new IOException(
                "invalid XMLTreeDescription " + xtd.toString());
        if (xtd.getFileSize() != fileSize)
            throw new IOException(
                "file size attribute was "
                    + xtd.getFileSize()
                    + " expected "
                    + fileSize);

        HashTreeRecord htr = new HashTreeRecord(hashtree);

        if (!Base32.encode(htr.getRoot()).equals(root32))
            throw new IOException("Root hashes do not match");

        NODES = htr.getNodes(fileSize);
        ROOT_HASH = htr.getRoot();
        TREE = null;
        URI = null;
    }    

    /**
     * reads a new HashTreeHandler from an InputStream
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from
     * @param fileSize
     *            the size of the file we expect the hash tree for
     * @param root32
     *            Base32 encoded root hash
     * @return HashTreeHandler containing the HashTree from the InputStream
     * @throws IOException
     *             in case of a problem reading from the InputStream
     * @see com.limegroup.gnutella.dime.AbstractDIMEMessage
     */
    static HashTreeHandler read(InputStream is, long fileSize, String root32)
      throws IOException {
        LOG.trace("creating HashTreeDIMEMessage from network");
        DIMEParser parser = new DIMEParser(is);
        DIMERecord xmlRecord = parser.nextRecord();
        DIMERecord treeRecord = parser.nextRecord();
        if(LOG.isDebugEnabled()) {
            if(parser.hasNext())
                LOG.debug("more elements in the dime record.");
            LOG.debug("xml id: [" + xmlRecord.getIdentifier() + "]");
            LOG.debug("xml type: [" + xmlRecord.getTypeString() + "]");
            LOG.debug("tree id: [" + treeRecord.getIdentifier() + "]");
            LOG.debug("tree type: [" + treeRecord.getTypeString() + "]");
            LOG.debug("xml type num: [" + xmlRecord.getTypeId() + "]");
            LOG.debug("tree type num: [" + treeRecord.getTypeId() + "]");
        }   
        String xml = new String(xmlRecord.getData(), "UTF-8");
        byte[] tree = treeRecord.getData();
        return new HashTreeHandler(fileSize, xml, tree, root32);

    }

    /**
     * method for writing a HashTree to an OutputStream
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to os.
     */
    public void write(OutputStream os) throws IOException {
        DIMEGenerator gen = new DIMEGenerator();
        gen.add(createXMLRecord());
        gen.add(createTreeRecord());
        gen.write(os);
    }

    /**
     * @return Returns the Nodes.
     */
    public List getNodes() {
        return NODES;
    }

    /**
     * @return Returns the RootHash.
     */
    public byte[] getRootHash() {
        return ROOT_HASH;
    }

    /*
     * Creates new DIMERecord for sending this hashtree in a DIMEMessage
     */
    private DIMERecord createXMLRecord() {
        String xml =
            XML_TREE_DESC_START
            + "<file size='"
            + TREE.getFileSize()
            + "' segmentsize='"
            + HashTree.BLOCK_SIZE
            + "'/>"
            + "<digest algorithm='"
            + DIGEST
            + "' outputsize='"
            + HASH_SIZE
            + "'/>"
            + "<serializedtree depth='"
            + TREE.getDepth()
            + "' type='"
            + SERIALIZED_TREE_TYPE
            + "' uri='"
            + URI
            + "'/>"
            + XML_TREE_DESC_END;
        try {
            return DIMERecord.create(DIMERecord.TYPE_MEDIA_TYPE,
                                     null,
                                     null,
                                     XML_TYPE_BYTES,
                                     xml.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            LOG.debug(uee);
            return null;
        }
    }

    /*
     * Creates new DIMERecord for sending this hashtree
     */
    private DIMERecord createTreeRecord() {
        List[] generations = new ArrayList[TREE.getDepth() + 1];
        generations[TREE.getDepth()] = new ArrayList(TREE.getNodes());
        int size = TREE.getNodes().size();
        for (int i = TREE.getDepth() - 1; i >= 0; i--) {
            generations[i] = new ArrayList(
                HashTree.createParentGeneration(generations[i + 1]));
            size += generations[i].size();
        }
        byte[] data = new byte[size * HASH_SIZE];

        int offset = 0;
        for (int i = 0; i <= TREE.getDepth(); i++) {
            Iterator iter = generations[i].iterator();
            while (iter.hasNext()) {
                System.arraycopy(iter.next(), 0, data, offset, HASH_SIZE);
                offset += HASH_SIZE;
            }
        }
        Assert.that(offset == size * HASH_SIZE);
        try {
            return DIMERecord.create(DIMERecord.TYPE_ABSOLUTE_URI,
                                     null,
                                     URI.toString().getBytes("UTF-8"),
                                     TREE_TYPE_BYTES,
                                     data);
        } catch(UnsupportedEncodingException uee) {
            LOG.debug(uee);
            return null;
        }
    }

    /**
     * @author Gregorio Roper
     * 
     * private class holding the XML Tree description
     */
    private class XMLTreeDescription {
        private static final int UNKNOWN = 0;
        private static final int VALID = 1;
        private static final int INVALID = 2;
        private int _parsed = UNKNOWN;
        private long _fileSize = 0;
        private int _blockSize = 0;
        private String _algorithm = null;
        private int _hashSize = 0;
        private String _serializationType = null;
        private String data;        

        protected XMLTreeDescription(String xml) {
            data = xml;
        }

        /*
         * Accessor for the _fileSize;
         */
        long getFileSize() {
            return _fileSize;
        }

        /**
         * Check if the xml tree description if the tree is what we expected
         */
        boolean isValid() {
            if (_parsed == UNKNOWN) {
                _parsed = parse() ? VALID : INVALID;
            } else if(_parsed == INVALID) {
                return false;
            } else if (_blockSize != HashTree.BLOCK_SIZE) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unexpected block size: " + _blockSize);
                return false;
            } else if (!DIGEST.equals(_algorithm)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unsupported digest algorithm: " + _algorithm);
                return false;
            } else if (_hashSize != HASH_SIZE) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unexpected block size: " + _blockSize);
                return false;
            } else if (!SERIALIZED_TREE_TYPE.equals(_serializationType)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unexpected serialization type: " + 
                              _serializationType);
                return false;
            }
            return true;
        }

        /*
         * A simple parsing method for reading the xml tree description.
         */
        private boolean parse() {
            // hack!
            // Shareaza sends invalid XML,
            if (data.indexOf("system") > 0 && data.indexOf("system") < data.indexOf(SYSTEM_ID)) {
                StringBuffer buf = new StringBuffer(data);
                int offset = data.indexOf("system");
                buf.replace(
                    offset,
                    offset + SYSTEM_STRING.length(),
                    SYSTEM_STRING);
                data = buf.toString();
            }
            
            if (LOG.isDebugEnabled())
                LOG.debug("XMLTreeDescription read: " + data);

            DOMParser parser = new DOMParser();
            InputSource is = new InputSource(new StringReader(data));
            is.setSystemId(SYSTEM_ID);
            is.setEncoding("UTF-8");

            try {
                parser.parse(is);
            } catch (IOException ioe) {
                LOG.debug(ioe);
                return false;
            } catch (SAXException saxe) {
                LOG.debug(saxe);
                return false;
            }

            Document doc = parser.getDocument();
            Node treeDesc = doc.getElementsByTagName("hashtree").item(0);
            if (treeDesc == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't find hashtree element: " + data);
                return false;
            }

            NodeList nodes = treeDesc.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    if (el.getTagName().equals("file"))
                        parseFileElement(el);
                    else if (el.getTagName().equals("digest"))
                        parseDigestElement(el);
                    else if (el.getTagName().equals("serializedtree"))
                        parseSerializedtreeElement(el);
                }
            }
            return true;
        }

        private void parseFileElement(Element e) {
            try {
                _fileSize = Long.parseLong(e.getAttribute("size"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse file size: " + e.getNodeValue(), 
                              nfe);
            }

            try {
                _blockSize = Integer.parseInt(e.getAttribute("segmentsize"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse block size: " + e.getNodeValue(),
                              nfe);
            }
        }

        private void parseDigestElement(Element e) {
            _algorithm = e.getAttribute("algorithm");
            try {
                _hashSize = Integer.parseInt(e.getAttribute("outputsize"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse hash size: " + e.getNodeValue(),
                              nfe);
            }
        }

        private void parseSerializedtreeElement(Element e) {
            _serializationType = e.getAttribute("type");
            try {
                // value is ignored, but if it can't be parsed we should add
                // a notice to the Log
                Integer.parseInt(e.getAttribute("depth"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse depth: " + e.getNodeValue(),
                              nfe);
            }
        }
    }

    /**
     * @author Gregorio Roper
     * 
     * private class holding serialized HashTree
     */
    private class HashTreeRecord {
        private final byte[] DATA;
        
        protected HashTreeRecord(byte[] data) {
            DATA = data;
        }

        /*
         * Accessor for root hash.
         */
        byte[] getRoot() {
            if (DATA.length < HASH_SIZE)
                return null;
            byte[] ret = new byte[HASH_SIZE];
            System.arraycopy(DATA, 0, ret, 0, HASH_SIZE);
            return ret;
        }

        /*
         * Returns a List containing a generation for nodes from the hash tree
         * 
         * @throws IOException if the hashes did not match.
         */
        List getNodes(long fileSize) throws IOException {
            int depth = HashTree.calculateDepth(fileSize);
            List hashes = new ArrayList();
            byte[] data = DATA;

            if (data.length % HASH_SIZE != 0) {
                if (LOG.isDebugEnabled())
                    LOG.debug("illegal size of data field for HashTreeRecord");
                // this is not good but we will continue anyway
            }

            // read the hashes from the data field
            for (int i = 0; i + HASH_SIZE <= data.length; i += HASH_SIZE) {
                byte[] hash = new byte[HASH_SIZE];
                System.arraycopy(data, i, hash, 0, HASH_SIZE);
                hashes.add(hash);
            }

            String root32 = Base32.encode(getRoot());
            // iterator of all hashes we read
            Iterator hashIterator = hashes.iterator();
            // the current generation we are working on
            List generation = new ArrayList();
            // stores the last verified generation
            List parent = null;
            // index of the generation we are working on.
            int genIndex = 0;
            while (genIndex <= depth && hashIterator.hasNext()) {
                byte[] hash = (byte[]) hashIterator.next();
                generation.add(hash);
                if (parent == null) {
                    // add generation 0 containing the root hash
                    genIndex++;
                    parent = generation;
                    generation = new ArrayList();
                } else if (generation.size() > parent.size() * 2) {
                    // the current generation is already too big => the hash
                    // tree is corrupted, abort at once!
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("parent");
                        String str = "";
                        for (Iterator iter = parent.iterator(); iter.hasNext(); ) {
                            str = str + Base32.encode((byte[])iter.next()) + "; "; 
                        }
                        LOG.debug(str);
                        str = "";
                        LOG.debug("newparent");
                        List newparent = HashTree.createParentGeneration(generation);
                        for (Iterator iter = newparent.iterator(); iter.hasNext(); ) {
                            str = str + Base32.encode((byte[])iter.next()) + "; "; 
                        }
                        LOG.debug(str);
                        str = "";
                        LOG.debug("generation");
                        for (Iterator iter = generation.iterator(); iter.hasNext(); ) {
                            str = str + Base32.encode((byte[])iter.next()) + "; "; 
                        }
                        LOG.debug(str);
                        str = "";

                    }
                    throw new IOException("corrupted hash tree detected");
                } else if (
                    generation.size() > parent.size()
                        && generation.size() <= parent.size() * 2) {
                    List newParent =
                        HashTree.createParentGeneration(generation);
                    // maybe we have a match, check them!
                    boolean match = true;
                    if (parent.size() == newParent.size()) {
                        for (int i = 0; match && i < parent.size(); i++) {
                            byte[] one = (byte[]) parent.get(i);
                            byte[] two = (byte[]) newParent.get(i);
                            for (int j = 0; match && j < HASH_SIZE; j++)
                                if (one[j] != two[j])
                                    match = false;
                        }
                        // the current generation is complete and verified!
                        if (match) {
                            genIndex++;
                            parent = generation;
                            generation = new ArrayList();
                        }
                    }
                }
            } // end of while

            // this is the last parentGeneration we have created.
            LOG.debug("Good hash tree received.");
            return parent;
        }
    }

}
