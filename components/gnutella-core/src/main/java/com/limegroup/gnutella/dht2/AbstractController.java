package com.limegroup.gnutella.dht2;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

abstract class AbstractController extends Controller {

    protected final ConnectionServices connectionServices;
    
    public AbstractController(DHTMode mode, 
            ConnectionServices connectionServices) {
        super(mode);
        
        this.connectionServices = connectionServices;
    }
}
