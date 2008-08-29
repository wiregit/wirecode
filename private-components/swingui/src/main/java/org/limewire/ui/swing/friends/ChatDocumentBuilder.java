package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.friends.Message.Type;
import static org.limewire.ui.swing.util.I18n.tr;

import java.util.ArrayList;

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
    
    public static String buildChatText(ArrayList<Message> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        for(int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if(!isPreviousFromSameSender(message, messages, i)) {
                appendDiv(builder, message);
            }
            builder.append(processContent(message));
            
            builder.append(LINE_BREAK).append(isNextFromSameSender(message, messages, i) ? "" : LINE_BREAK);
        }
        
        builder.append(BOTTOM);
        return builder.toString();
    }

    private static StringBuilder appendDiv(StringBuilder builder, Message message) {
        Type type = message.getType();
        String cssClass = type == Type.Sent ? "me" : type == Type.Received ? "them" : "typing";
        String content = message.getSenderName();
        if (type == Type.Typing) {
            content += tr(" is typing a message...");
        }
        return builder.append("<div class=\"")
        .append(cssClass)
        .append("\">")
        .append(content)
        .append(type != Type.Typing ? ":" : "")
        .append("</div>");
    }

    private static String processContent(Message message) {
        if (message.getType() == Type.Typing) {
            return "";
        }
        
        String messageText = message.getMessageText();
        
        messageText = messageText.replace("<", "&lt;").replace(">", "&gt;");
        
        return URLWrapper.wrap(messageText);
    }
    
    private static boolean isPreviousFromSameSender(Message message, ArrayList<Message> messages, int index) {
        return index > 0 && message.getType() == messages.get(index - 1).getType();
    }
    
    private static boolean isNextFromSameSender(Message message, ArrayList<Message> messages, int index) {
        return index + 1 < messages.size() && message.getType() == messages.get(index + 1).getType();
    }
}
