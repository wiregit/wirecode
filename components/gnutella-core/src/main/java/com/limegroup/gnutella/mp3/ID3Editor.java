package com.limegroup.gnutella.mp3;

import java.io.*;
import java.util.*;

/**
 * Used when a user wants to edit meta-information about a .mp3 file, and asks
 * to save it. For this class to work efficiently, the removeID3Tags method
 * is called before. rewriteID3Tags method is called. 
 *
 * @author Sumeet Thadani
 */

public class ID3Editor{

    private String title_;
    private String artist_;
    private String album_;
    private String year_;
    private String track_;
    private String comment_;
    private String genre_;

    public String removeID3Tags(String xmlStr){
        //title
        int i = xmlStr.indexOf("<title>");
        int j;
        String a="";
        String b="";
        String title="";
        if(i>=0){
            j = xmlStr.indexOf(">",i+8);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            title = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        //artist
        String artist="";
        i=xmlStr.indexOf("<artist>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+9);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            artist = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        //album
        String album="";
        i=xmlStr.indexOf("<album>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+8);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            album = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        //year
        String year="";
        i=xmlStr.indexOf("<year>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+7);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            year = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        //track
        String track="";
        i=xmlStr.indexOf("<track>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+8);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            track = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        //comment
        String comment="";
        i=xmlStr.indexOf("<comments>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+11);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            comment = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        //genre
        String genre="";
        i=xmlStr.indexOf("<genre>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+8);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            genre = xmlStr.substring(i,j);
            xmlStr = a+b;
        }
        String bitrate="";
        i=xmlStr.indexOf("<bitrate>");
        if(i>=0){
            j = xmlStr.indexOf(">",i+10);
            a = xmlStr.substring(0,i);
            b = xmlStr.substring(j+1);
            // the value is thrown away, user can't change bitrate
            xmlStr = a+b;
        }
       

        if(!title.equals(""))
            this.title_ = getInfo(title);
        if(!artist.equals(""))
            this.artist_ = getInfo(artist);
        if(!album.equals(""))
            this.album_ = getInfo(album);
        if(!year.equals(""))
            this.year_ = getInfo(year);
        if(!track.equals(""))
            this.track_ = getInfo(track);
        if(!comment.equals(""))
            this.comment_ = getInfo(comment);
        if(!genre.equals(""))
            this.genre_ = getInfo(genre);
        return xmlStr;//this has been suitable modified
    }
    
    public boolean writeID3DataToDisk(String fileName){
        File f= null;
        RandomAccessFile file = null;
        try{
            f = new File(fileName);
            file = new RandomAccessFile(f,"rw");
        }catch(IOException e){
            return false;
        }
        long length=0;
        try{
            length = file.length();
            if(length < 128)//could not write - file too small
                return true;//since that info was probably not there
            file.seek(length - 128);
        }catch(IOException ee){
            return true;//since that info was probably not there
        }        
        byte[] buffer = new byte[30];//max buffer length...drop/pickup vehicle
        
        //see if there are ID3 Tags in the file
        String tag="";
        try{
            file.readFully(buffer,0,3);
            tag = new String(buffer,0,3, "Cp437");
        }catch (Exception e){
            return true; //since the info was probably not there
        }
        if(!tag.equals("TAG"))
            return true;//since that info was probably not there

        boolean b = toFile(title_,30,file,buffer);
        b = (b && toFile(artist_,30,file,buffer));
        b = (b&& toFile(album_,30,file,buffer));
        b = (b&& toFile(year_,4,file,buffer));
        //comment and track (a little bit tricky)
        b = (b&& toFile(comment_,28,file,buffer));//28 bytes for comment
        try{
            file.write(0);//separator b/w comment and track(track is optional)
            byte trackByte;
            if (track_ == null || track_.equals(""))
                trackByte = (byte)0;
            else
                trackByte = Byte.parseByte(track_);
            file.write(trackByte);
            //genre
            byte genreByte= getGenreByte();
            file.write(genreByte);
            file.close();
        }catch(IOException e){
            b = false;
        }            
        return b;
    }

    private boolean toFile(String val,int maxLen, RandomAccessFile file,
                        byte[] buffer){
        byte[] fromString;
        if(val==null || val.equals("")){
            fromString = new byte[maxLen];
            Arrays.fill(fromString,0,maxLen-1,(byte)0);//fill it all with 0
        }
        else
            fromString = val.getBytes();
        int len = fromString.length;
        if(len < maxLen){
            System.arraycopy(fromString,0,buffer,0,len);
            Arrays.fill(buffer,len,maxLen-1,(byte)0);//fill the rest with 0s
        }
        else//cut off the rest
            System.arraycopy(fromString,0,buffer,0,maxLen);
        try{
            file.write(buffer,0,maxLen);
        }catch (IOException e){
            return false;
        }
        return true;
    }

    private String getInfo(String tag){
        int i = tag.indexOf(">");//end of opening  tag
        int j = tag.indexOf("<",i);//begining of closing tag
        return tag.substring(i+1,j);
    }

    private byte getGenreByte(){
        if(genre_==null) return 0;            
        else if(genre_.equals("Blues")) return 0;
        else if(genre_.equals("Classic Rock")) return 1;
        else if(genre_.equals("Country")) return 2;
        else if(genre_.equals("Dance")) return 3;
        else if(genre_.equals("Disco")) return 4;
        else if(genre_.equals("Funk")) return 5;
        else if(genre_.equals("Grunge")) return 6;
        else if(genre_.equals("Hop")) return 7;
        else if(genre_.equals("Jazz")) return 8;
        else if(genre_.equals("Metal")) return 9;
        else if (genre_.equals("New Age")) return 10;
        else if(genre_.equals("Oldies")) return 11;
        else if(genre_.equals("Other")) return 12;
        else if(genre_.equals("Pop")) return 13;
        else if (genre_.equals("R &amp; B")) return 14;
        else if(genre_.equals("Rap")) return 15;
        else if(genre_.equals("Reggae")) return 16;
        else if(genre_.equals("Rock")) return 17;
        else if(genre_.equals("Techno")) return 17;
        else if(genre_.equals("Industrial")) return 19;
        else if(genre_.equals("Alternative")) return 20;
        else if(genre_.equals("Ska")) return 21;
        else if(genre_.equals("Metal")) return 22;
        else if(genre_.equals("Pranks")) return 23;
        else if(genre_.equals("Soundtrack")) return 24;
        else if(genre_.equals("Euro-Techno")) return 25;
        else if(genre_.equals("Ambient")) return 26;
        else if(genre_.equals("Trip-Hop")) return 27;
        else if(genre_.equals("Vocal")) return 28;
        else if (genre_.equals("Jazz+Funk")) return 29;
        else if(genre_.equals("Fusion")) return 30;
        else if(genre_.equals("Trance")) return 31;
        else if(genre_.equals("Classical")) return 32;
        else if(genre_.equals("Instrumental")) return 33;
        else if(genre_.equals("Acid")) return 34;
        else if(genre_.equals("House")) return 35;
        else if(genre_.equals("Game")) return 36;
        else if(genre_.equals("Sound Clip")) return 37;
        else if(genre_.equals("Gospel")) return 38;
        else if(genre_.equals("Noise")) return 39;
        else if(genre_.equals("AlternRock")) return 40;
        else if(genre_.equals("Bass")) return 41;
        else if(genre_.equals("Soul")) return 42;
        else if(genre_.equals("Punk")) return 43;
        else if(genre_.equals("Space")) return 44;
        else if(genre_.equals("Meditative")) return 45;
        else if(genre_.equals("Instrumental Pop")) return 46;
        else if(genre_.equals("Instrumental Rock")) return 47;
        else if(genre_.equals("Ethnic")) return 48;
        else if(genre_.equals("Gothic")) return 49;
        else if(genre_.equals("Darkwave")) return 50;
        else if(genre_.equals("Techno-Industrial")) return 51;
        else if(genre_.equals("Electronic")) return 52;
        else if(genre_.equals("Pop-Folk")) return 53;
        else if(genre_.equals("Eurodance")) return 54;
        else if(genre_.equals("Dream")) return 55;
        else if(genre_.equals("Southern Rock")) return 56;
        else if(genre_.equals("Comedy")) return 57;
        else if(genre_.equals("Cult")) return 58;
        else if(genre_.equals("Gangsta")) return 59;
        else if(genre_.equals("Top 40")) return 60;
        else if(genre_.equals("Christian Rap")) return 61;
        else if(genre_.equals("Pop/Funk")) return 62;
        else if(genre_.equals("Jungle")) return 63;
        else if(genre_.equals("Native American")) return 64;
        else if(genre_.equals("Cabaret")) return 65;
        else if(genre_.equals("New Wave")) return 66;
        else if(genre_.equals("Psychadelic")) return 67;
        else if(genre_.equals("Rave")) return 68;
        else if(genre_.equals("Showtunes")) return 69;
        else if(genre_.equals("Trailer")) return 70;
        else if(genre_.equals("Lo-Fi")) return 71;
        else if(genre_.equals("Tribal")) return 72;
        else if(genre_.equals("Acid Punk")) return 73;
        else if(genre_.equals("Acid Jazz")) return 74;
        else if(genre_.equals("Polka")) return 75;
        else if(genre_.equals("Retro")) return 76;
        else if(genre_.equals("Musical")) return 77;
        else if(genre_.equals("Rock &amp; Roll")) return 78;
        else if(genre_.equals("Hard Rock")) return 79;
        else if(genre_.equals("Folk")) return 80;
        else if(genre_.equals("Folk-Rock")) return 81;
        else if(genre_.equals("National Folk")) return 82;
        else if(genre_.equals("Swing")) return 83;
        else if(genre_.equals("Fast Fusion")) return 84;
        else if(genre_.equals("Bebob")) return 85;
        else if(genre_.equals("Latin")) return 86;
        else if(genre_.equals("Revival")) return 87;
        else if(genre_.equals("Celtic")) return 88;
        else if(genre_.equals("Bluegrass")) return 89;
        else if(genre_.equals("Avantgarde")) return 90;
        else if(genre_.equals("Gothic Rock")) return 91;
        else if(genre_.equals("Progressive Rock")) return 92;
        else if(genre_.equals("Psychedelic Rock")) return 93;
        else if(genre_.equals("Symphonic Rock")) return 94;
        else if(genre_.equals("Slow Rock")) return 95;
        else if(genre_.equals("Big Band")) return 96;
        else if(genre_.equals("Chorus")) return 97;
        else if(genre_.equals("Easy Listening")) return 98;
        else if(genre_.equals("Acoustic")) return 99;
        else if(genre_.equals("Humour")) return 100;
        else if(genre_.equals("Speech")) return 101;
        else if(genre_.equals("Chanson")) return 102;
        else if(genre_.equals("Opera")) return 103;
        else if(genre_.equals("Chamber Music")) return 104;
        else if(genre_.equals("Sonata")) return 105;
        else if(genre_.equals("Symphony")) return 106;
        else if(genre_.equals("Booty Bass")) return 107;
        else if(genre_.equals("Primus")) return 108;
        else if(genre_.equals("Porn Groove")) return 109;
        else if(genre_.equals("Satire")) return 110;
        else if(genre_.equals("Slow Jam")) return 111;
        else if(genre_.equals("Club")) return 112;
        else if(genre_.equals("Tango")) return 113;
        else if(genre_.equals("Samba")) return 114;
        else if(genre_.equals("Folklore")) return 115;
        else if(genre_.equals("Ballad")) return 116;
        else if(genre_.equals("Power Ballad")) return 117;
        else if(genre_.equals("Rhythmic Soul")) return 118;
        else if(genre_.equals("Freestyle")) return 119;
        else if(genre_.equals("Duet")) return 120;
        else if(genre_.equals("Punk Rock")) return 121;
        else if(genre_.equals("Drum Solo")) return 122;
        else if(genre_.equals("A capella")) return 123;
        else if(genre_.equals("Euro-House")) return 124;
        else if(genre_.equals("Dance Hall")) return 125;
        else return 0;
        }



}
