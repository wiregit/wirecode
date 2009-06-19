package org.limewire.facebook.service;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.livemessage.LiveMessageHandler;
import org.limewire.facebook.service.livemessage.LiveMessageHandlerRegistry;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.MessageReader;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This listens for new chat messages and live messages, and dispatches them to the
 * appropriate handlers.  It does this via http polling, where the requests are long running (COMET style).
 */
class ChatListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(org.limewire.facebook.service.ChatListener.class);
    
    private static final String HOME_PAGE = "http://www.facebook.com/home.php";
    private final FacebookFriendConnection connection;
    private final LiveMessageHandlerRegistry handlerRegistry;

    private final ChatManager chatManager;
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
        this.chatManager = connection.getChatManager();
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
            LOG.debug("error getting initial sequence number", e1);
        } catch(JSONException e1){
            LOG.debug("error parsing initial sequence number", e1);
        }

        int currentSeq;
        while(!done) {
            try {
                currentSeq = getSeq();
                if(seq > currentSeq) {
                    seq = currentSeq;
                }
                
                while(seq <= currentSeq && !done) {
                    //get the old message between oldseq and seq
                    String msgResponseBody = connection.httpGET(getMessageRequestingUrl(seq));
                    if(msgResponseBody != null) {
                        String prefix = "for (;;);";
                        if(msgResponseBody.startsWith(prefix)) {
                            msgResponseBody = msgResponseBody.substring(prefix.length());
                        }
                        JSONObject response = new JSONObject(msgResponseBody);
                        LOG.debugf("message: {0}", response);
                        if(response.getString("t").equals("msg")) {
                            dispatchMessage(response);
                        } else if(response.getString("t").equals("refresh")) {
                            getPOSTFormID();        
                        }
                        //seq++;
                    }                        
                    seq++;
                }
            } catch (IOException e) {
                LOG.debug("error getting chat message", e);
            } catch (JSONException e) {
                LOG.debug("error parsing chat message", e);
            }
        }
        LOG.debug("chat listener is done");
    }

    private void dispatchMessage(JSONObject message) throws JSONException {
        if(message.has("ms")) {
            JSONArray ms = message.getJSONArray("ms");
            final JSONObject payload = ms.getJSONObject(0);
            String msgType = payload.getString("type");

            if(payload.has("event_name")) {
                processLiveMessage(payload);
            } else if ("msg".equals(msgType) || "typ".equals(msgType)) {
                processChatMessage(payload, msgType);
            }
        }            
    }

    private void processLiveMessage(JSONObject payload) throws JSONException {
        if (!payload.has("response")) {
            LOG.debugf("no 'response' in message payload: {0}", payload);
            return;
        }
        
        JSONObject lwMessage = payload.getJSONObject("response");
        
        String presenceId = lwMessage.getString("from");
        connection.addPresence(presenceId);
            
        String to = lwMessage.getString("to");
        if (!to.equals(connection.getPresenceId())) {
            LOG.debugf("message not for us: {0}", payload);
            return;
        }
        
        String messageType = payload.getString("event_name");
        LiveMessageHandler handler = handlerRegistry.getHandler(messageType);
        if (handler != null) {
            handler.handle(messageType, lwMessage);
        } else {
            LOG.debugf("no handler for type: {0}", messageType);
        }
    }

    private void processChatMessage(JSONObject payload, String msgType) throws JSONException {
        String parsedSenderId = payload.getString("from");

        // look up the MessageReader based on the sender (friend) id
        if (parsedSenderId != null) {
            MessageReader handler = chatManager.getMessageReader(parsedSenderId);

            if (handler != null) {
                if (msgType.equals("msg")) {
                    JSONObject messageJson = payload.getJSONObject("msg");
                    String msg = messageJson.getString("text");
                    handler.readMessage(msg);
                } else {
                    ChatState state = payload.getInt("st") == 1 ? ChatState.composing : ChatState.active;
                    handler.newChatState(state);
                }
            } else {
                // this can happen, when we just signed off and removed all friend presences
                // but the friend is currently typing to us an the connection is not
                // closed yet, it's ok in this case
                LOG.debugf("no handler for sender: {0}", parsedSenderId);
            }
        } else {
            LOG.debugf("no 'from' in message payload: {0}", payload);
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
            String seqResponseBody = connection.httpGET(getMessageRequestingUrl(-1));
            if (seqResponseBody == null) {
                LOG.debug("null response for seq");
                continue;
            }
            tempSeq = parseSeq(seqResponseBody);
            if(tempSeq >= 0){
                return tempSeq;
            }
            try {
                LOG.debug("retrying to retrieve the seq code after 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
        return tempSeq;
    }
    
    private String getMessageRequestingUrl(long seq) {
        return "http://0.channel" + channel + ".facebook.com/x/0/false/p_" + uid + "=" + seq;
    }
    
    /**
     * @return -1 if sequence number could not be parsed
     */
    private int parseSeq(String msgResponseBody) throws JSONException, IOException {
        LOG.debugf("parsing seq from: {0}", msgResponseBody);
        //for (;;);{"t":"refresh", "seq":0}
        String prefix = "for (;;);";
        if(msgResponseBody.startsWith(prefix)) {
            msgResponseBody = msgResponseBody.substring(prefix.length());
        }
        
        JSONObject body = new JSONObject(msgResponseBody);
        if(body.has("seq")) {
            return body.getInt("seq");
        } else if(body.has("t") && body.getString("t").equals("refresh")) {
            LOG.debug("refreshing post form id");
            getPOSTFormID();    
            return -1;
        }
        else {
            return -1;
        }
    }
}
