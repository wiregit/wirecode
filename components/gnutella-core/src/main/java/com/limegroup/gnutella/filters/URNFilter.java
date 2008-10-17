package com.limegroup.gnutella.filters;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
public class URNFilter implements SpamFilter {
    
    private static final Log LOG = LogFactory.getLog(URNFilter.class);
    private static final HashSet<URN> blacklist = new HashSet<URN>();
    // TODO: make this into a SIMPP setting
    private static final String[] badURNs = new String[]{
        "urn:sha1:4IUO6VHPEG65C3PMNRW5JAZHXOJSICFG",
        "urn:sha1:52KRXO3KNVWTPTPMWMNBB2XVAPFYTYF3",
        "urn:sha1:5T6HGYBCEXZMQ2NCAITILE5SLBMX7IZH",
        "urn:sha1:6FPIGULR3OMOMOESYFELHS2YVV2M7ZGF",
        "urn:sha1:AN5N6Z6CRLK6KM46MINPNEPRBGE3YYOA",
        "urn:sha1:ATY26E5YGCPHOEUNWAIVVNO4SLXEKDQS",
        "urn:sha1:BAYURWNMQAF5DGVPMRQRLRIZ77R3AGDJ",
        "urn:sha1:FZMBTBIMI7BMTNSDHYAQTVL4NR2AZXRS",
        "urn:sha1:GOZG2MFW5XWQHWRIFS62FWVSPDD7YUFK",
        "urn:sha1:HVIXE33CM7J6IXTWFSD43QGGDL566QEU",
        "urn:sha1:JYMLNADQONAK3MUH7EQM57ADRE6CPRZA",
        "urn:sha1:KHXDJGPNSEXFP6HKXF5UMF23JGC2CYUK",
        "urn:sha1:MF2PHNLWS5Q4CGJ35HT4DNLIHEN5GN7O",
        "urn:sha1:MFJJ3GVPQKKY7ZIELFOK2RJ2Z53GJ74F",
        "urn:sha1:OIFR7TOZMF7ZTJGUWIZCIIAHQE4AUG2K",
        "urn:sha1:P53HONQTHF4LIWKWQZTD5RESX7DWPJUU",
        "urn:sha1:PLKMRUZ7COPB7G2XQP2CAKR2TVCIBDJC",
        "urn:sha1:PZB2T2DCTUUP6WGLW5RHBVVFZDLGJHAE",
        "urn:sha1:QYWDCNFLK4B43TCL72V4HUS3YRT4YZKI",
        "urn:sha1:UO3FR5RVVAWDCOV7B5WEUAANTDLM65CT",
        "urn:sha1:VHKHYERJO5QE7QER74MUKQM5NM4X7L2K",
        "urn:sha1:XK3GHUI2ZHMQQH65TKPSTMBHUL5SU4ED",
        "urn:sha1:Y64ZHRNFAYZEOZ2BMGCWY7DV2ZL2HWY4"
    };
    
    public URNFilter() {
        try {
            for(String s : badURNs)
                blacklist.add(URN.createSHA1Urn(s));
        } catch (Exception x) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error creating URN blacklist: " + x);
        }
    }
    
    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            try {
                for(Response r : q.getResultsArray()) {
                    for(URN u : r.getUrns()) {
                        if(blacklist.contains(u)) {
                            if(LOG.isDebugEnabled())
                                LOG.debug("Filtering response with URN " + u);
                            return false;
                        }
                    }
                }
                return true;
            } catch (BadPacketException bpe) {
                return true;
            }
        } else if(m instanceof QueryRequest) {
            QueryRequest q = (QueryRequest)m;
            for(URN u : q.getQueryUrns()) {
                if(blacklist.contains(u)) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Filtering request with URN " + u);
                    return false;
                }
            }
            return true;
        }
        return true; // Don't block other kinds of messages
    }
}