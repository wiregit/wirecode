package com.limegroup.gnutella.messages.vendor;

import java.io.File;

import org.limewire.inspection.Inspector;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class InspectionResponseFactoryImpl implements InspectionResponseFactory {

    private static final String INSPECTION_FILE = "inspection.props";
    
    private final Inspector inspector;
    
    @Inject
    public InspectionResponseFactoryImpl(Inspector inspector) {
        this.inspector = inspector;
        this.inspector.load(new File(CommonUtils.getCurrentDirectory(),INSPECTION_FILE));
        //TODO: put FEC object here eventually
    }
    
    public InspectionResponse[] createResponses(InspectionRequest request) {
        return new InspectionResponse[]{new InspectionResponse(request,inspector)};
    }

}
