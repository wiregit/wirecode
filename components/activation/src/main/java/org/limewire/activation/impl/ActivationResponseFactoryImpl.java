package org.limewire.activation.impl;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.limewire.activation.api.ActivationItem;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import com.google.inject.Singleton;
import com.google.inject.Inject;

@Singleton
class ActivationResponseFactoryImpl implements ActivationResponseFactory {
    private static Log LOG = LogFactory.getLog(ActivationResponseFactoryImpl.class);
    
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
    private final ActivationItemFactory activationItemFactory;
    
    @Inject
    public ActivationResponseFactoryImpl(ActivationItemFactory activationItemFactory) {
        this.activationItemFactory = activationItemFactory;
    }

    @Override
    public ActivationResponse createFromJson(String json) throws InvalidDataException {
        return createJson(json, false);
    }

    @Override
    public ActivationResponse createFromDiskJson(String json) throws InvalidDataException {
        return createJson(json, true);
    }
    
    @Override
    public ActivationResponse createErrorResponse(ActivationResponse.Type error) {
        return new ActivationResponse(null, null, error, null, 0, Collections.<ActivationItem>emptyList(), null);
    }
    
    private ActivationResponse createJson(String json, boolean loadedFromDisk) throws InvalidDataException {
        
        JSONTokener tokener = new JSONTokener(json);
        try {
            JSONObject parentObj = new JSONObject(tokener);
            
            // parse values which are in all responses
            String response = parentObj.getString("response");
            String lid = parentObj.getString("lid");
            ActivationResponse.Type type = ActivationResponse.Type.valueOf(response.toUpperCase());
            String mcode = parentObj.optString("mcode", null);
            int refresh =  parentObj.optInt("refresh", 0);
            String optionalMessage = null;
            if (type == ActivationResponse.Type.ERROR) {
                optionalMessage = parentObj.getString("message");
            }
            LOG.debugf("Parsed response from json input, type: {0}", type);
            List<ActivationItem> items = parseActivationItems(parentObj, loadedFromDisk);
            return new ActivationResponse(json, lid, type, mcode, refresh, items, optionalMessage);
        } catch (JSONException e) {
            throw new InvalidDataException("Error parsing JSON String " + json, e);
        } catch (ParseException e) {
            throw new InvalidDataException("Error parsing JSON String " + json, e);
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Error parsing JSON " +
                    "String due to unrecognized response type: " + json, e);
        }
    }

    private List<ActivationItem> parseActivationItems(JSONObject parentObj,
                                                      boolean loadedFromDisk) throws ParseException, JSONException {
        if (!parentObj.has("modules")) {
            return Collections.emptyList();    
        }
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        JSONArray array = parentObj.getJSONArray("modules");
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            int moduleId = Integer.parseInt(obj.getString("id"));
            String moduleName = obj.getString("name");
            String purchaseDate = obj.getString("pur");
            String expDate = obj.getString("exp");
            ActivationItem.Status status = ActivationItem.Status.valueOf(obj.getString("status").toUpperCase());

            Date pur = formatter.parse(purchaseDate);
            Date exp = formatter.parse(expDate);
            ActivationItem item;
            if (loadedFromDisk) {
                item = activationItemFactory.createActivationItemFromDisk(moduleId, moduleName, pur, exp, status);
            } else {
                item = activationItemFactory.createActivationItem(moduleId, moduleName, pur, exp, status);
            }
            items.add(item);
        }
        return items;
    }
}
