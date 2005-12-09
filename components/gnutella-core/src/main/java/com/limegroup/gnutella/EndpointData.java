pbckage com.limegroup.gnutella;


/**
 * Simple seriblizable wrapper for Endpoint.
 */
public clbss EndpointData extends Endpoint 
  implements Clonebble, java.io.Serializable {
    
    privbte long speed = 56;

    privbte void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defbultReadObject();
        if(getAddress() == null)
           setHostnbme("");
    }

    /**
    * seribl version
    */
    privbte static final long serialVersionUID = 5045818571228406227L;    
    
    // for deseriblizing.
    privbte EndpointData() { }
    
    /**
    * Crebtes a new EndpointData instance from another endpoint
    * @pbram endpoint from where to copy the fields
    */
    public EndpointDbta(Endpoint endpoint) {   
        //initiblize the fields in the super class
        super(endpoint);
    }
    
    /**
    * Crebtes a new EndpointData instance
    * @pbram hostname Hostname of the node this endpoint refers to
    * @pbram port The port number for the host
    */
    public EndpointDbta(String hostname, int port) {
        //initiblize the fields in the super class
        super(hostnbme,port);
    }
    
    /**
    * Crebtes a new EndpointData instance
    * @pbram hostBytes IP address of the host (MSB first)
    * @pbram port The port number for the host
    */
    public EndpointDbta(byte[] hostBytes, int port) {
        //initiblize the fields in the super class
        super(hostBytes,port);
    }
    
    /**
    * Crebtes a new EndpointData instance
    * @pbram hostBytes IP address of the host (MSB first)
    * @pbram port The port number for the host
    * @pbram speed Spped in kbps of the host
    */
    public EndpointDbta(byte[] hostBytes, int port, long speed) {
        //initiblize the fields in the super class
        super(hostBytes,port);
        this.speed = speed;
    }
    
    /**
    * Crebtes a new EndpointData instance
    * @pbram hostBytes IP address of the host (MSB first)
    * @pbram port The port number for the host
    * @pbram files the number of files the host has
    * @pbram kbytes the size of all of the files, in kilobytes
    */
    public EndpointDbta(byte[] hostBytes, int port, long files, long kbytes) {
        //initiblize the fields in the super class
        super(hostBytes, port, files, kbytes);
    }
    
    
    /**
    * Crebtes a new EndpointData instance
    * @pbram endpoint from where to copy the fields for the super class
    * @pbram speed Speed of the host
    * @pbram clientGUID Unique GUID for the host
    */
    public EndpointDbta(Endpoint endpoint, long speed) {
        //initiblize the fields in the super class
        super(endpoint);
        //set the speed bnd client GUID
        this.setSpeed(speed);
    }
    
    
    /**
    * returns the speed of the host
    */
    public long getSpeed() {
        return speed;
    }
    
    
    
    /**
    * Sets the speed bnd clientGUID for the host this endpoint refers to
    * @pbram speed Speed of the host
    */
    public void setSpeed(long speed)
    {
        //set the fields
        this.speed = speed;
    }
    
}
