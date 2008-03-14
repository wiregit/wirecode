package org.limewire.xmpp.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.tree.DefaultElement;
import org.dom4j.tree.DefaultText;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

public class SearchListener implements PacketListener {
    private final XMPPConnection connection;
    private final List<File> sharedResources;

    public SearchListener(XMPPConnection connection, File... sharedResources) {
        this.connection = connection;
        this.sharedResources = Arrays.asList(sharedResources);
    }

    public void processPacket(Packet packet) {
        Search iq = (Search)packet;
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

    private void handleResult(IQ iq) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void handleError(IQ packet) {
        System.out.println("ERROR:\n" + packet.toXML());
    }

    private void handleSet(IQ packet) {
        // sendError(packet);
    }

    private void handleGet(Search packet) {
        IQ queryResult = new SearchResult(searchLocalResources(packet.getKeywords()));
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(packet.getPacketID());
        queryResult.setType(IQ.Type.RESULT);
        connection.sendPacket(queryResult);
    }

    private Element [] searchLocalResources(List<String> keywordsList) {
        ArrayList<Element> queryReplies = new ArrayList<Element>();
        if(keywordsList != null) {
            for(String keyword : keywordsList) {
                for(File shared : sharedResources) {
                    if(shared.exists()) {
                        if(shared.isFile()) {
                            searchFile(keyword, shared, queryReplies);
                        } else if(shared.isDirectory()) {
                            searchDir(keyword, shared, queryReplies);
                        }
                    }
                }
            }
        }
        return queryReplies.toArray(new Element[]{});
    }

    private void searchDir(String keyword, File shared, ArrayList<Element> queryReplies) {
        File [] files = shared.listFiles();
        if(files != null) {
            for(File f : files) {
                if(f.isFile()) {
                    searchFile(keyword, f, queryReplies);
                } else if(f.isDirectory()){
                    // TODO eliminate infinite recursion from symbolic links
                    searchDir(keyword, f, queryReplies);
                }
            }
        }
    }

    private void searchFile(String keyword, File shared, ArrayList<Element> queryReplies) {
        keyword = keyword.toLowerCase();
        String fileName = shared.getName().toLowerCase();
        if(fileName.contains(keyword)) {
            DefaultElement reply = new DefaultElement("search-result", new Namespace("", "jabber:iq:lw-search-results"));
            reply.add(new DefaultText(shared.getPath()));
            queryReplies.add(reply);
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
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof Search;
                //return packet.getExtension("search", "jabber:iq:lw-search") != null;
            }
        };
    }
}
