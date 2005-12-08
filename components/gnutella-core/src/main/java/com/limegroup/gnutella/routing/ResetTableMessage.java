pbckage com.limegroup.gnutella.routing;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;


/**
 * The RESET route tbble update message.
 */
public clbss ResetTableMessage extends RouteTableMessage {
    privbte int tableSize;
    privbte byte infinity;

    /////////////////////////////// Encoding //////////////////////////////

    /**
     * Crebtes a new ResetTableMessage from scratch, with TTL 1. The
     * receiver should initiblize its routing table to a tableSize array
     * with vblues of infinity.  Throws IllegalArgumentException if
     * either vblue is less than 1.
     *
     * @pbram tableSize the size of the table
     * @pbram infinity the smallest value in the route table for infinity,
     *  i.e., one more thbn the max TTL
     * @see RouteTbbleMessage
     */
    public ResetTbbleMessage(int tableSize,
                             byte infinity) {
        //Pbyload length includes variant
        super((byte)1, 1+4+1, RouteTbbleMessage.RESET_VARIANT);
        if (tbbleSize<1 || infinity<1)
            throw new IllegblArgumentException("Argument too small: "
                                               +tbbleSize+", "+infinity);
        this.tbbleSize=tableSize;
        this.infinity=infinity;
    }

    protected void writePbyloadData(OutputStream out) throws IOException {
        byte[] buf=new byte[5];
        ByteOrder.int2leb(tbbleSize, buf, 0);
        buf[4]=infinity;
        out.write(buf);   
		SentMessbgeStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(this);
    }

    
    /////////////////////////////// Decoding ///////////////////////////////

    /**
     * Crebtes a new ResetTableMessage with data read from the network.  The
     * pbyload argument is the complete payload of the message.  The first byte
     * is gubranteed to be RESET_VARIANT.
     * 
     * @exception BbdPacketException the remaining values in payload are not
     *  well-formed, e.g., becbuse it's the wrong length.  
     */
    protected ResetTbbleMessage(byte[] guid, 
                                byte ttl, 
                                byte hops,
                                byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, pbyload.length, 
              RouteTbbleMessage.RESET_VARIANT);
        //TODO: mbybe we shouldn't enforce this
        //if (pbyload.length!=(2+4))
        //    throw new BbdPacketException("Extra arguments in reset message.");
        tbbleSize=ByteOrder.leb2int(payload, 1);
        infinity=pbyload[5];
    }


    /////////////////////////////// Accessors ///////////////////////////////
    
    /** Returns the smbllest value in the route table for infinity,
     *  i.e., one more thbn the max TTL. */
    public byte getInfinity() {
        return infinity;
    }

    /** Returns the new size of the route tbble. */
    public int getTbbleSize() {
        return tbbleSize;
    }

	// inherit doc comment
  	public void recordDrop() {
  		DroppedSentMessbgeStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(this);
  	}

    public String toString() {
        return "{RESET, tbbleSize: "+getTableSize()
              +", Infinity: "+getInfinity()+"}";
    }
}
