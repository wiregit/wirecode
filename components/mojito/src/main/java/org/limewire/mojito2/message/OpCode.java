package org.limewire.mojito2.message;

import java.io.IOException;

enum OpCode {
    
    PING_REQUEST(0x01),
    PING_RESPONSE(0x02),
    
    STORE_REQUEST(0x03),
    STORE_RESPONSE(0x04),
    
    FIND_NODE_REQUEST(0x05),
    FIND_NODE_RESPONSE(0x06),
    
    FIND_VALUE_REQUEST(0x07),
    FIND_VALUE_RESPONSE(0x08);
    
    private final int opcode;
        
    private OpCode(int opcode) {
        this.opcode = opcode;
    }

    public int byteValue() {
        return opcode;
    }
    
    public boolean isRequest() {
        switch (this) {
            case PING_REQUEST:
            case FIND_NODE_REQUEST:
            case FIND_VALUE_REQUEST:
            case STORE_REQUEST:
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return name() + " (" + byteValue() + ")";
    }
    
    private static OpCode[] VALUES;
    
    static {
        OpCode[] values = values();
        VALUES = new OpCode[values.length];
        for (OpCode o : values) {
            int index = o.opcode % VALUES.length;
            if (VALUES[index] != null) {
                throw new IllegalStateException();
            }
            VALUES[index] = o;
        }
    }
    
    public static OpCode valueOf(int opcode) throws IOException {
        OpCode o = VALUES[opcode % VALUES.length];
        if (o != null && o.opcode == opcode) {
            return o;
        }
        throw new IOException("OpCode=" + opcode);
    }
    
    public static OpCode valueOf(Message message) {
        if (message instanceof PingRequest) {
            return PING_REQUEST;
        } else if (message instanceof PingResponse) {
            return PING_RESPONSE;
        } else if (message instanceof NodeRequest) {
            return FIND_NODE_REQUEST;
        } else if (message instanceof NodeResponse) {
            return FIND_NODE_RESPONSE;
        } else if (message instanceof ValueRequest) {
            return FIND_VALUE_REQUEST;
        } else if (message instanceof ValueResponse) {
            return FIND_VALUE_RESPONSE;
        } else if (message instanceof StoreRequest) {
            return STORE_REQUEST;
        } else if (message instanceof StoreResponse) {
            return STORE_RESPONSE;
        }
        
        throw new IllegalArgumentException("message=" + message);
    }
}
