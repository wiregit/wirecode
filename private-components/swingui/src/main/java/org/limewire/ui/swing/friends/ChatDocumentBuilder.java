package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.ArrayList;

import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.FileMetaData;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
class ChatDocumentBuilder {
    static final String LIBRARY_LINK = "#library";

    private static final String LINE_BREAK = "<br/>";

    static String TOP = 
        "<html>" +
            "<head>" +
                "<style>" +
                    "body { " +
                        "background-color: #ffffff;" +
                        "margin: 0 4px;}" +
                    ".me { " +
                        "color: #0D3065;" +
                        "font-size: 105%;" +
                        "font-weight: bold;}" +
                    ".them { " +
                        "color: #771324;" + 
                        "font-size: 105%;" +
                        "font-weight: bold;}" +
                    ".typing { " +
                        "font-size: 90%;" +
                        "color: #646464;}" + 
                    "form { text-align: center;}" +
                "</style>" +
            "</head>" +
            "<body>";
    
    static String BOTTOM = 
        "</body>" +
        "</html>";
    
    public static String buildChatText(ArrayList<Message> messages, ChatState currentChatState, 
            String conversationName, boolean friendSignedOff) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        Type lastMessageType = null;
        long lastMessageTimeFromMe = 0;
        for(Message message : messages) {

            Type type = message.getType();
            
            if (lastMessageType == null) {
                //The first message of a conversation
                appendDiv(builder, message);
            } else if (lastMessageType != type || sixtySecondRule(lastMessageTimeFromMe, message)) {
                builder.append(LINE_BREAK);
                appendDiv(builder, message);
            }
            
            lastMessageType = type;
            
            builder.append(processContent(message));
            
            builder.append(LINE_BREAK);
            
            if (type == Type.Sent) {
                lastMessageTimeFromMe = message.getMessageTimeMillis();
            }
        }

        appendIsTypingMessage(builder, conversationName, currentChatState, friendSignedOff);
        
        builder.append(BOTTOM);
        return builder.toString();
    }

    private static boolean sixtySecondRule(long lastMessageTimeFromMe, Message message) {
        return message.getType() == Type.Sent && lastMessageTimeFromMe + 60000 < message.getMessageTimeMillis();
    }

    private static StringBuilder appendDiv(StringBuilder builder, Message message) {
        Type type = message.getType();
        String cssClass = type == Type.Sent ? "me" : "them";
        String content = message.getSenderName();
        return builder.append("<div class=\"")
        .append(cssClass)
        .append("\">")
        .append(content)
        .append(":")
        .append("</div>");
    }
    
    private static void appendIsTypingMessage(StringBuilder builder, String senderName, ChatState chatState, boolean friendSignedOff) {
        String stateMessage = null;
        if (friendSignedOff) {
            stateMessage = tr("{0} has signed off", senderName);
        } else if (chatState == ChatState.composing) {
            stateMessage = tr("{0} is typing a message...", senderName);
        } else if (chatState == ChatState.paused) {
            stateMessage = tr("{0} has entered text", senderName);
        } else {
            return;
        }
        
        String cssClass = "typing";
        
        builder.append("<div class=\"")
               .append(cssClass)
               .append("\">")
               .append(stateMessage)
               .append("</div>")
               .append("<br/>");
    }

    private static String processContent(Message message) {
        String messageText = message.getMessageText();
        if (message.hasFileOffer()) {
            StringBuilder bldr = new StringBuilder();
            FileMetaData offeredFile = message.getFileOffer();
            boolean isIncoming = (message.getType() == Message.Type.Received);
            String fileOfferReceived = tr("{0} wants to share a file with you", message.getFriendID());
            String fileOfferSent = tr("Sharing file with {0}", message.getFriendID());

            bldr.append(isIncoming ? fileOfferReceived : fileOfferSent)
                .append("<br/>")
                .append("<form action=\"\"><input type=\"hidden\" name=\"fileid\" value=\"")
                .append(offeredFile.toXML())
                .append("\"/><input type=\"submit\" value=\"")
                .append(offeredFile.getName())
                .append(isIncoming ? "\"/>" : ":disabled\"/>")
                .append("</form><br/>");

            if (isIncoming) {
                // {0} and {1} are HTML tags that make Library a link
                bldr.append(tr("Download it now, or get it from their " +
                               "{0}Library{1} later.","<a href=\"" + LIBRARY_LINK + "\">", "</a>"));
            }
            return bldr.toString();
        }
        messageText = messageText.replace("<", "&lt;").replace(">", "&gt;");
        
        return URLWrapper.wrap(messageText);
    }
}
