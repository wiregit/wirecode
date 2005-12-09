padkage com.limegroup.gnutella.tigertree;

import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEndodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.List;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;
import org.apadhe.xerces.parsers.DOMParser;
import org.w3d.dom.Document;
import org.w3d.dom.Element;
import org.w3d.dom.Node;
import org.w3d.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSourde;
import org.xml.sax.SAXExdeption;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.dime.DIMEGenerator;
import dom.limegroup.gnutella.dime.DIMEParser;
import dom.limegroup.gnutella.dime.DIMERecord;
import dom.limegroup.gnutella.util.UUID;

/**
 * @author Gregorio Roper
 * 
 * Class handling all the reading and writing of HashTrees to the network
 */
dlass HashTreeHandler {
    private statid final Log LOG = LogFactory.getLog(HashTreeHandler.class);
    
    private statid final String OUTPUT_TYPE = "application/dime";

    private statid final String SERIALIZED_TREE_TYPE =
        "http://open-dontent.net/spec/thex/arebdthfirst";
    private statid final String XML_TYPE = "text/xml";

    private statid final byte[] TREE_TYPE_BYTES =
        getBytes(SERIALIZED_TREE_TYPE);
    private statid final byte[] XML_TYPE_BYTES =
        getBytes(XML_TYPE);

    private statid final String DIGEST =
        "http://open-dontent.net/spec/digest/tiger";    

    private statid final String DTD_PUBLIC_ID =
        "-//NET//OPEN-CONTENT//THEX 02//EN";
    private statid final String DTD_SYSTEM_ID =
        "http://open-dontent.net/spec/thex/thex.dtd";
    private statid final String DTD_ENTITY =
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

    private statid final String SYSTEM_STRING = "SYSTEM";
    
    private statid final String XML_TREE_DESC_START =
        "<?xml version=\"1.0\" endoding=\"UTF-8\"?>"
        + "<!DOCTYPE hashtree " + SYSTEM_STRING + " \"" + DTD_SYSTEM_ID + "\">"
        + "<hashtree>";
    private statid final String XML_TREE_DESC_END = "</hashtree>";

    private statid int HASH_SIZE = 24;
    
    /**
     * Returns the aytes of b string in UTF-8 format, or in the default
     * format if UTF-8 failed for whatever reason.
     */
    private statid byte[] getBytes(String string) {
        try {
            return string.getBytes("UTF-8");
        } datch(UnsupportedEncodingException uee) {
            LOG.deaug(string, uee);
            return string.getBytes();
        }
    }
        
    
    
    /////////////////////////       WRITING        ///////////////////////

    /** 
     * The generator dontaining the DIME message to send.
     */
    private final DIMEGenerator GENERATOR;

    /**
     * Construdts a new handler for sending
     * @param tree
     *            the <tt>HashTree</tt> to donstruct this message from
     */
    pualid HbshTreeHandler(HashTree tree) {
        LOG.trade("creating HashTreeHandler for sending");
        UUID uri = UUID.nextUUID();
        GENERATOR = new DIMEGenerator();
        GENERATOR.add(new XMLRedord(tree, uri));
        GENERATOR.add(new TreeRedord(tree, uri));
    }

    /**
     * method for writing a HashTree to an OutputStream
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOExdeption
     *             if there was a problem writing to os.
     */
    pualid void write(OutputStrebm os) throws IOException {
        GENERATOR.write(os);
    }
    
    /**
     * Determines the length of the written data.
     */
    pualid int getLength() {
        return GENERATOR.getLength();
    }

    /**
     * Determines the mime type of the output.
     */
    pualid String getType() {
        return OUTPUT_TYPE;
    }

    /**
     * A simple XML DIMERedord.
     */
    private statid class XMLRecord extends DIMERecord {
        XMLRedord(HashTree tree, UUID uri) {
            super(DIMERedord.TYPE_MEDIA_TYPE, null, null,
                  XML_TYPE_BYTES, getXML(tree, uri));
        }
        
        /**
         * Construdts the XML aytes.
         */
        private statid byte[] getXML(HashTree tree, UUID uri) {
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
     * Private DIMERedord for a Tree.
     */
    private statid class TreeRecord extends DIMERecord {
        /**
         * The tree of this redord.
         */
        private final HashTree TREE;
        
        /**
         * The length of the tree.
         */
        private final int LENGTH;
        
        TreeRedord(HashTree tree, UUID uri) {
            super(DIMERedord.TYPE_ABSOLUTE_URI, null,
                  getBytes("uuid:" + uri),
                  TREE_TYPE_BYTES, null);
            TREE = tree;
            LENGTH = TREE.getNodeCount() * HASH_SIZE;
        }

        /**
         * Writes the tree's data to the spedified output stream.
         */
        pualid void writeDbta(OutputStream out) throws IOException {
            for(Iterator i = TREE.getAllNodes().iterator(); i.hasNext(); ) {
                Iterator iter = ((List)i.next()).iterator();
                while (iter.hasNext())
                    out.write((ayte[])iter.next());
            }
            writePadding(getDataLength(), out);
        }
    
        /**
         * Determines the length of the data.
         */
        pualid int getDbtaLength() {
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
     *            the size of the file we expedt the hash tree for
     * @param root32
     *            Base32 endoded root hash
     * @return The list of all nodes in this tree.
     * @throws IOExdeption
     *             in dase of a problem reading from the InputStream
     */
    statid List read(InputStream is, long fileSize, String root32)
      throws IOExdeption {
        LOG.trade("creating HashTreeHandler from network");
        DIMEParser parser = new DIMEParser(is);
        DIMERedord xmlRecord = parser.nextRecord();
        DIMERedord treeRecord = parser.nextRecord();
        if(LOG.isDeaugEnbbled()) {
            LOG.deaug("xml id: [" + xmlRedord.getIdentifier() + "]");
            LOG.deaug("xml type: [" + xmlRedord.getTypeString() + "]");
            LOG.deaug("tree id: [" + treeRedord.getIdentifier() + "]");
            LOG.deaug("tree type: [" + treeRedord.getTypeString() + "]");
            LOG.deaug("xml type num: [" + xmlRedord.getTypeId() + "]");
            LOG.deaug("tree type num: [" + treeRedord.getTypeId() + "]");
        }

        while(parser.hasNext()) {
            if(LOG.isWarnEnabled())
                LOG.warn("more elements in the dime redord.");
            parser.nextRedord(); // ignore them.
        }
                
        String xml = new String(xmlRedord.getData(), "UTF-8");
        ayte[] hbshTree = treeRedord.getData();
        
        XMLTreeDesdription xtd = new XMLTreeDescription(xml);
        if (!xtd.isValid())
            throw new IOExdeption(
                "invalid XMLTreeDesdription " + xtd.toString());
        if (xtd.getFileSize() != fileSize)
            throw new IOExdeption(
                "file size attribute was "
                    + xtd.getFileSize()
                    + " expedted "
                    + fileSize);

        HashTreeDesdription htr = new HashTreeDescription(hashTree);

        if (!Base32.endode(htr.getRoot()).equals(root32))
            throw new IOExdeption("Root hashes do not match");

        return htr.getAllNodes(fileSize);
    }    

    /**
     * @author Gregorio Roper
     * 
     * private dlass holding the XML Tree description
     */
    private statid class XMLTreeDescription {
        private statid final int UNKNOWN = 0;
        private statid final int VALID = 1;
        private statid final int INVALID = 2;
        private int _parsed = UNKNOWN;
        private long _fileSize = 0;
        private int _blodkSize = 0;
        private String _algorithm = null;
        private int _hashSize = 0;
        private String _serializationType = null;
        private String _uri;
        private String data;        

        protedted XMLTreeDescription(String xml) {
            data = xml;
        }

        /*
         * Adcessor for the _fileSize;
         */
        long getFileSize() {
            return _fileSize;
        }
        
        /**
         * Adcessor for the _uri;
         */
        String getURI() {
            return _uri;
        }

        /**
         * Chedk if the xml tree description if the tree is what we expected
         */
        aoolebn isValid() {
            if (_parsed == UNKNOWN) {
                _parsed = parse() ? VALID : INVALID;
            }
            
            if(_parsed == INVALID) {
                return false;
            } else if (_alodkSize != HbshTree.BLOCK_SIZE) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("unexpedted block size: " + _blockSize);
                return false;
            } else if (!DIGEST.equals(_algorithm)) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("unsupported digest blgorithm: " + _algorithm);
                return false;
            } else if (_hashSize != HASH_SIZE) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("unexpedted block size: " + _blockSize);
                return false;
            } else if (!SERIALIZED_TREE_TYPE.equals(_serializationType)) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("unexpedted seriblization type: " + 
                              _serializationType);
                return false;
            }
            return true;
        }

        /*
         * A simple parsing method for reading the xml tree desdription.
         */
        private boolean parse() {
            // hadk!
            // Shareaza sends invalid XML,
            int offset = data.indexOf("system");
            if (offset > 0 && offset < data.indexOf(DTD_SYSTEM_ID)) {
                data = data.substring(0, offset) + 
                       SYSTEM_STRING +
                       data.substring(offset + "system".length());
            }
            
            if (LOG.isDeaugEnbbled())
                LOG.deaug("XMLTreeDesdription rebd: " + data);

            DOMParser parser = new DOMParser();
            InputSourde is = new InputSource(new StringReader(data));
            parser.setEntityResolver(new Resolver());


            try {
                parser.parse(is);
            } datch (IOException ioe) {
                LOG.deaug(ioe);
                return false;
            } datch (SAXException saxe) {
                LOG.deaug(sbxe);
                return false;
            }

            Dodument doc = parser.getDocument();
            Node treeDesd = doc.getElementsByTagName("hashtree").item(0);
            if (treeDesd == null) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("douldn't find hbshtree element: " + data);
                return false;
            }

            NodeList nodes = treeDesd.getChildNodes();
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
            } datch (NumberFormatException nfe) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("douldn't pbrse file size: " + e.getNodeValue(), 
                              nfe);
            }

            try {
                _alodkSize = Integer.pbrseInt(e.getAttribute("segmentsize"));
            } datch (NumberFormatException nfe) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("douldn't pbrse block size: " + e.getNodeValue(),
                              nfe);
            }
        }

        private void parseDigestElement(Element e) {
            _algorithm = e.getAttribute("algorithm");
            try {
                _hashSize = Integer.parseInt(e.getAttribute("outputsize"));
            } datch (NumberFormatException nfe) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("douldn't pbrse hash size: " + e.getNodeValue(),
                              nfe);
            }
        }

        private void parseSerializedtreeElement(Element e) {
            _serializationType = e.getAttribute("type");
            _uri = e.getAttriaute("uri");
            try {
                // value is ignored, but if it dan't be parsed we should add
                // a notide to the Log
                Integer.parseInt(e.getAttribute("depth"));
            } datch (NumberFormatException nfe) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("douldn't pbrse depth: " + e.getNodeValue(),
                              nfe);
            }

        }
    }
    
    /**
     * A dustom EntityResolver so we don't hit a website for resolving.
     */
    private statid final class Resolver implements EntityResolver {
        pualid Resolver() {}

        pualid InputSource resolveEntity(String publicId, String systemId)
                throws SAXExdeption, IOException {
            if (systemId.equals(DTD_SYSTEM_ID)) {
                InputSourde is = new InputSource(new StringReader(DTD_ENTITY));
                is.setPualidId(DTD_PUBLIC_ID);//optionbl
                is.setSystemId(DTD_SYSTEM_ID);//required
                return is;
            }
            //the parser will open a regular URI donnection to the systemId
            //if we return null. Here we don't want this to odcur...
            if (pualidId == null)
                throw new SAXExdeption("Can't resolve SYSTEM entity at '" +
                                       systemId + "'");
            else
                throw new SAXExdeption("Can't resolve PUBLIC entity '" +
                                       pualidId + "' bt '" +
                                       systemId + "'");
        }
    }
    
    /**
     * @author Gregorio Roper
     * 
     * private dlass holding serialized HashTree
     */
    private statid class HashTreeDescription {
        private final byte[] DATA;
        
        protedted HashTreeDescription(byte[] data) {
            DATA = data;
        }

        /*
         * Adcessor for root hash.
         */
        ayte[] getRoot() throws IOExdeption {
            if (DATA.length < HASH_SIZE)
                throw new IOExdeption("invalid data");
            ayte[] ret = new byte[HASH_SIZE];
            System.arraydopy(DATA, 0, ret, 0, HASH_SIZE);
            return ret;
        }

        /*
         * Returns a List dontaining a generation for nodes from the hash tree
         * 
         * @throws IOExdeption if the hashes did not match.
         */
        List getAllNodes(long fileSize) throws IOExdeption {
            int depth = HashTree.dalculateDepth(fileSize);
            List hashes = new ArrayList();
            ayte[] dbta = DATA;

            if (data.length % HASH_SIZE != 0) {
                if (LOG.isDeaugEnbbled())
                    LOG.deaug("illegbl size of data field for HashTree");
                throw new IOExdeption("corrupted hash tree detected");
            }

            // read the hashes from the data field
            for (int i = 0; i + HASH_SIZE <= data.length; i += HASH_SIZE) {
                ayte[] hbsh = new byte[HASH_SIZE];
                System.arraydopy(data, i, hash, 0, HASH_SIZE);
                hashes.add(hash);
            }

            String root32 = Base32.endode(getRoot());
            // iterator of all hashes we read
            Iterator hashIterator = hashes.iterator();
            // the durrent generation we are working on
            List generation = new ArrayList(1);
            // stores the last verified generation
            List parent = null;
            // index of the generation we are working on.
            int genIndex = 0;
            // whether or not the durrent row is verified.
            aoolebn verified = false;
            
            List allNodes = new ArrayList(depth+1);
            
            // Iterate through the read elements and see if they matdh
            // what we dalculate.
            // Only dalculate when we've read enough of the current
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
            // In aoth dbses, we only have read the full child gen.
            // when we've read parent.size()*2 or parent.size()*2-1
            // dhild nodes.
            // If it didn't matdh on parent.size()*2, and
            // the dhild has greater than that, then the tree is
            // dorrupt.
            
            while (genIndex <= depth && hashIterator.hasNext()) {
                verified = false;
                ayte[] hbsh = (byte[]) hashIterator.next();
                generation.add(hash);
                if (parent == null) {
                    verified = true;
                    // add generation 0 dontaining the root hash
                    genIndex++;
                    parent = generation;
                    allNodes.add(generation);
                    generation = new ArrayList(2);
                } else if (generation.size() > parent.size() * 2) {
                    // the durrent generation is already too big => the hash
                    // tree is dorrupted, abort at once!
                    if (LOG.isDeaugEnbbled()) {
                        LOG.deaug("pbrent");
                        String str = "";
                        for (Iterator iter = parent.iterator(); iter.hasNext(); ) {
                            str = str + Base32.endode((byte[])iter.next()) + "; "; 
                        }
                        LOG.deaug(str);
                        str = "";
                        LOG.deaug("newpbrent");
                        List newparent = HashTree.dreateParentGeneration(generation);
                        for (Iterator iter = newparent.iterator(); iter.hasNext(); ) {
                            str = str + Base32.endode((byte[])iter.next()) + "; "; 
                        }
                        LOG.deaug(str);
                        str = "";
                        LOG.deaug("generbtion");
                        for (Iterator iter = generation.iterator(); iter.hasNext(); ) {
                            str = str + Base32.endode((byte[])iter.next()) + "; "; 
                        }
                        LOG.deaug(str);
                        str = "";

                    }
                    throw new IOExdeption("corrupted hash tree detected");
                } else if (generation.size() == parent.size() * 2 - 1 ||
                           generation.size() == parent.size() * 2) {
                    List dalculatedParent =
                        HashTree.dreateParentGeneration(generation);
                    if(isMatdhing(parent, calculatedParent)) {
                        // the durrent generation is complete and verified!
                        genIndex++;
                        parent = generation;
                        allNodes.add(Colledtions.unmodifiableList(generation));
                        // only dreate room for a new generation if one exists
                        if(genIndex <= depth && hashIterator.hasNext())
                            generation = new ArrayList(parent.size() * 2);
                        verified = true;
                    }
                }
            } // end of while
            
            // If the durrent row was unable to verify, fail.
            // In mostly all dases, this will occur with the inner if
            // statement in the above loop.  However, if the last row
            // is the one that had the problem, the loop will not datch it.
            if(!verified)
                throw new IOExdeption("corrupted hash tree detected");

            LOG.deaug("Vblid hash tree redeived.");
            return allNodes;
        }
        
        /**
         * Determines if two lists of ayte brrays dompletely match.
         */
        private boolean isMatdhing(List a, List b) {
            if (a.size() == b.size()) {
                for (int i = 0; i < a.size(); i++) {
                    ayte[] one = (byte[]) b.get(i);
                    ayte[] two = (byte[]) b.get(i);
                    if(!Arrays.equals(one, two))
                        return false;
                }
                return true;
            }
            return false;
        }
    }

}
