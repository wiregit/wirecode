package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import org.limewire.inspection.InspectionException;
import org.limewire.inspection.InspectionUtils;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.BEncoder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.util.DataUtils;

/**
 * Response for an inspection request.
 */
public class InspectionResponse extends VendorMessage {
    
    private static final int VERSION = 1;
    
    /**
     * Creates a response for the provided inspection request.
     */
    public InspectionResponse(InspectionRequest request) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_RESP, VERSION,
                derivePayload(request));
        setGUID(new GUID(request.getGUID()));
    }
    
    private static byte[] derivePayload(InspectionRequest request) {
        /*
         * The format is a deflated bencoded mapping from
         * the indices of the inspected fields to their values.
         */
        String [] requested = request.getRequestedFields();
        Map<Integer, Object> responses = 
            new HashMap<Integer, Object>(requested.length);
        
        // if a timestamp was requested, it is put under the "-1" key.
        if (request.requestsTimeStamp())
            responses.put(-1,System.currentTimeMillis());
        
        for (int i = 0; i < requested.length; i++) {
            try {
                responses.put(i, InspectionUtils.inspectValue(requested[i]));
            } catch (InspectionException skip){}
        }
        
        if (responses.isEmpty())
            return DataUtils.EMPTY_BYTE_ARRAY;
        
        
        // since the inspected values may contain any character, bencoding is safest
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        try {
            try {
                BEncoder.getEncoder(dos, false, true,"UTF-8").encodeDict(responses);
            } catch (Throwable bencoding) {
                // a BEInspectable returned invalid object - report the error.
                String msg = bencoding.toString();
                String ret = "d5:error"+msg.length()+":"+msg+"e";
                dos.write(ret.getBytes());
            }
            dos.flush();
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        } finally {
            IOUtils.close(dos);
        }
        return baos.toByteArray();
    }

    /**
     * @return true if this response contains anything and 
     * should be sent.
     */
    public boolean shouldBeSent() {
        return getPayload().length > 0;
    }
}
