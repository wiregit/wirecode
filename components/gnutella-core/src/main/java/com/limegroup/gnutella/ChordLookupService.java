package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import edu.ucr.cs.chord.*;
import edu.ucr.cs.chord.filelookup.*;
import java.util.StringTokenizer;
import java.net.InetAddress;

/** 
    A facade to the ChordLookup backend. Called by the 
    routing layer for updates and lookups.

    The current version only supports using the Chord if you are a member. Future
    versions may support connecting to chord members and using it remotely.
*/

public class ChordLookupService 
{
      public static final int DEFAULT_CHORD_PORT=8888;

      Acceptor _acceptor;
      UploadManager _uploadManager;
      ActivityCallback _callback;
      DownloadManager _downloader;

      LocalFileLookupNode _lookup;

      boolean connected=false;
      boolean connecting=false;

      public void initialize(Acceptor acceptor, UploadManager uploadManager, DownloadManager downloader, ActivityCallback callback)
      {
	  _acceptor=acceptor;
	 _callback=callback;
	 _downloader=downloader;
	 _uploadManager=uploadManager;

	 // Adam: The line below determines which host is allowed to be the "first" chord host. It is
	 // necessary since in general nodes aren't allowed to become chord nodes before discovering another chord node.
	 // This should probably be replaced with a command line parameter or something in the future.

	 try{
	    // WARNING: don't fool around with this property. There is
	    // invariably just ONE seednode per chord.
	    String seedNode=System.getProperty("edu.ucr.cs.chord.SeedNode");
	    if(seedNode==null || seedNode.equals("")) return;
	    
	    int localPort=DEFAULT_CHORD_PORT;
	    try{
	       localPort=Integer.parseInt(System.getProperty("edu.ucr.cs.chord.LocalPort"));
	    }catch(Exception e){}

	    if("true".equals(System.getProperty("edu.ucr.cs.chord.SeedNode")))
	    {
	       _lookup=new LocalFileLookupNode(localPort);
	       connected=true;
	    }
	    else {
	       connect(Integer.parseInt(seedNode.substring(seedNode.indexOf(":")+1)),
		       seedNode.substring(0,seedNode.indexOf(":")));
	    }
	 }catch(Exception e){e.printStackTrace();}
      }

      public boolean isMember()
      {
	 return connected;
      }
      
      public boolean isConnecting()
      {
	 return connecting;
      }

      public void connect(int remotePort, String remoteHost)
      {
	 connecting=true;
	 try{
	    int localPort=DEFAULT_CHORD_PORT;
	    try{
	       localPort=Integer.parseInt(System.getProperty("edu.ucr.cs.chord.LocalPort"));
	    }catch(Exception e) {}

	    // TODO: needs to connect to a known server instead of this nonsense.
	    _lookup=new LocalFileLookupNode(localPort, remotePort, remoteHost);
	    connected=true;
	    connecting=false;

//	    Thread.sleep(5000); // TODO: Temporary.
	 }catch(Exception e){
	    // TODO: nicer exception handling
	    e.printStackTrace();
	 }
      }

      /**
	 Adds a local file to the Chord DLT.
      */
      public void addFile(FileDesc file)
      {
	 if(!connected) return;

	 URN urn=null;
	 Iterator i=file.getUrns().iterator();
	 while(i.hasNext()) {
	    URN u=(URN)i.next();
	    if(u.getUrnType().isSHA1()) {
	       urn=u;
	       break;
	    }
	 }

	 Assert.that(urn!=null,"File must have SHA-1 hash to be added to Chord");

	 try{
	    _lookup.add(urn.toString(),"gnutella://"+InetAddress.getLocalHost().getHostName()+":"+_acceptor.getPort());
	 }catch(Exception e) {
	    // TODO: Handle this nicer
	    e.printStackTrace();
	    return;
	 }
      }

      /**
	 Looks up a file in the Chord DLT. This should all be
	 done asynchronously, not like this!!!
      */
      public void lookupURN(URN urn)
      {
	 if(!connected) return;
	 System.out.println("Looking up URN: "+urn.toString());

	 try{
	    String results[]=_lookup.get(urn.toString());
	    if(results==null) return;
	    Set urns=new HashSet();
	    urns.add(urn);

//	    System.out.println("My address: "+InetAddress.getLocalHost()+" my port: "+_acceptor.getPort());

	    for(int i=0;i<results.length;i++)
	    {
	       String result=results[i];
	       result=result.substring("gnutella://".length()); // ok, so it's an ugly hack.
	       
	       String host=result.substring(0,result.indexOf(":"));
	       int port=Integer.parseInt(result.substring(result.indexOf(":")+1));
	       
	       // don't spoof packets from ourselves to ourselves.
	       if(InetAddress.getByName(host).equals(InetAddress.getLocalHost()) &&
		  _acceptor.getPort()==port)
		  continue;

	       Response response=new Response(0,
					      0,
					      null,
					      urns);

	       QueryReply queryReply=new QueryReply(new byte[] {'C','H','O','R','D',
								0,0,0,0,0,0,0,0,0,0,0}, 
						    (byte)4,    // TTL - don't care
						    port,
						    InetAddress.getByName(host).getAddress(),
						    3, // Speed - don't know
						    new Response[] {response},
						    new byte[16]); // CLIENT GUID - don't care 	       
						    

	       System.out.println("Spoofing query result: "+result);
	       _callback.handleQueryReply(queryReply);
	       _downloader.handleQueryReply(queryReply);
	    }	 

	 }catch(Exception e) {
	    // TODO: Handle this nicer
	    e.printStackTrace();
	    return;
	 }
      }
}






