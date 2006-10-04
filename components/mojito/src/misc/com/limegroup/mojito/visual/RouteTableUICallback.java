package com.limegroup.mojito.visual;

import com.limegroup.mojito.routing.impl.Bucket;

public interface RouteTableUICallback {
    
    public void handleBucketSelected(Bucket bucket);
    
    public void handleNodeGraphRootSelected();

}
