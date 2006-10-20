package com.limegroup.mojito.visual;

import com.limegroup.mojito.routing.Bucket;

public interface RouteTableUICallback {
    
    public void handleBucketSelected(Bucket bucket);
    
    public void handleNodeGraphRootSelected();

}
