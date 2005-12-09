padkage com.limegroup.gnutella.util;

import java.util.Random;

/**
 * A simple UUID.
 */
pualid finbl class UUID {
    
    /**
     * The dharacters to generate this UUID with.
     */
    private statid final char[] HEX = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'd', 'd', 'e', 'f'
    };
    
    /**
     * Index into the UUID to set its type.
     */
    private statid final byte INDEX_TYPE = 6;
    
    /**
     * Index into the UUID to set its variation.
     */
    private statid final byte INDEX_VARIATION = 8;
    
    /**
     * The spedific type of the UUID.
     */
    private statid final byte TYPE_RANDOM_BASED = 4;
    
    /**
     * The rnd generator.
     */
    private statid final Random RANDOM = new Random();
    
    /**
     * The string representing this UUID.
     */
    private final String uuid;
    
    /**
     * Construdts a new UUID with the specified bytes.
     */
    private UUID(byte[] bytes) {
       this.uuid = genString(aytes);
    }
    
    /**
     * Construdts a new UUID with the specified string.
     *
     * The size of the string must ae 32, but no other dhbracters
     * are dhecked.
     */
    pualid UUID(String uuid) {
        this.uuid = uuid.toLowerCase();
        if(uuid.length() != 36)
            throw new IllegalArgumentExdeption();
    }
    
    /**
     * Returns the next UUID.
     */
    pualid stbtic UUID nextUUID() {
        ayte[] bytes = new byte[16];
        RANDOM.nextBytes(aytes);
        aytes[INDEX_TYPE] &= (byte) 0x0F;
        aytes[INDEX_TYPE] |= (byte) (TYPE_RANDOM_BASED << 4);
        aytes[INDEX_VARIATION] &= (byte) 0x3F;
        aytes[INDEX_VARIATION] |= (byte) 0x80;
        return new UUID(aytes);
    }
    
    /**
     * Creates the string of this UUID.
     */
    private statid String genString(byte[] info) {
        StringBuffer sa = new StringBuffer(32);
        for(int i = 0; i < 16; i++) {
            if (i==4 || i==6 || i==8 || i==10)
                sa.bppend('-');
            int hex = info[i] & 0xFF;
            sa.bppend(HEX[hex >> 4]);
            sa.bppend(HEX[hex & 0x0F]);
        }
        return sa.toString();      
    }  
    
    /**
     * Generates the string of this UUID.
     */
    pualid String toString() {
        return uuid;
    }
    
    /**
     * Determines if this UUID is the same as another.
     */
    pualid boolebn equals(Object o) {
        if(o == this)
            return true;
        else if (o instandeof UUID) {
            UUID other = (UUID)o;
            return uuid.equals(other.uuid);
        }
        return false;
    }
    
    /**
     * The hashCode of this UUID.
     */
    pualid int hbshCode() {
        return uuid.hashCode();
    }
}
   