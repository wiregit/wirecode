package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
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
import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

/**
 * @author Gregorio Roper
 * 
 * Class handling all the reading and writing of HashTrees to the network
 */
public class HashTreeDIMEMessage {
    static transient int HASH_SIZE = 24;

    private static final String DIGEST =
        "http://open-content.net/spec/digest/tiger";

    private static final String SERIALIZED_TREE_TYPE =
        "http://open-content.net/spec/thex/breadthfirst";

    private static final String XML_RECORD_ID = "xtd";
    private static final String TREE_RECORD_ID = "tree";

    private static final String XML_TREE_DESC_END = "</hashtree>";
    private static final String XML_TREE_DESC_START =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE hashtree SYSTEM " 
            + "\"http://open-content.net/spec/thex/thex.dtd\">"
            + "<hashtree>";

    private static final String SYSTEM_ID =
        "http://open-content.net/spec/thex/thex.dtd";
    private static final String SYSTEM_STRING = "SYSTEM";

    static Log LOG = HashTree.LOG;

    private final HashTree TREE;
    private final List NODES;
    private final byte[] ROOT_HASH;

    /**
     * Constructs a new DIMEMessage for sending
     * 
     * @param thexUri
     *            the <tt>String</tt> containing the URI fir this message.
     * @param tree
     *            the <tt>HashTree</tt> to construct this message from
     */
    public HashTreeDIMEMessage(String thexUri, HashTree tree) {
        //super(TYPE_URI, thexUri);
        LOG.trace("creating HashTreeDIMEMessage for sending");
        TREE = tree;
        NODES = TREE.getNodes();
        ROOT_HASH = Base32.decode(TREE.getRootHash());
    }

    /**
     * reads a new HashTreeDIMEMessage from an InputStream
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from
     * @param fileSize
     *            the size of the file we expect the hash tree for
     * @param root32
     *            Base32 encoded root hash
     * @return HashTreeDIMEMessage containing the HashTree from the InputStream
     * @throws IOException
     *             in case of a problem reading from the InputStream
     * @see com.limegroup.gnutella.dime.AbstractDIMEMessage
     */
    static HashTreeDIMEMessage read(
        InputStream is,
        long fileSize,
        String root32)
        throws IOException {
        LOG.trace("creating HashTreeDIMEMessage from network");
        //is = new PrintingInputStream(is);
        DIMEParser parser = new DIMEParser(is);
        DIMERecord xml = parser.nextRecord();
        DIMERecord tree = parser.nextRecord();
        if(LOG.isDebugEnabled()) {
            if(parser.hasNext())
                LOG.debug("more elements in the dime record.");
            LOG.debug("xml id: [" + xml.getIdentifier() + "]");
            LOG.debug("xml type: [" + xml.getTypeString() + "]");
            LOG.debug("tree id: [" + tree.getIdentifier() + "]");
            LOG.debug("tree type: [" + tree.getTypeString() + "]");
            LOG.debug("xml type num: [" + xml.getTypeId() + "]");
            LOG.debug("tree type num: [" + tree.getTypeId() + "]");
        }   
            
        return new HashTreeDIMEMessage(fileSize, xml, tree, root32);

    }

    /**
     * method for writing a DIMEMessage to an OutputStream
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to os.
     * @see com.limegroup.gnutella.dime.AbstractDIMEMessage#write(java.io.OutputStream)
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

    /**
     * Creates a HashTreeDIMEMessage read from the network
     * 
     * @param fileSize
     * @param xml
     * @param hashtree
     * @param root32
     * @throws IOException
     */
    private HashTreeDIMEMessage(long fileSize, DIMERecord xml,
                                DIMERecord hashtree, String root32)
                                throws IOException {

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
        
        if (!hashtree.isLastRecord())
            LOG.debug("unexpected message in THEX transfer");        
    }

    /*
     * Creates new DIMERecord for sending this hashtree in a DIMEMessage
     */
    private DIMERecord createXMLRecord() {
        String xml = XML_TREE_DESC_START;
        xml += "<file size='"
            + TREE.getFileSize()
            + "' segmentsize='"
            + HashTree.BLOCK_SIZE
            + "'/>";
        xml += "<digest algorithm='"
            + DIGEST
            + "' outputsize='"
            + HASH_SIZE
            + "'/>";
        xml += "<serializedtree depth='"
            + TREE.getDepth()
            + "' type='"
            + SERIALIZED_TREE_TYPE
            + "' uri='"
            + TREE.getThexURI()
            + "'/>";
        xml += XML_TREE_DESC_END;
        try {
            return DIMERecord.create(DIMERecord.TYPE_MEDIA_TYPE,
                                     null,
                                     XML_RECORD_ID.getBytes("UTF-8"),
                                     "application/xml".getBytes("UTF-8"),
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
                                     TREE_RECORD_ID.getBytes("UTF-8"),
                                     "http://open-content.net/spec/thex/breadthfirst".getBytes("UTF-8"),
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
        private boolean _parsed = false;
        private long _fileSize = 0;
        private int _blockSize = 0;
        private String _algorithm = null;
        private int _hashSize = 0;
        private String _serializationType = null;
        private final byte[] DATA;

        protected XMLTreeDescription(DIMERecord record) {
            DATA = record.getData();
             LOG.debug(new String(DATA));
        }

        /*
         * Accessor for the _fileSize;
         */
        long getFileSize() {
            return _fileSize;
        }

        /*
         * Check if the xml tree description if the tree is what we expected
         */
        boolean isValid() {
            if (!_parsed)
                parse();
            if (_blockSize != HashTree.BLOCK_SIZE) {
                debug("unexpected block size: " + _blockSize);
                return false;
            } else if (!DIGEST.equals(_algorithm)) {
                debug("unsupported digest algorithm: " + _algorithm);
                return false;
            } else if (_hashSize != HASH_SIZE) {
                debug("unexpected block size: " + _blockSize);
                return false;
            } else if (!SERIALIZED_TREE_TYPE.equals(_serializationType)) {
                debug("unexpected serialization type: " + _serializationType);
                return false;
            }
            return true;
        }

        /*
         * A simple parsing method for reading the xml tree description.
         */
        private void parse() {
            String data;
            try {
                data = new String(DATA, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                debug(uee.toString());
                return;
            }

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
                debug(ioe.toString());
                return;
            } catch (SAXException saxe) {
                debug(saxe.toString());
                return;
            }

            Document doc = parser.getDocument();
            Node treeDesc = doc.getElementsByTagName("hashtree").item(0);
            if (treeDesc == null) {
                debug("couldn't find hashtree element: " + data);
                return;
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
            _parsed = true;
        }

        private void parseFileElement(Element e) {
            try {
                _fileSize = Long.parseLong(e.getAttribute("size"));
            } catch (NumberFormatException nfe) {
                debug("couldn't parse file size: " + e.getNodeValue());
            }

            try {
                _blockSize = Integer.parseInt(e.getAttribute("segmentsize"));
            } catch (NumberFormatException nfe) {
                debug("couldn't parse block size: " + e.getNodeValue());
            }
        }

        private void parseDigestElement(Element e) {
            _algorithm = e.getAttribute("algorithm");
            try {
                _hashSize = Integer.parseInt(e.getAttribute("outputsize"));
            } catch (NumberFormatException nfe) {
                debug("couldn't parse hash size: " + e.getNodeValue());
            }
        }

        private void parseSerializedtreeElement(Element e) {
            _serializationType = e.getAttribute("type");
            try {
                // value is ignored, but if it can't be parsed we should add
                // a notice to the Log
                Integer.parseInt(e.getAttribute("depth"));
            } catch (NumberFormatException nfe) {
                debug("couldn't parse depth: " + e.getNodeValue());
            }
        }

        private void debug(String str) {
            if (LOG.isDebugEnabled())
                LOG.debug(str);
        }
    }

    /**
     * @author Gregorio Roper
     * 
     * private class holding serialized HashTree
     */
    private class HashTreeRecord {
        private final byte[] DATA;
        
        protected HashTreeRecord(DIMERecord record) {
            DATA = record.getData();
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
            if (LOG.isDebugEnabled())
                LOG.debug("Good hash tree received.");
            return parent;
        }
    }

}
