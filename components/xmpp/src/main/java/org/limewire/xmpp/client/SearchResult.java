package org.limewire.xmpp.client;

import org.dom4j.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class SearchResult extends IQ {

    private static HashMap<String, SearchResult> searchResults = new HashMap<String, SearchResult>();

    private Element [] childElements;
    private List<String> results;

    public SearchResult(Element[] childElements) {
        this.childElements = childElements;
    }

    public SearchResult(XmlPullParser parser) {
       results = new ArrayList<String>();
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("search-result")) {
                        results.add(parser.nextText());
                        //System.out.println(results.size() - 1 + ": " + results.get(results.size() - 1));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("search-results")) {
                        return;
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XmlPullParserException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public String getChildElementXML() {
        return "<search-results xmlns=\"jabber:iq:lw-search-results\">" + getChildrenXML(childElements) + "</search-results>";
    }

    private String getChildrenXML(Element[] childElements) {
        StringBuilder children = new StringBuilder();
        if(childElements != null) {
            for(Element child : childElements) {
                children.append(child.asXML());
            }
        }
        return children.toString();
    }

    public List<String> getResults() {
        return results;
    }

    public static IQProvider getIQProvider() {
        return new SearchResultIQProvider();
    }

    private static class SearchResultIQProvider implements IQProvider {
        public IQ parseIQ(XmlPullParser parser) throws Exception {
            SearchResult result = new SearchResult(parser);
            // TODO better concurrency
            synchronized (searchResults) {
                System.out.println("search: " + result.getPacketID());
                searchResults.put(result.getPacketID(), result);
                print(result);
            }
            return result;
        }

        private void print(SearchResult result) {
            List<String> results = result.getResults();
            for(int i = 0; i < results.size(); i++) {
                System.out.println(i + ": " + results.get(i));
            }
        }
    }
}
