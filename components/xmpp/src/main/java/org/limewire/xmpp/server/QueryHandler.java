package org.limewire.xmpp.server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultAttribute;
import org.dom4j.tree.DefaultElement;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.RoutingTable;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.ClientSession;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class QueryHandler extends IQHandler {
    private final List<JID> ultrapeers;
    private final List<JID> leaves;
    private final RoutingTable routingTable;
    private final boolean isUltrapeer;
    // TODO map data structure
    private final List<File> sharedResources;
    private final HashMap<String, IQ> queryRoutes;

    /**
     * Create a basic module with the given name.
     *
     * @param moduleName The name for the module or null to use the default
     */
    public QueryHandler(String moduleName, List<JID> ultrapeers, List<JID> leaves, RoutingTable routingTable, boolean isUltrapeer, List<File> sharedResources) {
        super(moduleName);
        this.ultrapeers = ultrapeers;
        this.leaves = leaves;
        this.routingTable = routingTable;
        this.isUltrapeer = isUltrapeer;
        this.sharedResources = sharedResources;
        this.queryRoutes = new HashMap<String, IQ>();
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        if(packet.getType().equals(IQ.Type.get)) {
            return handleGet(packet);
        } else if(packet.getType().equals(IQ.Type.result)) {
            return handleResult(packet);
        } else if(packet.getType().equals(IQ.Type.set)) {
            return handleSet(packet);
        } else if(packet.getType().equals(IQ.Type.error)) {
            return handleError(packet);
        } else {
            return sendError(packet);
        }
    }

    private IQ sendError(IQ packet) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private IQ handleError(IQ packet) {
        System.out.println("ERROR:\n" + packet.toXML());
        return null;
    }

    private IQ handleSet(IQ packet) {
        return sendError(packet);
    }

    private IQ handleResult(IQ packet) {
        IQ original = queryRoutes.get(packet.getID());
        if(original != null) {
            IQ queryResult = IQ.createResultIQ(original);
            queryResult.setChildElement(packet.getChildElement());
            ClientSession client = routingTable.getClientRoute(original.getFrom());
            client.process(queryResult);
        }
        return null;
    }

    private IQ handleGet(IQ packet) {
        if(isUltrapeer) {
            propagateSearchToUltrapeers(packet);
            searchLeaves(packet);
        }
        Element queryReply = searchLocalResources(packet.getChildElement());
        IQ queryResult = IQ.createResultIQ(packet);
        queryResult.setChildElement(queryReply);
        return queryResult;
    }

    private void searchLeaves(IQ incoming) {
        // TODO add bloom filter
        Element query = incoming.getChildElement();
        query = query.createCopy();
        Element ttl = query.element("ttl");
        Integer ttlValue = Integer.parseInt(ttl.getText());
        ttlValue--;
        if(ttlValue > 0) {
//            Element source = query.element(new QName("source", new Namespace("iq:jabber:lw-query", "")));
//            if(source == null) {
//                source = new DefaultElement("source");
//                source.addAttribute("jid", incoming.getFrom().toString());
//                source.addAttribute("iq-id", incoming.getID());
//                query.add(source);
//            }
            ttl.setText(ttlValue.toString());
            Element hops = query.element("hops");
            Integer hopsValue = Integer.parseInt(hops.getText());
            hopsValue++;
            hops.setText(hopsValue.toString());
            synchronized (leaves) {
                for(JID leaf : leaves) {
                    if(!leaf.equals(incoming.getFrom())) {
                        send(query, leaf, incoming);
                    }
                }
            }
        } 
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

    private void propagateSearchToUltrapeers(IQ incoming) {
        Element query = incoming.getChildElement();
        query = query.createCopy();
        Element ttl = query.element("ttl");
        Integer ttlValue = Integer.parseInt(ttl.getText());
        ttlValue--;
        if(ttlValue > 0) {
//            Element source = query.element(new QName("source", new Namespace("iq:jabber:lw-query", "")));
//            if(source == null) {
//                source = new DefaultElement("source");
//                source.addAttribute("jid", incoming.getFrom().toString());
//                source.addAttribute("iq-id", incoming.getID());
//                query.add(source);
//            }
            ttl.setText(ttlValue.toString());
            Element hops = query.element("hops");
            Integer hopsValue = Integer.parseInt(hops.getText());
            hopsValue++;
            hops.setText(hopsValue.toString());
            synchronized (ultrapeers) {
                for(JID ultrapeer : ultrapeers) {
                    if(!ultrapeer.equals(incoming.getFrom())) {
                        send(query, ultrapeer, incoming);
                    }
                }
            }
        }        
    }

    private void send(Element query, JID to, IQ incoming) {
        // TODO in a separate Thread
        ClientSession client = routingTable.getClientRoute(to);
        if(client != null) {
            IQ iq = new IQ();
            iq.setTo(to);
            iq.setFrom(incoming.getTo());
            iq.setChildElement(query);
            client.process(iq);
            queryRoutes.put(iq.getID(), incoming);
        } else {
            // TODO log error
            // TODO remove from list
        }
    }

    public IQHandlerInfo getInfo() {
        return new IQHandlerInfo("query", "jabber:iq:lw-query");
    }
}
