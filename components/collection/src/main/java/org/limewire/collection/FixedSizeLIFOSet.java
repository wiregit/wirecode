package org.limewire.collection;

/**
 * A fixed size Last-In-First-Out (LIFO) Set.
 * A fixed size <code>Set</code> where the last added element is the first 
 * item in the list. Upon reaching the capacity of elements, 
 * <code>FixedSizeLIFOSet</code> removes either the last item first (default)
 * first item first (FIFO).
 <pre>
 
    public class MyObject{
        public String s;
        public int item;
        public MyObject(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item ;
        }
    }   
 
    void sampleCodeFixedSizeLIFOSet(){
        FixedSizeLIFOSet<MyObject> fslsLIFO = new FixedSizeLIFOSet<MyObject>(3, FixedSizeLIFOSet.EjectionPolicy.LIFO);
        
        if(!fslsLIFO.add(new MyObject("1", 1)))
            System.out.println("Add failed 1");
        System.out.println(fslsLIFO);
        if(!fslsLIFO.add(new MyObject("2", 2)))
            System.out.println("Add failed 2");
        System.out.println(fslsLIFO);
        if(!fslsLIFO.add(new MyObject("3", 3)))
            System.out.println("Add failed 3");
        System.out.println(fslsLIFO);
        if(!fslsLIFO.add(new MyObject("4", 4)))
            System.out.println("Add failed 4");
        System.out.println(fslsLIFO);
        
        System.out.println("********************");

        FixedSizeLIFOSet<MyObject> fslsFIFO = new FixedSizeLIFOSet<MyObject>(3, FixedSizeLIFOSet.EjectionPolicy.FIFO);
        
        if(!fslsFIFO.add(new MyObject("1", 1)))
            System.out.println("Add failed 1");
        System.out.println(fslsFIFO);
        if(!fslsFIFO.add(new MyObject("2", 2)))
            System.out.println("Add failed 2");
        System.out.println(fslsFIFO);
        if(!fslsFIFO.add(new MyObject("3", 3)))
            System.out.println("Add failed 3");
        System.out.println(fslsFIFO);
        if(!fslsFIFO.add(new MyObject("4", 4)))
            System.out.println("Add failed 4");
        System.out.println(fslsFIFO);
    }
    Output:
        [1=1]
        [2=2, 1=1]
        [3=3, 2=2, 1=1]
        [4=4, 2=2, 1=1]
        ********************
        [1=1]
        [2=2, 1=1]
        [3=3, 2=2, 1=1]
        [4=4, 3=3, 2=2]
 </pre>

 */
public class FixedSizeLIFOSet<E> extends LIFOSet<E> {

	/**
	 * The EjectionPolicy controls which element should
	 * be removed from the Set if has reached its maximum
	 * capacity.
	 */
	public static enum EjectionPolicy {
		/**
		 * Removes the last-in (newest) element from the
		 * Set if it has reached its maximum capacity.
		 */
		LIFO,
		
		/**
		 * Removes the first-in (eldest) element from the
		 * Set if has reached its maximum capacity.
		 */
		FIFO;
	}
	
    final int maxSize;
    
    private final EjectionPolicy policy;
    
    public FixedSizeLIFOSet(int maxSize) {
        this(maxSize, EjectionPolicy.LIFO);
    }

    public FixedSizeLIFOSet(int maxSize, EjectionPolicy policy) {
    	this.maxSize = maxSize;
    	this.policy = policy;
    }
    
    public FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor) {
    	this(maxSize, initialCapacity, loadFactor, EjectionPolicy.LIFO);
    }
    
    public FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor, EjectionPolicy policy) {
        super(initialCapacity, loadFactor);
        this.maxSize = maxSize;
        this.policy = policy;
    }
    
    @Override
    public boolean add(E o) {
    	boolean added = super.add(o);
    	if (added && size() > maxSize) {
    		if (policy == EjectionPolicy.FIFO) {
    			remove(0);
    			
    		} else { // EjectionPolicy.LIFO
    			remove(Math.max(0, size()-2));
    		}
    		
    		assert (size() <= maxSize);
    	}
        
        return added;
    }
}
