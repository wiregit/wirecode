package org.limewire.ui.swing.friends.chat;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.text.html.StyleSheet;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class MessageTextImpl extends AbstractMessageImpl implements MessageText {

    private static final int MAX_LENGTH_PIXELS = 255;//smaller than chat window width to account for non-fixed width fonts

    private final String message;

    @Resource(key = "ChatInputPanel.textFont")
    private Font textFont;

    public MessageTextImpl(String senderName, ChatFriend chatFriend, Type type, String message) {
        this(senderName, chatFriend.getName(), chatFriend.getID(), type, message);
    }

    public MessageTextImpl(String senderName, String friendName, String friendId, Type type,
            String message) {
        super(senderName, friendName, friendId, type);
        GuiUtils.assignResources(this);
        this.message = message;
    }

    public String getMessageText() {
        return message;
    }

    public String toString() {
        return getMessageText();
    }

    public String format() {
        return insertBreaksAddAnchorsTags(message.replace("<", "&lt;").replace(">", "&gt;"));
    }

    /**
     * Takes the given string creating anchor tags for whereever it finds urls,
     * and creating wbr tags whenever a word over the MAX_LENGTH_PIXELS is
     * encountered.
     */
    private String insertBreaksAddAnchorsTags(String wrap) {
        StringTokenizer stringTokenizer = new StringTokenizer(wrap, " \n\t\r");
        StringBuffer htmlString = new StringBuffer();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            boolean isURL = URLWrapper.isURL(token);
            StringBuffer brokenString = new StringBuffer();
            String[] brokenTokens = breakString(token);
            for (int i = 0; i < brokenTokens.length; i++) {
                brokenString.append(brokenTokens[i]).append("<wbr>");
            }
            // if the string is a url make sure to wrap it in an anchor tag
            if (isURL) {
                htmlString.append(URLWrapper.createAnchorTag(token, brokenString.toString()));
            } else {
                htmlString.append(brokenString.toString());
            }

            htmlString.append(" ");
        }
        return htmlString.toString();
    }

    /**
     * Breaks up the given token into multiple Strings each with a maximum of
     * MAX_LENGTH_PIXELS wide.
     */
    private String[] breakString(String token) {
        int pixelWidth1Character = getPixelWidth(new String(new char[] { token.charAt(0) }),
                textFont);
        int maxCharacters = (MAX_LENGTH_PIXELS / pixelWidth1Character);
        List<String> brokenStrings = new ArrayList<String>();
        int index = 0;
        int length = token.length();
        while (index < length) {
            int start = index;
            int end = index + maxCharacters;
            if (end > length) {
                end = length;
            }
            String brokenString = token.substring(start, end);
            brokenStrings.add(brokenString);
            index = end;
        }
        return brokenStrings.toArray(new String[brokenStrings.size()]);
    }

    /**
     * Returns the width of the message in the given font and editor kit.
     */
    private int getPixelWidth(String text, Font font) {
        StyleSheet css = new StyleSheet();
        FontMetrics fontMetrics = css.getFontMetrics(font);
        return fontMetrics.stringWidth(text);
    }
}
