package org.limewire.xmpp.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultAttribute;

import java.io.File;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class SearchListener implements PacketListener {
    private final List<File> sharedResources;

    public SearchListener(List<File> sharedResources) {
        this.sharedResources = sharedResources;
    }

    public void processPacket(Packet packet) {
        IQ iq = (IQ)packet;
        if(iq.getType().equals(IQ.Type.GET)) {
            handleGet(iq);
        } else if(iq.getType().equals(IQ.Type.RESULT)) {
            handleResult(iq);
        } else if(iq.getType().equals(IQ.Type.SET)) {
            handleSet(iq);
        } else if(iq.getType().equals(IQ.Type.ERROR)) {
            handleError(iq);
        } else {
            //sendError(packet);
        }
    }

    private void handleError(IQ packet) {
        System.out.println("ERROR:\n" + packet.toXML());
    }

    private void handleSet(IQ packet) {
        // sendError(packet);
    }

    private void handleGet(IQ packet) {
        IQ queryResult = new SearchResult();
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(IQ.nextID());

    }

    private Element searchLocalResources(Element childElement) {
        DefaultElement queryReply = new DefaultElement("query-reply", new Namespace("", "iq:jabber:lw-query-reply"));
        Element keywords = childElement.element("keywords");
        String[] keywordsList = parseKeywords(keywords.getText());
        if(keywordsList != null) {
            for(String keyword : keywordsList) {
                for(File shared : sharedResources) {
                    if(shared.exists()) {
                        if(shared.isFile()) {
                            searchFile(keyword, shared, queryReply);
                        } else if(shared.isDirectory()) {
                            searchDir(keyword, shared, queryReply);
                        }
                    }
                }
            }
        }
        return queryReply;
    }

    private void searchDir(String keyword, File shared, DefaultElement queryReply) {
        File [] files = shared.listFiles();
        if(files != null) {
            for(File f : files) {
                if(f.isFile()) {
                    searchFile(keyword, f, queryReply);
                } else if(f.isDirectory()){
                    // TODO eliminate infinite recursion from symbolic links
                    searchDir(keyword, f, queryReply);
                }
            }
        }
    }

    private void searchFile(String keyword, File shared, DefaultElement queryReply) {
        keyword = keyword.toLowerCase();
        String fileName = shared.getName().toLowerCase();
        if(fileName.contains(keyword)) {
            DefaultElement reply = new DefaultElement("reply", new Namespace("", "iq:jabber:lw-query-reply"));
            reply.add(new DefaultAttribute("file", shared.getPath()));
            queryReply.add(reply);
        }

    }

    private String[] parseKeywords(String keywords) {
        StringTokenizer st = new StringTokenizer(keywords);
        ArrayList<String> wordList = new ArrayList<String>();
        while(st.hasMoreElements()) {
            wordList.add(st.nextToken());
        }
        return wordList.toArray(new String[]{});
    }

    public static class SearchFilter implements PacketFilter {
        public boolean accept(Packet packet) {
            return packet.getExtension("search", "jabber:iq:lw-search") != null;
        }
    }
}
