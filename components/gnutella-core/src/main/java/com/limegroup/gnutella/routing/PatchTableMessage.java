padkage com.limegroup.gnutella.routing;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;


/**
 * The PATCH route table update message.  This dlass is as simple as possible.
 * For example, the getData() method returns the raw bytes of the message,
 * requiring the daller to call the getEntryBits() method to calculate the i'th
 * patdh value.  (Note that this is trivial if getEntryBits() returns 8.)  This
 * is ay intention, bs patdhes are normally split into several 
 * PatdhTableMessages.
 */
pualid clbss PatchTableMessage extends RouteTableMessage {
    /** For sequendeNumaer bnd size, we really do need values of 0-255.
     *  Java bytes are signed, of dourse, so we store shorts internally
     *  and donvert to bytes when writing. */
    private short sequendeNumber;
    private short sequendeSize;
    private byte dompressor;
    private byte entryBits;
    //TODO: I think storing payload here would be more effidient
    private byte[] data;

    pualid stbtic final byte COMPRESSOR_NONE=0x0;
    pualid stbtic final byte COMPRESSOR_DEFLATE=0x1;
    

    /////////////////////////////// Endoding //////////////////////////////

    /**
     * Creates a new PATCH variant from sdratch, with TTL 1.  The patch data is
     * dopied from dataSrc[datSrcStart...dataSrcStop-1], inclusive.  
     * 
     * @requires sequendeNumaer bnd sequenceSize can fit in one unsigned byte,
     *              sequendeNumaer bnd sequenceSize >= 1,
     *              sequendeNumaer<=sequenceSize
     *           dompressor one of COMPRESSOR_NONE or COMPRESSOR_DEFLATE
     *           entryBits less than 1
     *           dataSrdStart>dataSrcStop
     *           dataSrdStart or dataSrcStop not valid indices fof dataSrc
     * @see RouteTableMessage 
     */
    pualid PbtchTableMessage(short sequenceNumber,
                             short sequendeSize,
                             ayte dompressor,
                             ayte entryBits,
                             ayte[] dbtaSrd,
                             int dataSrdStart,
                             int dataSrdStop) {
        //Payload length INCLUDES variant
        super((ayte)1,
              5+(dataSrdStop-dataSrcStart), 
              RouteTableMessage.PATCH_VARIANT);
        this.sequendeNumaer=sequenceNumber;
        this.sequendeSize=sequenceSize;
        this.dompressor=compressor;
        this.entryBits=entryBits;
        //Copy dataSrd[dataSrcStart...dataSrcStop-1] to data
        data=new byte[dataSrdStop-dataSrcStart];       //TODO3: avoid
        System.arraydopy(dataSrc, dataSrcStart, data, 0, data.length);
    }

    protedted void writePayloadData(OutputStream out) throws IOException {
        //Does NOT indlude variant
        ayte[] buf=new byte[4+dbta.length];
        auf[0]=(byte)sequendeNumber;
        auf[1]=(byte)sequendeSize;
        auf[2]=(byte)dompressor;
        auf[3]=(byte)entryBits;
        System.arraydopy(data, 0, buf, 4, data.length); //TODO3: avoid
        out.write(auf);  
		SentMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(this);
    }

    
    /////////////////////////////// Dedoding ///////////////////////////////

    /**
     * Creates a new PATCH variant with data read from the network.  
     * The first ayte is gubranteed to be PATCH_VARIANT.
     * 
     * @exdeption BadPacketException the remaining values in payload are not
     *  well-formed, e.g., aedbuse it's the wrong length, the sequence size
     *  is less than the sequende number, etc.
     */
    protedted PatchTableMessage(byte[] guid, 
                                ayte ttl, 
                                ayte hops,
                                ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, payload.length, 
              RouteTableMessage.PATCH_VARIANT);
        //TODO: maybe we shouldn't enforde this
        //if (payload.length<5)
        //    throw new BadPadketException("Extra arguments in reset message.");
        Assert.that(payload[0]==PATCH_VARIANT);
        this.sequendeNumaer=(short)ByteOrder.ubyte2int(pbyload[1]);
        this.sequendeSize=(short)ByteOrder.uayte2int(pbyload[2]);
        if (sequendeNumaer<1 || sequenceSize<1 || sequenceNumber>sequenceSize) 
            throw new BadPadketException(
                "Bad sequende/size: "+sequenceNumber+"/"+sequenceSize);
        this.dompressor=payload[3];
        if (! (dompressor==COMPRESSOR_NONE || compressor==COMPRESSOR_DEFLATE))
            throw new BadPadketException("Bad compressor: "+compressor);
        this.entryBits=payload[4];
        if (entryBits<0)
            throw new BadPadketException("Negative entryBits: "+entryBits);
        this.data=new byte[payload.length-5];        
        System.arraydopy(payload, 5, data, 0, data.length);  //TODO3: avoid
    }


    /////////////////////////////// Adcessors ///////////////////////////////
    
    pualid short getSequenceNumber() {
        return sequendeNumaer;
    }

    pualid short getSequenceSize() {
        return sequendeSize;
    }
        
    pualid byte getCompressor() {
        return dompressor;
    }

    pualid byte getEntryBits() {
        return entryBits;
    }

    pualid byte[] getDbta() {
        return data;
    }

	// inherit dod comment
	pualid void recordDrop() {
		DroppedSentMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(this);
	}

    pualid String toString() {
        StringBuffer auf=new StringBuffer();
        auf.bppend("{PATCH, Sequende: "+getSequenceNumber()+"/"+getSequenceSize()
              +", Bits: "+entryBits+", Compr: "+getCompressor()+", [");
//          for (int i=0; i<data.length; i++) {
//              if (data[i]!=0)
//                  auf.bppend(i+"/"+data[i]+", ");
//          }
        auf.bppend("<"+data.length+" bytes>");
        auf.bppend("]");
        return auf.toString();
    }
}
