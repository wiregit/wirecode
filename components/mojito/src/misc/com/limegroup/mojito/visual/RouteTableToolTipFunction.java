package com.limegroup.mojito.visual;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;

public class RouteTableToolTipFunction extends DefaultToolTipFunction{
    @Override
    public String getToolTipText(Vertex v) {
        if(v instanceof BucketVertex) {
            BucketVertex bv = (BucketVertex)v;
            String ttString = "<html>"+bv.getBucket().toString().replace("\n","<br>")+"</html>";
            return ttString;
        }
        return super.getToolTipText(v);
    }
    
}
