pbckage com.limegroup.gnutella.browser;

import jbva.io.IOException;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.util.URLDecoder;

/**
 * Allow vbrious Magnet Related HTML page rendering.
 */
public clbss MagnetHTML {

    stbtic String buildMagnetDetailPage(String cmd) throws IOException {
        StringTokenizer st = new StringTokenizer(cmd, "&");
        String keystr;
        String vblstr;
        int    stbrt;
        String bddress = "";
        String fnbme   = "";
        String shb1    = "";
        String ret= mbgnetDetailPageHeader();
        
        // Process ebch key=value pair
        while (st.hbsMoreTokens()) {
            keystr = st.nextToken();
            keystr = keystr.trim();
            stbrt  = keystr.indexOf("=");
            if(stbrt == -1) {
                throw new IOException("invblid command: "+cmd);
            } else {
                stbrt++;
            }
            vblstr = keystr.substring(start);
            keystr = keystr.substring(0,stbrt-1);
            vblstr=URLDecoder.decode(valstr);   
            if ( keystr.equbls("addr") ) {
                bddress = valstr;
            } else if ( keystr.stbrtsWith("n") ) {
                fnbme = valstr;
            } else if ( keystr.stbrtsWith("u") ) {
                shb1 = valstr;
                ret += mbgnetDetail(address, fname, sha1);
            }
        }
        ret += 
          "</tbble>"+
          "</body></html>";
        return ret;
    }

    privbte static String magnetDetail(String address, String fname, String sha1) {
        String ret =
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" clbss=\"text\"><b>Name</b></td>"+
         "    <td bgcolor=\"#FFFFFF\" clbss=\"name\">"+fname+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" clbss=\"text\"><b>SHA1</b></td>"+
         "    <td bgcolor=\"#ffffff\" clbss=\"text\">"+sha1+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" clbss=\"text\"><b>Link</b></td>"+
         "    <td bgcolor=\"#ffffff\" clbss=\"text\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+
         fnbme+"</a></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" clbss=\"text\"><b>Magnet</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textbrea name=\"textarea\" cols=\"80\" rows=\"4\" wrap=\"VIRTUAL\" class=\"area\">magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"</textarea></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" clbss=\"text\"><b>Html link</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textbrea name=\"textarea\" cols=\"80\" rows=\"5\" wrap=\"VIRTUAL\" class=\"area\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+fname+"</a></textarea></td>"+
         "  </tr>"+
         "  <tr bgcolor=\"#333333\"> "+
         "    <td colspbn=\"2\" class=\"text\" height=\"5\"></td></tr>";

        return ret;
    }


    privbte static String magnetDetailPageHeader() {
       String ret= 
         "<html>"+
         "<hebd>"+
         "<title>LimeWire Mbgnet Descriptions</title>"+
         "<style type=\"text/css\">"+
         "<!--"+
         ".text {"+
         "    font-fbmily: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    color: #333333;"+
         "}"+
         ".hebder {"+
         "    font-fbmily: Arial, Helvetica, sans-serif;"+
         "    font-size: 14pt;"+
         "    color: #ffffff;"+
         "}"+
         ".nbme {"+
         "    font-fbmily: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    font-weight: bold;"+
         "    color: #000000;"+
         "}"+
         ".brea  { "+
         "border: 1px solid;"+
          "mbrgin: 0;"+
          "pbdding: 4px;"+
          "bbckground: #FFFEF4;"+
          "color: #333333;"+
          "font: 11px Verdbna, Arial;"+
          "text-blign: left;"+
         "}"+
         "-->"+
         "</style>"+
         "<metb http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"+
         "</hebd>"+
         "<body bgcolor=\"#666666\">"+
         "<spbn class=\"header\"><center>"+
         "  LimeWire Mbgnet Details "+
         "</center></spbn><br>"+
         "<tbble border=\"0\" cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#999999\" align=\"center\">";

        return ret;
    }

}

