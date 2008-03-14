package org.limewire.promotion.containers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.util.ByteUtil;

import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * Wraps multiple messages into one message, generally so you can stuff a bunch
 * of messages inside a {@link SignedMessageContainer}.
 */
public class MultiMessageContainer extends MapMessageContainer {
    private static final String KEY_WRAPPED_BYTES = "W";

    public byte[] getType() {
        return ByteUtil.toUTF8Bytes("MULT");
    }

    @Override
    public void parse(GGEP rawGGEP) throws BadGGEPBlockException {
        if (!rawGGEP.hasKey(KEY_WRAPPED_BYTES))
            throw new BadGGEPBlockException("Missing wrappedBytes");
        super.parse(rawGGEP);
    }

    /**
     * @return the wrapped bytes, or a new empty array if there is a problem
     *         loading the array.
     */
    private byte[] getWrappedBytes() {
        byte[] bytes = getBytes(KEY_WRAPPED_BYTES);
        if (bytes == null)
            return new byte[0];
        return bytes;
    }

    /**
     * @return a list of wrapped messages, in the same order they were added.
     *         This method does not cache results, so a second call will create
     *         a new list with new instances of the wrapped messages.
     */
    public List<MessageContainer> getWrappedMessages() {
        List<MessageContainer> list = new ArrayList<MessageContainer>();
        byte[] bytes = getWrappedBytes();
        if (bytes.length > 0) {
            int[] nextOffset = new int[1];
            int offset = 0;
            MessageContainerParser parser = new MessageContainerParser();
            while (offset < bytes.length) {
                try {
                    list.add(parser.parse(new GGEP(bytes, offset, nextOffset)));
                    offset = nextOffset[0];
                } catch (BadGGEPBlockException ex) {
                    throw new RuntimeException("Parsing error: ", ex);
                }
            }
        }
        return list;
    }

    /**
     * Takes the given list and encodes it into the wrapping system of this
     * message, so later changes to the given list will NOT be reflected by this
     * container.
     */
    public void setWrappedMessages(List<MessageContainer> messages) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (MessageContainer message : messages) {
            try {
                out.write(message.getEncoded());
            } catch (IOException ex) {
                throw new RuntimeException("IOException? WTF?", ex);
            }
        }
        put(KEY_WRAPPED_BYTES, out.toByteArray());
    }
}
