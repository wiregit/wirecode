package org.limewire.ui.swing.friends;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MockFileMetadata;
import org.limewire.xmpp.api.client.Presence.Mode;

public class ChatDocumentBuilderTest extends TestCase {
    public void testBuildChatTextWithLinks() {
        compareOutput("hey http://www.foo.com there",
                      "<div class=\"me\">me:</div>hey <a href=\"http://www.foo.com\">http://www.foo.com</a> there<br/>");
        
        compareOutput("hey foo.com there", 
                      "<div class=\"me\">me:</div>hey <a href=\"http://foo.com\">foo.com</a> there<br/>");
        
        compareOutput("hey www.foo.com there", 
                      "<div class=\"me\">me:</div>hey <a href=\"http://www.foo.com\">www.foo.com</a> there<br/>");

        compareOutput("hey https://www.foo.com there", 
                      "<div class=\"me\">me:</div>hey <a href=\"https://www.foo.com\">https://www.foo.com</a> there<br/>");

        compareOutput("hey www.foo there", 
                      "<div class=\"me\">me:</div>hey www.foo there<br/>");

        compareOutput("hey magnet://foo.bar now", 
        "<div class=\"me\">me:</div>hey <a href=\"magnet://foo.bar\">magnet://foo.bar</a> now<br/>");
    }
    
    public void testBuildChatTextWithMarkup() {
        compareOutput("hey <b>foo</b> there", 
                      "<div class=\"me\">me:</div>hey &lt;b&gt;foo&lt;/b&gt; there<br/>");
    }
    
    public void testBuildChatTextForRealMessage() {
        compareOutput("<div class=\"them\">you:</div>hey there<br/>", ChatState.active, 
                new Type[] {Type.Received}, 
                new String[] {"hey there"});
    }

    public void testBuildChatTextForExternallyInitiatedConversationWhenTheyAreTyping() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
                    .append("<div class=\"typing\">you is typing a message...</div><br/>");
        compareOutput(conversation.toString(), ChatState.composing, 
                new Type[] {Type.Received},  
                new String[] {"heynow"});
    }

    public void testBuildChatTextForExternallyInitiatedConversationWhenTheyArePaused() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
        .append("<div class=\"typing\">you has entered text</div><br/>");
        compareOutput(conversation.toString(), ChatState.paused, 
                new Type[] {Type.Received},  
                new String[] {"heynow"});
    }

    public void testBuildChatTextForConversationWhenTheyHaveSignedOff() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
        .append("<div class=\"typing\">you has signed off</div><br/>");
        compareOutput(conversation.toString(), null, true, 
                new Type[] {Type.Received},  
                new String[] {"heynow"});
    }

    public void testBuildChatTextShowingTypingMessagesRemovedAfterReceivingFollowupMessage() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
                    .append("foobar<br/>");
        compareOutput(conversation.toString(), ChatState.active,
                new Type[] {Type.Received, Type.Received},  
                new String[] {"heynow", "foobar"});
    }
    
    public void testBuildChatTextForATwoPartyExchangeThatIsExternallyInitiated() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/><br/>")
                    .append("<div class=\"me\">me:</div>yo<br/><br/>")
                    .append("<div class=\"them\">you:</div>fooey<br/>");
        
        compareOutput(conversation.toString(), ChatState.active, 
                new Type[] {Type.Received, Type.Sent, Type.Received}, 
                new String[] {"heynow", "yo", "fooey"});
    }

    public void testBuildChatTextForMessagesISendMoreThan60SecondsApart() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"me\">me:</div>heynow<br/><br/>")
        .append("<div class=\"me\">me:</div>yo<br/>");
        
        MockChatFriend friend = new MockChatFriend(null, null, Mode.available);
        ArrayList<Message> messages = new ArrayList<Message>();
        messages.add(new MockMessage(friend, "heynow", 0, "me", Type.Sent, null));      
        messages.add(new MockMessage(friend, "yo", 600001, "me", Type.Sent, null));      
        compareOutput(conversation.toString(), null, false, messages);
    }
    
    public void testBuildChatTextAfterBeingOfferedAFile() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>wants to share a file with you<br/>")
                    .append("<form action=\"\"><input type=\"hidden\" name=\"fileid\" value=\"heynow-fileid\"/><input type=\"submit\" value=\"Foo doc.doc\"/></form><br/>")
                    .append("Download it now, or get it from his <a href=\"#library\">Library</a> later<br/>");
        
        MockChatFriend friend = new MockChatFriend(null, null, Mode.available);
        ArrayList<Message> messages = new ArrayList<Message>();
        messages.add(new MockMessage(friend, null, 0, "you", Type.FileOffer, new MockFileMetadata("heynow-fileid", "Foo doc.doc")));
        
        compareOutput(conversation.toString(), ChatState.active, false, messages);
    }

    private void compareOutput(String input, String expected) {
        compareOutput(expected, ChatState.active, new Type[] {Type.Sent}, input);
    }

    private void compareOutput(String expected, ChatState state, Type[] type, String... input) {
        compareOutput(expected, state, false, getMessages(type, input));
    }

    private void compareOutput(String expected, ChatState state, boolean friendHasSignedOff, Type[] type, String... input) {
        compareOutput(expected, state, friendHasSignedOff, getMessages(type, input));
    }

    private void compareOutput(String expected, ChatState state, boolean friendHasSignedOff, ArrayList<Message> messages) {
        String chatText = ChatDocumentBuilder.buildChatText(messages, state, "you", friendHasSignedOff);
        
        assertTrue(chatText.startsWith(ChatDocumentBuilder.TOP));
        assertTrue(chatText.endsWith(ChatDocumentBuilder.BOTTOM));
        
        chatText = chatText.replace(ChatDocumentBuilder.TOP, "");
        chatText = chatText.replace(ChatDocumentBuilder.BOTTOM, "");
        
        assertEquals(expected, chatText);
    }
    
    private ArrayList<Message> getMessages(Type types[], String... messages) {
        ArrayList<Message> list = new ArrayList<Message>();
        for(int i = 0; i < messages.length; i++) {
            Type type = types[i];
            list.add(new MessageImpl(type == Type.Sent ? "me" : "you", null, null, messages[i], type, null));
        }
        return list;
    }
}
