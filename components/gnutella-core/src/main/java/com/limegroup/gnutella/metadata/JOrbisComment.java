/* JOrbisComment -- pure Java Ogg Vorbis Comment Editor
 *
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 *
 * Written by: 2000 ymnk<ymnk@jcaft.com>
 *
 * Many thanks to 
 *   Monty <monty@xiph.org> and 
 *   The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec and
 * JOrbisPlayer depends on JOrbis.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/**
 * modified heavily to not be standalane program for
 * inlusion in the limewire code.
 */
package com.limegroup.gnutella.metadata;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.limewire.util.FileUtils;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.Info;


public class JOrbisComment {
	
 private static final	Log LOG = LogFactory.getLog(JOrbisComment.class);
	
  private State state=null;

  
  
  /**
   * updates the given ogg file with the new Comment field
   * @param comment the <tt>com.jcraft.jorbis.Comment</tt> object to 
   * put in the file
   * @param file the .ogg file to be updated
   */
  public void update(Comment comment, File file) throws IOException{
  	InputStream in =null;
  	OutputStream out = null;
  	File tempFile = null;
  	
  	try {
  		state =new State();
    	in =new BufferedInputStream(new FileInputStream(file));
    
    	read(in);
    
    	//update the comment
    	state.vc=comment;
    
    	//copy the newly created file in a temp folder
    	tempFile=null;
    
    	try {
    		tempFile = FileUtils.createTempFile(file.getName(),"tmp");
    	}catch(IOException e) {
    		//sometimes either the temp path is messed up or
    		//	there isn't enough space on that partition.
    		//try to create a temp file on the same folder as the
    		//original.  It will not be around long enough to get shared
    	
    		//if an exception is thrown, let it propagate
    		LOG.debug("couldn't create temp file in $TEMP, trying elsewhere");
    	
    		tempFile = new File(file.getAbsolutePath(),
				file.getName()+".tmp");
    	}
    	out=new BufferedOutputStream(new FileOutputStream(tempFile));
    
    
    	LOG.debug("about to write ogg file");
    
    	write(out);
    
    	out.flush();
    	
    }finally {
  		if (out!=null)
  		try {out.close(); }catch(IOException ignored){}
  		if (in!=null)
  	  		try {in.close(); }catch(IOException ignored){}
  	}
    
	if (tempFile.length() == 0)
	    throw new IOException("writing of file failed");
	
	//rename fails on some rare filesystem setups
	if (!FileUtils.forceRename(tempFile,file))
		//something's seriously wrong
		throw new IOException("couldn't rename file");
    
    
  }

  private static int CHUNKSIZE=4096;


  void read(InputStream in) throws IOException{
    state.in=in;

    Page og=new Page();

    int index;
    byte[] buffer;
    int bytes=0;

    state.oy=new SyncState();
    state.oy.init();
    
    index=state.oy.buffer(CHUNKSIZE);
    buffer=state.oy.data;
    bytes=state.in.read(buffer, index, CHUNKSIZE); 
    
    state.oy.wrote(bytes);
    
    if(state.oy.pageout(og)!=1)
    	throw new IOException("input truncated, empty or not an ogg");
    
    state.serial=og.serialno();
    state.os= new StreamState();
    state.os.init(state.serial);
//  os.reset();

    state.vi=new Info();
    state.vi.init();

    state.vc=new Comment();
    state.vc.init();

    if(state.os.pagein(og)<0) 
     throw new IOException ("Error reading first page of Ogg bitstream data.");
      

    Packet header_main = new Packet();

    if(state.os.packetout(header_main)!=1)
    	throw new IOException("Error reading initial header packet.");


    if(state.vi.synthesis_headerin(state.vc, header_main)<0) 
      throw new IOException("This Ogg bitstream does not contain Vorbis data.");


    state.mainlen=header_main.bytes;
    state.mainbuf=new byte[state.mainlen];
    System.arraycopy(header_main.packet_base, header_main.packet, 
		     state.mainbuf, 0, state.mainlen);

    int i=0;
    Packet header;
    Packet header_comments=new Packet();
    Packet header_codebooks=new Packet();

    header=header_comments;
    while(i<2) {
      while(i<2) {
        int result = state.oy.pageout(og);
  	if(result == 0) break; /* Too little data so far */
   	else if(result == 1){
          state.os.pagein(og);
          while(i<2){
	    result = state.os.packetout(header);
	    if(result == 0) break;
   	    if(result == -1)
	      throw new IOException("Corrupt secondary header.");

            state.vi.synthesis_headerin(state.vc, header);
	    if(i==1) {
	      state.booklen=header.bytes;
	      state.bookbuf=new byte[state.booklen];
              System.arraycopy(header.packet_base, header.packet, 
			       state.bookbuf, 0, header.bytes);
	    }
	    i++;
  	    header = header_codebooks;
	  }
        }
      }

      index=state.oy.buffer(CHUNKSIZE);
      buffer=state.oy.data; 
      bytes=state.in.read(buffer, index, CHUNKSIZE); 
      

      if(bytes == 0 && i < 2)
        throw new IOException("EOF before end of vorbis headers.");

      state.oy.wrote(bytes);
    }

    //System.out.println(state.vi);
  }

  int write(OutputStream out) throws IOException{
    StreamState streamout=new StreamState();
    Packet header_main=new Packet();
    Packet header_comments=new Packet();
    Packet header_codebooks=new Packet();

    Page ogout=new Page();

    Packet op=new Packet();
    long granpos = 0;

    int result;

    int index;
    byte[] buffer;

    int bytes, eosin=0;
    int needflush=0, needout=0;

    header_main.bytes = state.mainlen;
    header_main.packet_base= state.mainbuf;
    header_main.packet = 0;
    header_main.b_o_s = 1;
    header_main.e_o_s = 0;
    header_main.granulepos = 0;

    header_codebooks.bytes = state.booklen;
    header_codebooks.packet_base = state.bookbuf;
    header_codebooks.packet = 0;
    header_codebooks.b_o_s = 0;
    header_codebooks.e_o_s = 0;
    header_codebooks.granulepos = 0;

    streamout.init(state.serial);

    state.vc.header_out(header_comments);

    streamout.packetin(header_main);
    streamout.packetin(header_comments);
    streamout.packetin(header_codebooks);

//System.out.println("%1");

    while((result=streamout.flush(ogout))!=0){
//System.out.println("result="+result);
      
        out.write(ogout.header_base, ogout.header, ogout.header_len);
        out.flush();
      
      
        out.write(ogout.body_base, ogout.body,ogout.body_len);
        out.flush();
    }

//System.out.println("%2");

    while(state.fetch_next_packet(op)!=0){
      int size=state.blocksize(op);
      granpos+=size;
//System.out.println("#1");
      if(needflush!=0){ 
//System.out.println("##1");
        if(streamout.flush(ogout)!=0){
          
            out.write(ogout.header_base,ogout.header,ogout.header_len);
            out.flush();
          
          
            out.write(ogout.body_base,ogout.body,ogout.body_len);
            out.flush();
          
        }
      }
//System.out.println("%2 eosin="+eosin);
      else if(needout!=0){
//System.out.println("##2");
        if(streamout.pageout(ogout)!=0){
       
            out.write(ogout.header_base,ogout.header,ogout.header_len);
            out.flush();
       
       
            out.write(ogout.body_base,ogout.body,ogout.body_len);
            out.flush();
       
        }
      }

//System.out.println("#2");

      needflush=needout=0;

      if(op.granulepos==-1){
        op.granulepos=granpos;
        streamout.packetin(op);
      }
      else{
        if(granpos>op.granulepos){
          granpos=op.granulepos;
          streamout.packetin(op);
          needflush=1;
	}
        else{
          streamout.packetin(op);
          needout=1;
	}
      }
//System.out.println("#3");
    }

//System.out.println("%3");

    streamout.e_o_s=1;
    while(streamout.flush(ogout)!=0){
      
        out.write(ogout.header_base,ogout.header,ogout.header_len);
        out.flush();
      
      
        out.write(ogout.body_base,ogout.body,ogout.body_len);
        out.flush();
      
    }

//System.out.println("%4");

    state.vi.clear();
//System.out.println("%3 eosin="+eosin);

//System.out.println("%5");

    eosin=0; /* clear it, because not all paths to here do */
    while(eosin==0){ /* We reached eos, not eof */
      /* We copy the rest of the stream (other logical streams)
	 * through, a page at a time. */
      while(true){
        result=state.oy.pageout(ogout);
//System.out.println(" result4="+result);
	if(result==0) break;
	if(result<0){
	  if (LOG.isDebugEnabled()) 
	  	LOG.debug("Corrupt or missing data, continuing...");
	}
	else{
          /* Don't bother going through the rest, we can just 
           * write the page out now */
      
            out.write(ogout.header_base,ogout.header,ogout.header_len);
            out.flush();
	  
      
            out.write(ogout.body_base,ogout.body,ogout.body_len);
            out.flush();
	  
	}
      }

      index=state.oy.buffer(CHUNKSIZE);
      buffer=state.oy.data;
      bytes=state.in.read(buffer, index, CHUNKSIZE); 
      
      
//System.out.println("bytes="+bytes);
      state.oy.wrote(bytes);

      if(bytes == 0 || bytes==-1) {
        eosin = 1;
	break;
      }
    }

    /*
cleanup:
	ogg_stream_clear(&streamout);
	ogg_packet_clear(&header_comments);

	free(state->mainbuf);
	free(state->bookbuf);

	jorbiscomment_clear_internals(state);
	if(!eosin)
	{
		state->lasterror =  
			"Error writing stream to output. "
			"Output stream may be corrupted or truncated.";
		return -1;
	}

	return 0;
       }
    */
    return 0;
  }
  
  class State{
    private final int CHUNKSIZE=4096;
    SyncState oy;
    StreamState os;
    Comment vc;
    Info vi;

    InputStream in;
    int  serial;
    byte[] mainbuf;
    byte[] bookbuf;
    int mainlen;
    int booklen;
    String lasterror;

    int prevW;

    int blocksize(Packet p){
      int _this = vi.blocksize(p);
      int ret = (_this + prevW)/4;

      if(prevW==0){
        prevW=_this;
	return 0;
      }

      prevW = _this;
      return ret;
    }

    Page og=new Page();
    int fetch_next_packet(Packet p){
      int result;
      byte[] buffer;
      int index;
      int bytes;

      result = os.packetout(p);

      if(result > 0){
	return 1;
      }

      while(oy.pageout(og) <= 0){
        index=oy.buffer(CHUNKSIZE);
        buffer=oy.data; 
        try{ bytes=in.read(buffer, index, CHUNKSIZE); }
        catch(Exception e){
          ErrorService.error(e);
          return 0;
        }
        if(bytes>0)
          oy.wrote(bytes);
	if(bytes==0 || bytes==-1) {
          return 0;
	}
      }
      os.pagein(og);

      return fetch_next_packet(p);
    }
}

}