package com.limegroup.gnutella.privategroups;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.VCard;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * ValueStorage Provider
 *
 * @author Gaston Dombiak
 * @author Derek DeMoro
 */
public class ValueStorageProvider implements IQProvider {

    private static final String PREFERRED_ENCODING = "UTF-8";

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        final StringBuilder sb = new StringBuilder();
        try {
            int event = parser.getEventType();
            // get the content
            while (true) {
                switch (event) {
                    case XmlPullParser.TEXT:
                        // We must re-escape the xml so that the DOM won't throw an exception
                        sb.append(StringUtils.escapeForXML(parser.getText()));
                        break;
                    case XmlPullParser.START_TAG:
                        sb.append('<').append(parser.getName()).append('>');
                        for(int i = 0; i < parser.getAttributeCount(); i++){
                            sb.append("<").append(parser.getAttributeName(i)).append(">").append(parser.getAttributeValue(i));
                            sb.append("</").append(parser.getAttributeName(i)).append(">");
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        sb.append("</").append(parser.getName()).append('>');
                        break;
                    default:
                }

                if (event == XmlPullParser.END_TAG && "stor".equals(parser.getName())) break;

                event = parser.next();
            }
        }
        catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        String xmlText = sb.toString();
        return createValueStorageFromXML(xmlText);
    }

    /**
     * Builds a users ValueStorage from xml file.
     *
     * @param xml the xml representing a users ValueStorage.
     * @return the ValueStorage.
     * @throws
     */
    public static ValueStorage createValueStorageFromXML(String xml) throws Exception {
        ValueStorage stor = new ValueStorage();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(
                new ByteArrayInputStream(xml.getBytes(PREFERRED_ENCODING)));

        new ValueStorageReader(stor, document).initializeFields();
        return stor;
    }

    private static class ValueStorageReader {

        private final ValueStorage storValue;
        private final Document document;

        ValueStorageReader(ValueStorage value, Document document) {
            this.storValue = value;
            this.document = document;
        }

        public void initializeFields() {
            
            storValue.setIPAddress(getTagContents("ipAddress"));
            storValue.setPort(getTagContents("port"));
            storValue.setPublicKey(getTagContents("publicKey"));
        }

  

        private String getTagContents(String tag) {
            NodeList nodes = document.getElementsByTagName(tag);
            if (nodes != null && nodes.getLength() == 1) {
                return getTextContent(nodes.item(0));
            }
            return null;
        }

        private String getTextContent(Node node) {
            StringBuilder result = new StringBuilder();
            appendText(result, node);
            return result.toString();
        }

        private void appendText(StringBuilder result, Node node) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node nd = childNodes.item(i);
                String nodeValue = nd.getNodeValue();
                if (nodeValue != null) {
                    result.append(nodeValue);
                }
                appendText(result, nd);
            }
        }
    }
}
