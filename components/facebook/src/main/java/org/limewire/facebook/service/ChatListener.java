package org.limewire.facebook.service;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.livemessage.LiveMessageHandler;
import org.limewire.facebook.service.livemessage.LiveMessageHandlerRegistry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ChatListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(org.limewire.facebook.service.ChatListener.class);
    
    private static final String HOME_PAGE = "http://www.facebook.com/home.php";
    private final FacebookFriendConnection connection;
    private final LiveMessageHandlerRegistry handlerRegistry;

    private final String uid;
    private final String channel;
    private int seq;
    
    private volatile boolean done;

    @AssistedInject
    ChatListener(@Assisted FacebookFriendConnection connection,
               LiveMessageHandlerRegistry handlerRegistry) {
        this.connection = connection;
        this.handlerRegistry = handlerRegistry;
        this.seq = -1;
        this.uid = connection.getUID();
        this.channel = connection.getChannel();
    }

    void setDone() {
        this.done = true;
    }

    @Override
    public void run() {
        done = false;
        try {
            seq = getSeq();
        } catch(IOException e1){
            e1.printStackTrace();
            done = true;
        } catch(JSONException e1){
            e1.printStackTrace();
            done = true;
        }

        int i = 0;
        int currentSeq = seq;
        while(!done){
            LOG.debugf("iteration: {0}", i++);
            try {
                //PostMessage("1190346972", "SEQ:"+seq);
                currentSeq = getSeq();
                if(seq > currentSeq)
                    seq = currentSeq;
                
                while(seq <= currentSeq){
                    //get the old message between oldseq and seq
                    String msgResponseBody = connection.httpGET(getMessageRequestingUrl(seq));
                    if(msgResponseBody != null) {
                        String prefix = "for (;;);";
                        if(msgResponseBody.startsWith(prefix))
                            msgResponseBody = msgResponseBody.substring(prefix.length());
                        JSONObject response = new JSONObject(msgResponseBody);
                        LOG.debugf("message: {0}", response);
                        if(response.getString("t").equals("msg")) {
                            dispatchMessage(response);
                        } else if(response.getString("t").equals("refresh")) {
                            getPOSTFormID();        
                        }
                        //seq++;
                    }
//                        
                    seq++;
                }
            } catch (IOException e) {
                e.printStackTrace();
                done = true;
            } catch (JSONException e) {
                e.printStackTrace();
                done = true;
            }
        }
    }

    private void dispatchMessage(JSONObject message) throws JSONException {
        if(message.has("ms")) {
            JSONArray ms = message.getJSONArray("ms");
            final JSONObject payload = ms.getJSONObject(0);
            if(payload.has("event_name")) {
                String messageType = payload.getString("event_name");
                LiveMessageHandler handler = handlerRegistry.getHandler(messageType);
                if(handler != null) {
                    if(payload.has("response")) {
                        JSONObject lwMessage = payload.getJSONObject("response");
                        handler.handle(messageType, lwMessage);
                    } else {
                        LOG.debugf("no 'response' in message {0}", message);
                    }
                    
                } else {
                    LOG.debugf("no handler for type: {0}", messageType);
                }
            }
        }            
    }

    private void getPOSTFormID() throws IOException {
        LOG.debug("refreshing post_form_id");
        String homePage = connection.httpGET(HOME_PAGE);    
        String post_form_id;
        String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
        int formIdBeginPos = homePage.indexOf(postFormIDPrefix)
                + postFormIDPrefix.length();
        if (formIdBeginPos < postFormIDPrefix.length()){
            throw new IOException("can't find post form id");
        }
        else {
            post_form_id = homePage.substring(formIdBeginPos,
                    formIdBeginPos + 32);
        }  
        connection.setPostFormID(post_form_id);
    }

    private int getSeq() throws IOException, JSONException {
        int tempSeq = -1;
        while (tempSeq == -1) {
            //for (;;);{"t":"refresh", "seq":0}
            String seqResponseBody;

            seqResponseBody = connection.httpGET(getMessageRequestingUrl(-1));
//                String prefix = "for (;;);";
//                if(seqResponseBody != null && seqResponseBody.startsWith(prefix)) {
//                    seqResponseBody = seqResponseBody.substring(prefix.length());
//                    JSONObject body = new JSONObject(seqResponseBody);
//                    if(body != null && body.has("t") && body.getString("t").equals("refresh")) {
//                        getPOSTFormID(); 
//                        return getSeq();
//                    }
//                }
            
            tempSeq = parseSeq(seqResponseBody);

            if(tempSeq >= 0){
                return tempSeq;
            }

            try {
                LOG.debug("retrying to retrieve the seq code after 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return tempSeq;
    }
    
    private String getMessageRequestingUrl(long seq) {
        return "http://0.channel" + channel + ".facebook.com/x/0/false/p_" + uid + "=" + seq;
    }
    
    private int parseSeq(String msgResponseBody) throws JSONException, IOException {
        if(msgResponseBody == null)
            return -1;
        String prefix = "for (;;);";
        if(msgResponseBody.startsWith(prefix))
            msgResponseBody = msgResponseBody.substring(prefix.length());
        
        //JSONObject body =(JSONObject) JSONValue.parse(msgResponseBody);
        JSONObject body = new JSONObject(msgResponseBody);
        if(body != null && body.has("seq"))
            return body.getInt("seq");
        else if(body.has("t") && body.getString("t").equals("refresh")) {
            getPOSTFormID();    
            return getSeq();
        }
        else 
            return -1;
    }
}
