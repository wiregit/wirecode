package com.limegroup.mojito.visual;

import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.routing.impl.RouteTableImpl.RouteTableCallback;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseTree;
import edu.uci.ics.jung.utils.UserData;

public abstract class RouteTableGraph implements RouteTableCallback{
    
    protected RouteTableImpl routeTable;
    protected RootableSparseTree tree;
    protected Vertex root;
    protected RouteTableGraphCallback callback;

    public RouteTableGraph(RouteTableImpl routeTable, RouteTableGraphCallback callback) {
        this.routeTable = routeTable;
        this.callback = callback;
        routeTable.setRouteTableCallback(this);
    }
    
    public abstract void populateGraph();
    
    public abstract String getGraphInfo();
    
    public SparseTree getTree(){
        return tree;
    }
    
    public void deregister() {
        routeTable.setRouteTableCallback(null);
    }
    
    public class RootableSparseTree extends SparseTree {
        public RootableSparseTree(Vertex root) {
            super(root);
        }
        
        /**
         * Clears the tree and add this <tt>Vertex</tt>
         * as the new root
         */
        public synchronized void newRoot(Vertex root) {
            removeAllEdges();
            removeAllVertices();
            this.mRoot = root;
            addVertex( root );
            mRoot.setUserDatum(SPARSE_ROOT_KEY, SPARSE_ROOT_KEY, UserData.SHARED);
            mRoot.setUserDatum(IN_TREE_KEY, IN_TREE_KEY, UserData.SHARED);
        }
    }
}
