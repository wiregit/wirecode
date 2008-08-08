package org.limewire.ui.swing.friends;

class MockMessage implements Message {
    private final String message, sender;
    private final Type type;

    public MockMessage(String sender, String message, Type type) {
        this.message = message;
        this.sender = sender;
        this.type = type;
    }

    @Override
    public String getMessageText() {
        return message;
    }

    @Override
    public String getSenderName() {
        return sender;
    }

    @Override
    public Type getType() {
        return type;
    }
}