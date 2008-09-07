package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.ArrayList;

import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.ChatState;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
class ChatDocumentBuilder {
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
                "</style>" +
            "</head>" +
            "<body>";
    
    static String BOTTOM = 
        "</body>" +
        "</html>";
    
    public static String buildChatText(ArrayList<Message> messages, ChatState currentChatState) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        Type lastMessageType = null;
        String otherConversantName = null;
        long lastMessageTimeFromMe = 0;
        for(Message message : messages) {

            Type type = message.getType();
            if (type == Type.Received) {
                otherConversantName = message.getSenderName();
            }
            
            if (lastMessageType == null) {
                //The first message of a conversation
                appendDiv(builder, message);
            } else if (lastMessageType != type || (type == Type.Sent && lastMessageTimeFromMe + 60000 < message.getMessageTimeMillis())) {
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
        
        appendIsTypingMessage(builder, otherConversantName, currentChatState);
        
        builder.append(BOTTOM);
        return builder.toString();
    }

    private static StringBuilder appendDiv(StringBuilder builder, Message message) {
        Type type = message.getType();
        String cssClass = type == Type.Sent ? "me" : type == Type.Received ? "them" : "typing";
        String content = message.getSenderName();
        return builder.append("<div class=\"")
        .append(cssClass)
        .append("\">")
        .append(content)
        .append(":")
        .append("</div>");
    }
    
    private static void appendIsTypingMessage(StringBuilder builder, String senderName, ChatState chatState) {
        String stateMessage = null;
        switch(chatState) {
        case composing:
            stateMessage = " is typing a message...";
            break;
        case paused:
            stateMessage = " has entered text";
            break;
        default:
            return;
        }
        
        String cssClass = "typing";
        String content = senderName + tr(stateMessage);
        
        builder.append("<div class=\"")
        .append(cssClass)
        .append("\">")
        .append(content)
        .append("</div>")
        .append("<br/>");
    }

    private static String processContent(Message message) {
        String messageText = message.getMessageText();
        messageText = messageText.replace("<", "&lt;").replace(">", "&gt;");
        
        return URLWrapper.wrap(messageText);
    }
}
