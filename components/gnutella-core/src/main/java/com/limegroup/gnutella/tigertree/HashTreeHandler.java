package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.dime.DIMEGenerator;
import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.util.UUID;

/**
 * @author Gregorio Roper
 * 
 * Class handling all the reading and writing of HashTrees to the network
 */
class HashTreeHandler {
    private static final Log LOG = LogFactory.getLog(HashTreeHandler.class);
    
    private static final String OUTPUT_TYPE = "application/dime";

    private static final String SERIALIZED_TREE_TYPE =
        "http://open-content.net/spec/thex/breadthfirst";
    private static final String XML_TYPE = "text/xml";

    private static final byte[] TREE_TYPE_BYTES =
        getBytes(SERIALIZED_TREE_TYPE);
    private static final byte[] XML_TYPE_BYTES =
        getBytes(XML_TYPE);

    private static final String DIGEST =
        "http://open-content.net/spec/digest/tiger";    

    private static final String DTD_PUBLIC_ID =
        "-//NET//OPEN-CONTENT//THEX 02//EN";
    private static final String DTD_SYSTEM_ID =
        "http://open-content.net/spec/thex/thex.dtd";
    private static final String DTD_ENTITY =
        "<!ELEMENT hashtree (file,digest,serializedtree)>" +
        "<!ELEMENT file EMPTY>" +
        "<!ATTLIST file size CDATA #REQUIRED>" +
        "<!ATTLIST file segmentsize CDATA #REQUIRED>" +
        "<!ELEMENT digest EMPTY>" +
        "<!ATTLIST digest algorithm CDATA #REQUIRED>" +
        "<!ATTLIST digest outputsize CDATA #REQUIRED>" +
        "<!ELEMENT serializedtree EMPTY>" +
        "<!ATTLIST serializedtree depth CDATA #REQUIRED>"+
        "<!ATTLIST serializedtree type CDATA #REQUIRED>" +
        "<!ATTLIST serializedtree uri CDATA #REQUIRED>";

    private static final String SYSTEM_STRING = "SYSTEM";
    
    private static final String XML_TREE_DESC_START =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<!DOCTYPE hashtree " + SYSTEM_STRING + " \"" + DTD_SYSTEM_ID + "\">"
        + "<hashtree>";
    private static final String XML_TREE_DESC_END = "</hashtree>";

    private static int HASH_SIZE = 24;
    
    /**
     * Returns the bytes of a string in UTF-8 format, or in the default
     * format if UTF-8 failed for whatever reason.
     */
    private static byte[] getBytes(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch(UnsupportedEncodingException uee) {
            LOG.debug(string, uee);
            return string.getBytes();
        }
    }
        
    
    
    /////////////////////////       WRITING        ///////////////////////

    /** 
     * The generator containing the DIME message to send.
     */
    private final DIMEGenerator GENERATOR;

    /**
     * Constructs a new handler for sending
     * @param tree
     *            the <tt>HashTree</tt> to construct this message from
     */
    public HashTreeHandler(HashTree tree) {
        LOG.trace("creating HashTreeHandler for sending");
        UUID uri = UUID.nextUUID();
        GENERATOR = new DIMEGenerator();
        GENERATOR.add(new XMLRecord(tree, uri));
        GENERATOR.add(new TreeRecord(tree, uri));
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
        GENERATOR.write(os);
    }
    
    /**
     * Determines the length of the written data.
     */
    public int getLength() {
        return GENERATOR.getLength();
    }

    /**
     * Determines the mime type of the output.
     */
    public String getType() {
        return OUTPUT_TYPE;
    }

    /**
     * A simple XML DIMERecord.
     */
    private static class XMLRecord extends DIMERecord {
        XMLRecord(HashTree tree, UUID uri) {
            super(DIMERecord.TYPE_MEDIA_TYPE, null, null,
                  XML_TYPE_BYTES, getXML(tree, uri));
        }
        
        /**
         * Constructs the XML bytes.
         */
        private static byte[] getXML(HashTree tree, UUID uri) {
            String xml =
                XML_TREE_DESC_START
                + "<file size='"
                + tree.getFileSize()
                + "' segmentsize='"
                + HashTree.BLOCK_SIZE
                + "'/>"
                + "<digest algorithm='"
                + DIGEST
                + "' outputsize='"
                + HASH_SIZE
                + "'/>"
                + "<serializedtree depth='"
                + tree.getDepth()
                + "' type='"
                + SERIALIZED_TREE_TYPE
                + "' uri='uuid:"
                + uri
                + "'/>"
                + XML_TREE_DESC_END;
            return getBytes(xml);
        }
    }
    
    /**
     * Private DIMERecord for a Tree.
     */
    private static class TreeRecord extends DIMERecord {
        /**
         * The tree of this record.
         */
        private final HashTree TREE;
        
        /**
         * The length of the tree.
         */
        private final int LENGTH;
        
        TreeRecord(HashTree tree, UUID uri) {
            super(DIMERecord.TYPE_ABSOLUTE_URI, null,
                  getBytes("uuid:" + uri),
                  TREE_TYPE_BYTES, null);
            TREE = tree;
            LENGTH = TREE.getNodeCount() * HASH_SIZE;
        }

        /**
         * Writes the tree's data to the specified output stream.
         */
        public void writeData(OutputStream out) throws IOException {
            for(Iterator i = TREE.getAllNodes().iterator(); i.hasNext(); ) {
                Iterator iter = ((List)i.next()).iterator();
                while (iter.hasNext())
                    out.write((byte[])iter.next());
            }
            writePadding(getDataLength(), out);
        }
    
        /**
         * Determines the length of the data.
         */
        public int getDataLength() {
            return LENGTH;
        }
    }
    


        //////////////////////    READING   /////////////////////


    /**
     * Reads a HashTree in DIME format from an input stream.
     * Returns the list of all nodes of the tree.
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from
     * @param fileSize
     *            the size of the file we expect the hash tree for
     * @param root32
     *            Base32 encoded root hash
     * @return The list of all nodes in this tree.
     * @throws IOException
     *             in case of a problem reading from the InputStream
     */
    static List read(InputStream is, long fileSize, String root32)
      throws IOException {
        LOG.trace("creating HashTreeHandler from network");
        DIMEParser parser = new DIMEParser(is);
        DIMERecord xmlRecord = parser.nextRecord();
        DIMERecord treeRecord = parser.nextRecord();
        if(LOG.isDebugEnabled()) {
            LOG.debug("xml id: [" + xmlRecord.getIdentifier() + "]");
            LOG.debug("xml type: [" + xmlRecord.getTypeString() + "]");
            LOG.debug("tree id: [" + treeRecord.getIdentifier() + "]");
            LOG.debug("tree type: [" + treeRecord.getTypeString() + "]");
            LOG.debug("xml type num: [" + xmlRecord.getTypeId() + "]");
            LOG.debug("tree type num: [" + treeRecord.getTypeId() + "]");
        }

        while(parser.hasNext()) {
            if(LOG.isWarnEnabled())
                LOG.warn("more elements in the dime record.");
            parser.nextRecord(); // ignore them.
        }
                
        String xml = new String(xmlRecord.getData(), "UTF-8");
        byte[] hashTree = treeRecord.getData();
        
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

        HashTreeDescription htr = new HashTreeDescription(hashTree);

        if (!Base32.encode(htr.getRoot()).equals(root32))
            throw new IOException("Root hashes do not match");

        return htr.getAllNodes(fileSize);
    }    

    /**
     * @author Gregorio Roper
     * 
     * private class holding the XML Tree description
     */
    private static class XMLTreeDescription {
        private static final int UNKNOWN = 0;
        private static final int VALID = 1;
        private static final int INVALID = 2;
        private int _parsed = UNKNOWN;
        private long _fileSize = 0;
        private int _blockSize = 0;
        private String _algorithm = null;
        private int _hashSize = 0;
        private String _serializationType = null;
        private String _uri;
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
         * Accessor for the _uri;
         */
        String getURI() {
            return _uri;
        }

        /**
         * Check if the xml tree description if the tree is what we expected
         */
        boolean isValid() {
            if (_parsed == UNKNOWN) {
                _parsed = parse() ? VALID : INVALID;
            }
            
            if(_parsed == INVALID) {
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
            int offset = data.indexOf("system");
            if (offset > 0 && offset < data.indexOf(DTD_SYSTEM_ID)) {
                data = data.substring(0, offset) + 
                       SYSTEM_STRING +
                       data.substring(offset + "system".length());
            }
            
            if (LOG.isDebugEnabled())
                LOG.debug("XMLTreeDescription read: " + data);

            DOMParser parser = new DOMParser();
            InputSource is = new InputSource(new StringReader(data));
            parser.setEntityResolver(new Resolver());


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
            _uri = e.getAttribute("uri");
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
     * A custom EntityResolver so we don't hit a website for resolving.
     */
    private static final class Resolver implements EntityResolver {
        public Resolver() {}

        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            if (systemId.equals(DTD_SYSTEM_ID)) {
                InputSource is = new InputSource(new StringReader(DTD_ENTITY));
                is.setPublicId(DTD_PUBLIC_ID);//optional
                is.setSystemId(DTD_SYSTEM_ID);//required
                return is;
            }
            //the parser will open a regular URI connection to the systemId
            //if we return null. Here we don't want this to occur...
            if (publicId == null)
                throw new SAXException("Can't resolve SYSTEM entity at '" +
                                       systemId + "'");
            else
                throw new SAXException("Can't resolve PUBLIC entity '" +
                                       publicId + "' at '" +
                                       systemId + "'");
        }
    }
    
    /**
     * @author Gregorio Roper
     * 
     * private class holding serialized HashTree
     */
    private static class HashTreeDescription {
        private final byte[] DATA;
        
        protected HashTreeDescription(byte[] data) {
            DATA = data;
        }

        /*
         * Accessor for root hash.
         */
        byte[] getRoot() throws IOException {
            if (DATA.length < HASH_SIZE)
                throw new IOException("invalid data");
            byte[] ret = new byte[HASH_SIZE];
            System.arraycopy(DATA, 0, ret, 0, HASH_SIZE);
            return ret;
        }

        /*
         * Returns a List containing a generation for nodes from the hash tree
         * 
         * @throws IOException if the hashes did not match.
         */
        List getAllNodes(long fileSize) throws IOException {
            int depth = HashTree.calculateDepth(fileSize);
            List hashes = new ArrayList();
            byte[] data = DATA;

            if (data.length % HASH_SIZE != 0) {
                if (LOG.isDebugEnabled())
                    LOG.debug("illegal size of data field for HashTree");
                throw new IOException("corrupted hash tree detected");
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
            List generation = new ArrayList(1);
            // stores the last verified generation
            List parent = null;
            // index of the generation we are working on.
            int genIndex = 0;
            // whether or not the current row is verified.
            boolean verified = false;
            
            List allNodes = new ArrayList(depth+1);
            
            // Iterate through the read elements and see if they match
            // what we calculate.
            // Only calculate when we've read enough of the current
            // generation that it may be a full generation.
            // Imagine the trees:
            //           A
            //        /     \
            //       B       C
            //      / \       \
            //     D  E        C
            //    /\  /\        \
            //   F G H I         C
            //              or
            //           A
            //        /     \
            //       B       C
            //      / \     / \
            //     D  E    F   G
            //    /\  /\  /\   /\
            //   I H J K L M  N O
            //
            // In both cases, we only have read the full child gen.
            // when we've read parent.size()*2 or parent.size()*2-1
            // child nodes.
            // If it didn't match on parent.size()*2, and
            // the child has greater than that, then the tree is
            // corrupt.
            
            while (genIndex <= depth && hashIterator.hasNext()) {
                verified = false;
                byte[] hash = (byte[]) hashIterator.next();
                generation.add(hash);
                if (parent == null) {
                    verified = true;
                    // add generation 0 containing the root hash
                    genIndex++;
                    parent = generation;
                    allNodes.add(generation);
                    generation = new ArrayList(2);
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
                } else if (generation.size() == parent.size() * 2 - 1 ||
                           generation.size() == parent.size() * 2) {
                    List calculatedParent =
                        HashTree.createParentGeneration(generation);
                    if(isMatching(parent, calculatedParent)) {
                        // the current generation is complete and verified!
                        genIndex++;
                        parent = generation;
                        allNodes.add(Collections.unmodifiableList(generation));
                        // only create room for a new generation if one exists
                        if(genIndex <= depth && hashIterator.hasNext())
                            generation = new ArrayList(parent.size() * 2);
                        verified = true;
                    }
                }
            } // end of while
            
            // If the current row was unable to verify, fail.
            // In mostly all cases, this will occur with the inner if
            // statement in the above loop.  However, if the last row
            // is the one that had the problem, the loop will not catch it.
            if(!verified)
                throw new IOException("corrupted hash tree detected");

            LOG.debug("Valid hash tree received.");
            return allNodes;
        }
        
        /**
         * Determines if two lists of byte arrays completely match.
         */
        private boolean isMatching(List a, List b) {
            if (a.size() == b.size()) {
                for (int i = 0; i < a.size(); i++) {
                    byte[] one = (byte[]) a.get(i);
                    byte[] two = (byte[]) b.get(i);
                    if(!Arrays.equals(one, two))
                        return false;
                }
                return true;
            }
            return false;
        }
    }

}
