package org.limewire.activation.impl;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.limewire.activation.api.ActivationItem;
import org.limewire.io.InvalidDataException;
import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import com.google.inject.Singleton;
import com.google.inject.Inject;

@Singleton
public class ActivationResponseFactoryImpl implements ActivationResponseFactory {
    
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
    
    private final ActivationItemFactory activationItemFactory;
    
    @Inject
    public ActivationResponseFactoryImpl(ActivationItemFactory activationItemFactory) {
        this.activationItemFactory = activationItemFactory;    
    }
    
    @Override
    public ActivationResponse createFromJson(String json) throws InvalidDataException {
        
        JSONTokener tokener = new JSONTokener(json);
        try {
            JSONObject parentObj = new JSONObject(tokener);
            String lid = parentObj.getString("lid");
            String response = parentObj.getString("response");
            String mcode = parentObj.getString("mcode");
            int refresh = parentObj.getInt("refresh");
            List<ActivationItem> items = new ArrayList<ActivationItem>();
            
            JSONArray array = parentObj.getJSONArray("modules");
            for (int i=0; i<array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int moduleId = Integer.parseInt(obj.getString("id"));
                String moduleName = obj.getString("name");
                String purchaseDate = obj.getString("pur");
                String expDate = obj.getString("exp");
                ActivationItem.Status status = ActivationItem.Status.valueOf(obj.getString("status").toUpperCase());
                
                Date pur = formatter.parse(purchaseDate);
                Date exp = formatter.parse(expDate);
                ActivationItem item = activationItemFactory.createActivationItem(moduleId, 
                    moduleName, pur, exp, status);
                items.add(item);
            }
            return new ActivationResponse(json, lid, response, mcode, refresh, items);
        } catch (JSONException e) {
            throw new InvalidDataException("Error parsing JSON String " + json, e);
        } catch (ParseException e) {
            throw new InvalidDataException("Invalid date in JSON String " + json, e);
        } catch (Throwable e) {
            throw new InvalidDataException("Error parsing JSON String " + json, e);
        }
    }
}
