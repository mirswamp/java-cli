package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.Platform;
import org.continuousassurance.swamp.api.PlatformVersion;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;

import edu.uiuc.ncsa.security.core.exceptions.NotImplementedException;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  2:18 PM
 */
public class PlatformVersionHandler<T extends PlatformVersion> extends AbstractHandler<T> {
    public PlatformVersionHandler(Session session) {
        super(session);
    }

    @Override
    public List<T> getAll() {
        throw new NotImplementedException();
    }

    public List<T> getAll(Platform platform) {
        String url = createURL("platforms/" + platform.getIdentifierString() + "/versions");
              MyResponse mr = getClient().rawGet(url, null);
              ArrayList<T> platform_versions = new ArrayList<>();
              for (int i = 0; i < mr.jsonArray.size(); i++) {
                  JSONObject json = mr.jsonArray.getJSONObject(i);
                  T platform_version = fromJSON(json);
                  platform_version.setPlatform(platform);
                  platform_version.standardize();
                  platform_versions.add(platform_version);
              }
              return platform_versions;
    }

    protected T fromJSON(JSONObject json) {
        T platform_version = (T) new PlatformVersion(getSession());
        
        String[] sAttrib = {PlatformVersion.NAME_KEY, PlatformVersion.VERSION_STRING};
        String[] uAttrib = {PlatformVersion.PLATFORM_VERSION_UUID_KEY, PlatformVersion.PLATFORM_UUID_KEY};
        
        ConversionMapImpl map = new ConversionMapImpl();
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        
        
        platform_version.setConversionMap(map);
        return platform_version;
    }

    public PlatformVersion find(String name){
        for(PlatformVersion p: getAll()){
            if(p.getName().equals(name)){
                return p;
            }
        }
        return null;
    }

    @Override
    public String getURL() {
        return createURL("platforms");
    }
}
