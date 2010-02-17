package com.limegroup.gnutella.simpp;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;
import org.limewire.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.security.Certificate;
import com.limegroup.gnutella.security.CertificateParserImpl;
import com.limegroup.gnutella.security.CertifiedMessageVerifier.CertifiedMessage;
import com.limegroup.gnutella.xml.LimeXMLUtils;

public class SimppParser {
    
    private static final Log LOG = LogFactory.getLog(SimppParser.class);

    public static final String VERSION = "version";
    
    public static final String PROPS = "props";
    
    public static final String KEY_VERSION = "keyversion";
    
    public static final String NEW_VERSION = "newversion";
    
    public static final String SIGNATURE = "signature";
    
    public static final String CERTIFICATE = "certificate";

    private int _version;
    private String _propsData;

    private int keyVersion = -1;

    private int newVersion = -1;

    private byte[] signature;
    
    private byte[] signedPayload;

    private Certificate certificate;    

    //Format of dataBytes:
    //<xml for version related info with one tag containing all the props data>
    //TODO1: Change the way this is parsed as per the format described above. 
    public SimppParser(byte[] dataBytes) throws IOException {
        parseInfo(new String(dataBytes, "UTF-8"));
    }
    
    public int getVersion() {
        return _version;
    }
    
    public int getNewVersion() {
        return newVersion;
    }
    
    public String getPropsData() {
        return _propsData;
    }

    int getKeyVersion() {
        return keyVersion;
    }

    byte[] getSignature(){
        return signature;
    }
    
    Certificate getCertificate(){
        return certificate;
    }
    ///////////////////////////private helpers////////////////////////

    private void parseInfo(String xmlStr) throws IOException {
        if(xmlStr == null || xmlStr.equals(""))
            throw new IOException("null xml for version info");
        Document d = XMLUtils.getDocument(xmlStr, LOG);
        Element docElement = d.getDocumentElement();
        NodeList children = docElement.getChildNodes();
        int len = children.getLength();
        for(int i= 0; i< len; i++) {
            Node node = children.item(i);
            String nodeName = node.getNodeName().toLowerCase(Locale.US).trim();
            String value = LimeXMLUtils.getText(node.getChildNodes());
            if(nodeName.equals(VERSION)) {
                _version = parseInteger(value, -1);
            } else if (nodeName.equals(KEY_VERSION)) {
                keyVersion = parseInteger(value, -1);
            } else if (nodeName.equals(NEW_VERSION)) {
                newVersion = parseInteger(value, -1);
            } else if (nodeName.equals(SIGNATURE)) {
                signature = Base32.decode(value);
                signedPayload = StringUtils.toUTF8Bytes(stripSignature(xmlStr));
            } else if (nodeName.equals(CERTIFICATE)) {
                certificate = new CertificateParserImpl().parseCertificate(value);
            } else if(nodeName.equals(PROPS)) {
                _propsData = value;
            }
        }//end of for -- done all child nodes
        if (keyVersion <= -1 || newVersion <= -1 || signature == null) {
            throw new IOException("missing or invalid data: " + StringUtils.toString(this, keyVersion, newVersion, signature));
        }
    }
    
    public CertifiedMessage getCertifiedMessage() {
        return new CertifiedMessage() {
            @Override
            public byte[] getSignature() {
                return signature;
            }
            @Override
            public byte[] getSignedPayload() {
                return signedPayload;
            }
            @Override
            public int getKeyVersion() {
                return keyVersion;
            }
            @Override
            public Certificate getCertificate() {
                return certificate;
            }
            
            @Override
            public String toString() {
                return StringUtils.toString(SimppParser.this, keyVersion, signature, signedPayload, certificate);
            }
        };
    }
    
    static int parseInteger(String integer, int defaultValue) {
        try {
            return Integer.parseInt(integer);
        } catch(NumberFormatException nfx) {
            LOG.error("Unable to parse number: " + integer, nfx);
            return defaultValue;
        }
    }
    
    static String stripSignature(String input) {
        return input.replaceAll("<signature>[^<]*</signature>", "");
    }
    
}
