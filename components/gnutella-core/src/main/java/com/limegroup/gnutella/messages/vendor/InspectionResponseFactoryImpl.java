package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import org.limewire.inspection.InspectionException;
import org.limewire.inspection.Inspector;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.BEncoder;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.DataUtils;

@Singleton
public class InspectionResponseFactoryImpl implements InspectionResponseFactory {

    private static final String INSPECTION_FILE = "inspection.props";
    
    private static final int OLD_VERSION = 1;
    
    private final Inspector inspector;
    
    @Inject
    public InspectionResponseFactoryImpl(Inspector inspector) {
        this.inspector = inspector;
        this.inspector.load(new File(CommonUtils.getCurrentDirectory(),INSPECTION_FILE));
        //TODO: put FEC object here eventually
    }
    
    public InspectionResponse[] createResponses(InspectionRequest request) {
        return new InspectionResponse[]{new InspectionResponse(OLD_VERSION, request.getGUID(), derivePayload(request))};
    }

    private byte[] derivePayload(InspectionRequest request) {
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
                responses.put(i, inspector.inspect(requested[i]));
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
}
