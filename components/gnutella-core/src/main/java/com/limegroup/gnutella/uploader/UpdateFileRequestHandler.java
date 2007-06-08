package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.BasicHeaderProcessor;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.UserAgentHeaderInterceptor;
import com.limegroup.gnutella.statistics.UploadStat;

/**
 * Responds to requests for the update.xml file.
 */
public class UpdateFileRequestHandler implements HttpRequestHandler {
    
    private HTTPUploadSessionManager sessionManager;

    public UpdateFileRequestHandler(HTTPUploadSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        UploadStat.UPDATE_FILE.incrementStat();
        
        HTTPUploader uploader = sessionManager.getOrCreateUploader(request, context,
                UploadType.UPDATE_FILE, "Update-File Request");

        // check for free loader
        BasicHeaderProcessor processor = new BasicHeaderProcessor();
        processor.addInterceptor(new UserAgentHeaderInterceptor(
                uploader));
        processor.process(request, context);
        if (UserAgentHeaderInterceptor.isFreeloader(uploader
                .getUserAgent())) {
            sessionManager.handleFreeLoader(request, response, context, uploader);
        } else {
            // set file entity
            File file = new File(CommonUtils.getUserSettingsDir(),
                    "update.xml");
            uploader.setFile(file);
            uploader.setState(Uploader.UPDATE_FILE);

            response.setEntity(new FileResponseEntity(uploader));
        }

        sessionManager.sendResponse(uploader, response);
    }


}
