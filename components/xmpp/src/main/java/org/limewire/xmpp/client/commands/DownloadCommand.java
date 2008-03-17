package org.limewire.xmpp.client.commands;

import org.jivesoftware.smack.XMPPConnection;
import org.limewire.xmpp.client.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class DownloadCommand extends Command {
    private final HashMap<String, SearchResult> allResults;

    public DownloadCommand(XMPPConnection connection, HashMap<String, SearchResult> allResults) {
        super(connection);
        this.allResults = allResults;
    }

    public String getCommand() {
        return "get";
    }

    public void execute(String args) throws Exception {
        StringTokenizer st = new StringTokenizer(args);
        String resultBucket = st.nextToken();
        String resultID = st.nextToken();
        SearchResult bucket = allResults.get(resultBucket);
        if(bucket != null) {
            int id = Integer.parseInt(resultID);
            if(id < bucket.getResults().size() && id > -1) {
                String file = bucket.getResults().get(id);
                if(file != null) {
                                
                }
            }
        }
    }
}
