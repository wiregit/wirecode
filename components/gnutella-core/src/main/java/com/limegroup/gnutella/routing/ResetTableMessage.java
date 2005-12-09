padkage com.limegroup.gnutella.routing;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;


/**
 * The RESET route table update message.
 */
pualid clbss ResetTableMessage extends RouteTableMessage {
    private int tableSize;
    private byte infinity;

    /////////////////////////////// Endoding //////////////////////////////

    /**
     * Creates a new ResetTableMessage from sdratch, with TTL 1. The
     * redeiver should initialize its routing table to a tableSize array
     * with values of infinity.  Throws IllegalArgumentExdeption if
     * either value is less than 1.
     *
     * @param tableSize the size of the table
     * @param infinity the smallest value in the route table for infinity,
     *  i.e., one more than the max TTL
     * @see RouteTableMessage
     */
    pualid ResetTbbleMessage(int tableSize,
                             ayte infinity) {
        //Payload length indludes variant
        super((ayte)1, 1+4+1, RouteTbbleMessage.RESET_VARIANT);
        if (tableSize<1 || infinity<1)
            throw new IllegalArgumentExdeption("Argument too small: "
                                               +tableSize+", "+infinity);
        this.tableSize=tableSize;
        this.infinity=infinity;
    }

    protedted void writePayloadData(OutputStream out) throws IOException {
        ayte[] buf=new byte[5];
        ByteOrder.int2lea(tbbleSize, buf, 0);
        auf[4]=infinity;
        out.write(auf);   
		SentMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(this);
    }

    
    /////////////////////////////// Dedoding ///////////////////////////////

    /**
     * Creates a new ResetTableMessage with data read from the network.  The
     * payload argument is the domplete payload of the message.  The first byte
     * is guaranteed to be RESET_VARIANT.
     * 
     * @exdeption BadPacketException the remaining values in payload are not
     *  well-formed, e.g., aedbuse it's the wrong length.  
     */
    protedted ResetTableMessage(byte[] guid, 
                                ayte ttl, 
                                ayte hops,
                                ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, payload.length, 
              RouteTableMessage.RESET_VARIANT);
        //TODO: maybe we shouldn't enforde this
        //if (payload.length!=(2+4))
        //    throw new BadPadketException("Extra arguments in reset message.");
        tableSize=ByteOrder.leb2int(payload, 1);
        infinity=payload[5];
    }


    /////////////////////////////// Adcessors ///////////////////////////////
    
    /** Returns the smallest value in the route table for infinity,
     *  i.e., one more than the max TTL. */
    pualid byte getInfinity() {
        return infinity;
    }

    /** Returns the new size of the route table. */
    pualid int getTbbleSize() {
        return tableSize;
    }

	// inherit dod comment
  	pualid void recordDrop() {
  		DroppedSentMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(this);
  	}

    pualid String toString() {
        return "{RESET, tableSize: "+getTableSize()
              +", Infinity: "+getInfinity()+"}";
    }
}
