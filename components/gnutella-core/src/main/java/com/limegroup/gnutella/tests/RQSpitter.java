package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.*;

public class RQSpitter extends Thread{
    RouterService service;
    
    //constructor
    public RQSpitter(RouterService s){
        this.service = s;
    }

    public void run(){
        String query = "";
        String richQuery = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio title=\"Paint it black\" artist=\"Rolling Stones\" genre=\"Classic Rock\"></audio></audios>";        
        for(int i=0;i<1000;i++){
            service.query(query,richQuery,0);
        }
    }
}


