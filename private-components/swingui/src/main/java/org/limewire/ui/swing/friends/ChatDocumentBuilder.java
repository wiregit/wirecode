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
        boolean otherConversantIsTyping = currentChatState == ChatState.composing;
        
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        Type lastMessageType = null;
        String otherConversantName = null;
        
        for(Message message : messages) {

            if (message.getType() == Type.Received) {
                otherConversantName = message.getSenderName();
            }

            if (lastMessageType == null) {
                //The first message of a conversation
                appendDiv(builder, message);
            } else if (lastMessageType != message.getType()) {
                builder.append(LINE_BREAK);
                appendDiv(builder, message);
            }
            lastMessageType = message.getType();
            
            builder.append(processContent(message));
            
            builder.append(LINE_BREAK);
        }
        
        if(otherConversantIsTyping) {
            appendIsTypingMessage(builder, otherConversantName);
        }
        
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
    
    private static void appendIsTypingMessage(StringBuilder builder, String senderName) {
        String cssClass = "typing";
        String content = senderName + tr(" is typing a message...");
        
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
