package org.continuousassurance.swamp.session.handlers;

import edu.uiuc.ncsa.security.core.Identifier;
import net.sf.json.JSONObject;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.PackageVersion;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.util.HandlerFactoryUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/20/14 at  5:34 PM
 */
public class PackageHandler<T extends PackageThing> extends AbstractHandler<T> {


    public static final String PACKAGE_UUID_KEY = "package_uuid";
    public static final String PACKAGE_OWNER_UUID_KEY = "package_owner_uuid";
    public static final String PACKAGE_NAME_KEY = "name";
    public static final String PACKAGE_DESCRIPTION_KEY = "description";
    public static final String PACKAGE_TYPE_KEY = "package_type";
    public static final String PACKAGE_LANGUAGE_KEY = "package_language";
    public static final String PACKAGE_TYPE_ID_KEY = "package_type_id";
    public static final String PACKAGE_SHARING_STATUS_KEY = "package_sharing_status";
    public static final String EXTERNAL_URI_KEY = "external_uri";
    public static final String CREATE_DATE_KEY = "create_date";
    public static final String UPDATE_DATE_KEY = "update_date";
    public static final String IS_OWNED_KEY = "is_owned";
    public static final String VERSION_STRINGS = "version_strings";

    public static final int PACKAGE_TYPE_C_SOURCE = 1;
    public static final int PACKAGE_TYPE_JAVA_SOURCE = 2;
    public static final int PACKAGE_TYPE_JAVA_BYTECODE = 3;
    public static final int PACKAGE_TYPE_PYTHON2 = 4;
    public static final int PACKAGE_TYPE_PYTHON3 = 5;
    public static final String PACKAGE_SHARING_STATUS_PRIVATE = "private";
    public static final String PACKAGE_SHARING_STATUS_PUBLIC = "public";
    public static final String PACKAGE_SHARING_STATUS_SHARED = "shared";


    public PackageHandler(Session session) {
        super(session);
    }

    public static final String ENDPOINT_LIST = "packages/users/";
    // public static final String ENDPOINT_LIST = "packages/";


    public PackageThing create(String name,
                               String description,
                               int type) {
        return create(name, description, null, type, null);
    }

    public PackageThing create(String name,
                               String description,
                               String externalUri,
                               int type,
                               String pkg_lang) {
        ConversionMapImpl map = new ConversionMapImpl();
        map.put(PACKAGE_OWNER_UUID_KEY, getSession().getUserUID());
        // map.put(PACKAGE_OWNER_UUID_KEY, SWAMPIdentifiers.IDENTIFIER_CAPUT + "deadbeef-cafe-cafe-cafe-deadbeefdeadbeef");
        map.put(PACKAGE_SHARING_STATUS_KEY, PACKAGE_SHARING_STATUS_PRIVATE);
        map.put(PACKAGE_NAME_KEY, name);
        if (externalUri != null) {
            map.put(EXTERNAL_URI_KEY, externalUri);
        }
        map.put(PACKAGE_DESCRIPTION_KEY, description);
        map.put(PACKAGE_TYPE_ID_KEY, Integer.toString(type));
        if (pkg_lang != null) {
        	map.put(PACKAGE_LANGUAGE_KEY, pkg_lang.toLowerCase());
        }
        
        return (PackageThing) super.create(map);
    }

    public PackageThing create(String name,
    		String description,
    		String externalUri,
    		int type) {
    	ConversionMapImpl map = new ConversionMapImpl();
    	map.put(PACKAGE_OWNER_UUID_KEY, getSession().getUserUID());
    	// map.put(PACKAGE_OWNER_UUID_KEY, SWAMPIdentifiers.IDENTIFIER_CAPUT + "deadbeef-cafe-cafe-cafe-deadbeefdeadbeef");
    	map.put(PACKAGE_SHARING_STATUS_KEY, PACKAGE_SHARING_STATUS_PRIVATE);
    	map.put(PACKAGE_NAME_KEY, name);
    	if (externalUri != null) {
    		map.put(EXTERNAL_URI_KEY, externalUri);
    	}
    	map.put(PACKAGE_DESCRIPTION_KEY, description);
    	map.put(PACKAGE_TYPE_ID_KEY, Integer.toString(type));


    	return (PackageThing) super.create(map);
    }


    protected T fromJSON(JSONObject json) {
        T packageThing = (T) new PackageThing(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {PACKAGE_NAME_KEY, PACKAGE_DESCRIPTION_KEY, PACKAGE_TYPE_KEY, PACKAGE_TYPE_ID_KEY, PACKAGE_SHARING_STATUS_KEY, EXTERNAL_URI_KEY};
        String[] uAttrib = {PACKAGE_UUID_KEY};
        String[] bAttrib = {IS_OWNED_KEY};
        String[] dAttrib = {CREATE_DATE_KEY, UPDATE_DATE_KEY};
        String[] aAttrib = {VERSION_STRINGS};
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, bAttrib, json, DATA_TYPE_BOOLEAN);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        setAttributes(map, aAttrib, json, DATA_TYPE_ARRAY);
        packageThing.setConversionMap(map);
        return packageThing;
    }

    public List<T> getAll() {
        MyResponse mr = null;
        mr = getClient().rawGet(createURL(ENDPOINT_LIST + getSession().getUserUID()), null);
        ArrayList<T> pkgs = new ArrayList<>();
        // For packages, the first call gets all the packages and individual calls to a specific url get
        // the rest of the information.
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            //String uuid = json.getString(PACKAGE_UUID_KEY);
            //String url = createURL("packages/" + uuid);
            //MyResponse mr2 = getClient().rawGet(url, null);
            //pkgs.add(fromJSON(mr2.json));
            pkgs.add(fromJSON(json));
        }
        return pkgs;
    }

    @Override
    public T get(Identifier identifier) {
        T t = super.get(identifier);
        List<PackageVersion> versions = (List<PackageVersion>) HandlerFactoryUtil.getPackageVersionH().getAll(t);
        t.setVersions(versions);
        return t;
    }

    /**
     * Return all the packages associated with a project.
     * @param project
     * @return
     */
    public List<T> getAll(Project project) {
        MyResponse mr = null;
        mr = getClient().rawGet(createURL("packages/protected/"+ project.getUUIDString()), null);
        ArrayList<T> pkgs = new ArrayList<>();
        // For packages, the first call gets all the packages and individual calls to a specific url get
        // the rest of the information.
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            //String uuid = json.getString(PACKAGE_UUID_KEY);
            //String url = createURL("packages/" + uuid);
            //MyResponse mr2 = getClient().rawGet(url, null);
            //pkgs.add(fromJSON(mr2.json));
            pkgs.add(fromJSON(json));
        }
        return pkgs;
    }

    
    @Override
    public String getURL() {
        return createURL("packages");
    }

    /*
    public List<String> getTypes() {
        String url = createURL("packages/types");
        MyResponse mr = getClient().rawGet(url, null);

        List<String> pkg_types = new ArrayList<String>();
        for (int i = 0; i < mr.jsonArray.size(); ++i){
        	pkg_types.add(mr.jsonArray.getJSONObject(i).getString("name"));
        }
        return pkg_types;
    }*/

    public Map<String, Integer> getTypes() {
    	String url = createURL("packages/types");
    	MyResponse mr = getClient().rawGet(url, null);

    	Map<String, Integer> pkg_types = new HashMap<String, Integer>();
    	for (int i = 0; i < mr.jsonArray.size(); ++i){
    		pkg_types.put(mr.jsonArray.getJSONObject(i).getString("name"),
    				Integer.valueOf(mr.jsonArray.getJSONObject(i).getInt("package_type_id")));
    	}
    	return pkg_types;
    }

    /*
     * Input: Package Type name
     * Output: Returns a platform UUID
     * 
     * */
    public String getDefaultPlatform(String pkg_type) {
        String url = createURL("packages/types");
        MyResponse mr = getClient().rawGet(url, null);
        String default_platform_uuid = null;
        
        
        for (int i = 0; i < mr.jsonArray.size(); ++i){
        	if (pkg_type.equals(mr.jsonArray.getJSONObject(i).getString("name"))){
        		default_platform_uuid = mr.jsonArray.getJSONObject(i).getString("default_platform_uuid");
        		break;
        	}
        }
        return default_platform_uuid;
    }

    public boolean deletePackage(PackageThing pkg) {
    	MyResponse mr = null;
    	mr = getClient().delete(getURL() + "/" + pkg.getUUIDString());
    	//if (mr.getHttpResponseCode() == HttpStatus.SC_OK) {
    	// This is incorrect, the setHttpResponseCode is not called to set the actual error 
    	if (mr.getHttpResponseCode() == 0) {
    		return true;
    	}else {
    		return false;
    	}
    }
}
