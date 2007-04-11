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

import com.limegroup.bittorrent.bencoding.BEncoder;
import com.limegroup.gnutella.util.DataUtils;

public class InspectionResponse extends VendorMessage {
    
    private static final int VERSION = 1;
    public InspectionResponse(InspectionRequest request) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_RESP, VERSION,
                derivePayload(request));
    }
    
    private static byte[] derivePayload(InspectionRequest request) {
        Map<String, String> responses = 
            new HashMap<String, String>(request.getRequestedFields().length);
        
        for (String requested : request.getRequestedFields()) {
            try {
                responses.put(requested, InspectionUtils.inspectValue(requested));
            } catch (InspectionException skip){}
        }
        
        if (responses.isEmpty())
            return DataUtils.EMPTY_BYTE_ARRAY;
        
        // since the inspected values may contain any character, bencoding is safest
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        try {
            BEncoder.encodeDict(dos, responses);
            dos.flush();
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        } finally {
            IOUtils.close(dos);
        }
        return baos.toByteArray();
    }
    
    public boolean shouldBeSent() {
        return getPayload().length > 0;
    }
}
