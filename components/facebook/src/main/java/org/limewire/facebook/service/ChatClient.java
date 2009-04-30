package org.limewire.facebook.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.ThreadExecutor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class ChatClient {
    private static final String HOME_PAGE = "http://www.facebook.com/home.php";
    private final FacebookFriendConnection connection;
    private final PresenceListenerFactory presenceListenerFactory;
    private final ScheduledListeningExecutorService executorService;

    @AssistedInject
    ChatClient(@Assisted FacebookFriendConnection connection,
               PresenceListenerFactory presenceListenerFactory,
               @Named("backgroundExecutor") ScheduledListeningExecutorService executorService) {
        this.connection = connection;
        this.presenceListenerFactory = presenceListenerFactory;
        this.executorService = executorService;//ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName())); //executorService;
    }

    public void start() throws IOException {
        String homePage = connection.httpGET(HOME_PAGE);

        if(homePage == null){
            throw new IOException("no response");
        }
        String uid = connection.getUID();
        if(uid == null){
            throw new IOException("no uid");
        }
        
        String channel;
        String channelPrefix = " \"channel";
        int channelBeginPos = homePage.indexOf(channelPrefix)
                + channelPrefix.length();
        if (channelBeginPos < channelPrefix.length()){
            throw new IOException("can't find channel");
        }
        else {
            channel = homePage.substring(channelBeginPos,
                    channelBeginPos + 2);
        }
        
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
        
        ThreadExecutor.startThread(new ChatListener(uid, channel), "chat-listener-thread");
        executorService.scheduleAtFixedRate(presenceListenerFactory.createPresenceListener(post_form_id), 0, 90, TimeUnit.SECONDS);
    }
    
    private class ChatListener implements Runnable {
        private final String uid;
        private final String channel;
        private int seq;

        public ChatListener(String uid, String channel) {
            this.uid = uid;
            this.channel = channel;
            this.seq = -1;
        }

        @Override
        public void run() {
            try {
                seq = getSeq();
            } catch(IOException e1){
                e1.printStackTrace();
            } catch(JSONException e1){
                e1.printStackTrace();
            }

            int currentSeq = seq;
            while(true){
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
                            if(response.getString("t").equals("msg")) {
        //                        try {
        //                            ResponseParser.messageRequestResultParser(msgResponseBody);
        //                        } catch (JSONException e) {
        //                            e.printStackTrace();
        //                        }
                                //seq++;
                            }
                            //seq++;
                        }
//                        
                        seq++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private int getSeq() throws IOException, JSONException {
            int tempSeq = -1;
            while (tempSeq == -1) {
                //for (;;);{"t":"refresh", "seq":0}
                String seqResponseBody;

                seqResponseBody = connection.httpGET(getMessageRequestingUrl(-1));
                tempSeq = parseSeq(seqResponseBody);

                if(tempSeq >= 0){
                    return tempSeq;
                }

                try {
                    System.out.println("retrying to retrieve the seq code after 1 second...");
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
        
        private int parseSeq(String msgResponseBody) throws JSONException{
            if(msgResponseBody == null)
                return -1;
            String prefix = "for (;;);";
            if(msgResponseBody.startsWith(prefix))
                msgResponseBody = msgResponseBody.substring(prefix.length());
            
            //JSONObject body =(JSONObject) JSONValue.parse(msgResponseBody);
            JSONObject body = new JSONObject(msgResponseBody);
            if(body != null && body.has("seq"))
                return body.getInt("seq");
            else
                return -1;
        }
    }
}
