package com.limegroup.mojito.visual;
import java.awt.Paint;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.PickableVertexPaintFunction;
import edu.uci.ics.jung.visualization.PickedInfo;

class RouteTableVertexPaintFunction extends PickableVertexPaintFunction {
    
    private Paint localPaint;
    private Paint bucketFillPaint;
    
    public RouteTableVertexPaintFunction(
            PickedInfo pi, 
            Paint draw_paint, 
            Paint fill_paint,
            Paint bucket_fill_paint, 
            Paint picked_paint,
            Paint local_paint) {
        super(pi, draw_paint, fill_paint, picked_paint);
        this.localPaint = local_paint;
        this.bucketFillPaint = bucket_fill_paint;
    }

    @Override
    public Paint getDrawPaint(Vertex v) {
        return super.getDrawPaint(v);
    }

    @Override
    public Paint getFillPaint(Vertex v) {
        if(pi.isPicked(v)) {
            return picked_paint;
        }
        
        if(v instanceof BucketVertex) {
            BucketVertex bv = (BucketVertex) v;
            if(bv.isLocalBucket()) {
                return localPaint;
            } else {
                return bucketFillPaint;
            }
        } 
        
        return super.getFillPaint(v);
    }
}