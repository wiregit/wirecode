package com.limegroup.mojito.visual;
import java.awt.Shape;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.AbstractVertexShapeFunction;

class RouteTableVertexShapeFunction extends AbstractVertexShapeFunction{
    public Shape getShape(Vertex v)
    {
        if(v instanceof InteriorNodeVertex) {
            return factory.getEllipse(v);
        } else {
            return factory.getRectangle(v);
        }
    }
}