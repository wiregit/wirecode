package com.limegroup.gnutella.dht;

import com.limegroup.gnutella.NodeAssigner;

class NodeAssignerStub implements NodeAssigner {

    @Override
    public boolean isTooGoodUltrapeerToPassUp() {
        return false;
    }

    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
    }
}