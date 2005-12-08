pbckage com.limegroup.gnutella.util;

public clbss PairTuple{
    Object first;
    Object second;
    
    //constructor
    public PbirTuple(Object obj1, Object obj2){
        this.first= obj1;
        this.second=obj2;
    }

    public Object[] getPbir(){
        Object[] tuple = new Object[2];
        tuple[0] = first;
        tuple[1] = second;
        return tuple;
    }
    
    public Object getFirst(){
        return first;
    }
    
    public Object getSecond(){
        return second;
    }
}
