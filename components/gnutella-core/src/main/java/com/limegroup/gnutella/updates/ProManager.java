package com.limegroup.gnutella.updates;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.sun.net.ssl.SSLContext;
import com.bitzi.util.*;
import com.sun.net.ssl.TrustManagerFactory;
import com.sun.net.ssl.TrustManager;
import javax.net.ssl.*;
import javax.net.*;
import java.net.*;
import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.*;

import java.util.Arrays;

/**
 * Manages, buying of PRO from within LimeWire. Also, tells the rest of the code
 * if the instance of LimeWire has been upgraded to pro on this machine
 * @author Sumeet Thadani
 */
public class ProManager {
    
    private String macAddress = null;
    private final String START = "{";
    private final String END = "}";
    private final long EXPIRY = 16070400000l;//6*31*24*60*60*1000

    /////////////////////////External Interface////////////////////
    /**
     * @return true if we can find the MAC address of this machine - for
     * purposes of unique identifier, and we have a high enough version of java
     * to send server credit card info with SSL.
     */
    public boolean canBuyPro() {
        MacAddressFinder maf = new MacAddressFinder();
        this.macAddress = maf.getMacAddress();
        return(macAddress!=null && CommonUtils.isJava13OrLater());
    }

    /**
     * @exception IOException if unable to read the ProFile. This means the
     * file exists, but we are not able to read if for some reason. 
     * How should we deal with this? 
     * TODO1: Make sure IOEx is not thrown for character encoding reasons
     * TODO1: Make sure the locale is converted to us_en before we work on this
     */
    public boolean isPro() throws IOException, UnsupportedEncodingException {
        MacAddressFinder maf = new MacAddressFinder();
        this.macAddress = maf.getMacAddress();
        if(macAddress==null)
            return false;
        //read the file -- no file - we are not pro
        File file = null;
        byte[] bytes = null;
        file = new File(CommonUtils.getUserSettingsDir(),"ProFile");
        if(!file.exists())
            return false;
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        bytes = new byte[(int)raf.length()];
        raf.readFully(bytes);
        raf.close();
        String str = new String(bytes,"UTF-8");
        int index = str.indexOf("|");//works because we use base 32 on server
        if(index < 0) {//defective file and we are not pro 
            file.delete(); 
            return false;
        }
        byte[] signature = Base32.decode(str.substring(0,index));
        String startDate = str.substring(index);//includes the separator
        byte[] plainText = (this.macAddress+startDate).getBytes("UTF-8");
        UpdateMessageVerifier verifier = 
                                new UpdateMessageVerifier(signature,plainText);
        if(!verifier.verifySource()) //not verified
            return false;
        //OK. The File is verified. Check if its still time to be PRO
        long expiryTime = Long.parseLong(startDate.substring(1)) + EXPIRY;
        return System.currentTimeMillis() <= expiryTime;
    }

    /**
     * @exception IOException is thrown if the file the server sent back
     * could not be written
     * TODO1: Make sure IOEx is not thrown for character encoding reasons
     * TODO1: Make sure the locale is converted to us_en before we work on this
     */
    public boolean buyPro(String name, String address, String city, 
                          String state, String zip, String country, 
                          String email, String ccNumber, String expDate)  
    throws IOException, UnsupportedEncodingException {
        FileOutputStream fos =null;
        SSLSocket socket = null;
        try {
            Assert.that(canBuyPro());
            //1. open a secure socket to LimeWire server.
            socket = (SSLSocket)createConnectionToPayServer();
            //Force the socket to do the SSL handshake now.
            socket.startHandshake();
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            //2. Authenticate the server again!
            boolean serverAuthenticated = verifyConnection(os,is);
            System.out.println("Sumeet: verified server "+serverAuthenticated);
            if(!serverAuthenticated)
                return false;
            //3. generate and send info for server  -- including MAC address
            String str = getInfoForServer(name,address,city,state,zip,country,
                                       email, ccNumber, expDate);
            byte[] info = str.getBytes("UTF-8");//this has to be in English
            int size = info.length;//this can be more than 127...we need an int
            byte[] s = {0,0,0,0};
            ByteOrder.int2leb(size,s,0);
            //write the size of data the server should expect
            os.write(s);
            os.flush();
            //write the data.
            os.write(info);
            os.flush();
            //4. Server will send back a signed file, save it.
            File file=new File(CommonUtils.getUserSettingsDir(),"ProFile");
            fos = new FileOutputStream(file);
            int a = 0;
            while(a!=-1) {
                a = is.read();
                if(a!=-1)
                    fos.write(a);
            }
            return true;
        } catch (Exception e) {           
            e.printStackTrace();
            return false;
        } finally {
            //always close the file and the socket
            fos.close();//this will flush it automatically
            socket.close();
        }
    }
    
    ///////////////////////////Helper methods/////////////////////////////

    /**
     * Establishes a connection with the payment server using SSL. For the 
     * server to be authenticated, we distribute a certificate with LimeWire.
     */
    private Socket createConnectionToPayServer() throws 
                                       IOException, GeneralSecurityException {
        //For working with java 1.2 and java 1.3
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        //get the certificate disctibuted with LimeWire. 
        CertificateFactory certFact = CertificateFactory.getInstance("X.509");
        File file = new File(CommonUtils.getUserSettingsDir(),"limecert.cert");
        FileInputStream fis = new FileInputStream(file);

        Certificate cert = certFact.generateCertificate(fis);
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null,null);
        store.setCertificateEntry("lime",cert);        
        TrustManagerFactory tf = 
                          TrustManagerFactory.getInstance("SunX509","SunJSSE");
        tf.init(store);
        //create a SSLContext 
        SSLContext context = SSLContext.getInstance("SSLv3");
        com.sun.net.ssl.TrustManager[] mans = 
                          (com.sun.net.ssl.TrustManager[])tf.getTrustManagers();
        //initialize the SSLContext with a TrustManager loaded with our cert
        context.init(null,mans,null);
        //Get SSLSocketFactory from the context
        SSLSocketFactory factory = context.getSocketFactory();
        //connect to the server
        //return factory.createSocket(InetAddress.getByName("sales.limewire.com"),6002);
        return factory.createSocket(InetAddress.getByName("127.0.0.1"),6002);
    }

    /**
     * Although we have authenticated our server with the certificate shipped 
     * with LimeWire, this method does another layer of server authentication
     * using the key pair we use for updates.
     * The Client sends the server it's clientGUID and the server signs
     * it with it's private key. The client verifies the signature.
     * Now we know we are actually talking to the LimeWire Server.
     */
    private boolean verifyConnection(OutputStream os, InputStream is) 
                                                         throws IOException {
        //Send the server my client guid
        String guid = SettingsManager.instance().getClientID();
        byte[] gBytes = GUID.fromHexString(guid);
        os.write(gBytes);
        os.flush();
        //read the signed guid.
        byte read = -2;
        int size = is.read();
        byte[] bytes = new byte[size];
        is.read(bytes,0,size); 
        //verify the signature
        UpdateMessageVerifier verifier=new UpdateMessageVerifier(bytes,gBytes);
        return  verifier.verifySource();
    }

    /**
     * Formats all the information so that the server can read it, and 
     * process the transaction. 
     * <p>
     * Includes the MAC address as part of the formatted string.
     * @return null if unable to find the MAC address. 
     */
    private String getInfoForServer(String name, String address, String city, 
                          String state, String zip, String country, 
                            String email, String ccNumber, String expDate) {
        StringBuffer sb = new StringBuffer();
        sb.append(START); sb.append("1");sb.append(END);//version server use
        sb.append(START); sb.append(name);sb.append(END);
        sb.append(START); sb.append(address);sb.append(END);
        sb.append(START); sb.append(city);sb.append(END);
        sb.append(START); sb.append(state);sb.append(END);
        sb.append(START); sb.append(zip);sb.append(END);
        sb.append(START); sb.append(country);sb.append(END);
        sb.append(START); sb.append(email);sb.append(END);
        sb.append(START); sb.append(ccNumber);sb.append(END);
        sb.append(START); sb.append(expDate);sb.append(END);
        if(macAddress == null) {
            MacAddressFinder maf = new MacAddressFinder();
            macAddress = maf.getMacAddress();
        }
        if(macAddress == null)//useless we cannot do the transaction
            return null;
        sb.append(START); sb.append(macAddress);sb.append(END);
        return sb.toString();
    }

    ////////////////////////////testing code///////////////////////////////
    
    public static void main(String[] args) {
        try {
            ProManager man = new ProManager();
            //invalid
            //man.buyPro("Sumeet Thadani", "Blah 21 xth  St, 1R ","New York","NY","90210","USA","big@shit.com","4111111111111113","0504");
            //valid
            man.buyPro("Sumeet Thadani", "Blah 21 xth St, 1R ","New York","NY","90210","USA","big@shit.com","4111111111111111","0504");
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Sumeet: test failed");
        }
    }
}
