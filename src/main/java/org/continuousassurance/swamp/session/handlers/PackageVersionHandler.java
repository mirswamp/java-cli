package org.continuousassurance.swamp.session.handlers;

import edu.uiuc.ncsa.security.core.exceptions.NotImplementedException;
import edu.uiuc.ncsa.security.core.util.BasicIdentifier;
import net.sf.json.JSONObject;
import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.exceptions.NoJSONReturnedException;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.session.util.SWAMPIdentifiers;
import org.continuousassurance.swamp.util.HandlerFactoryUtil;

import java.io.File;
import java.util.*;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 1/13/15 at  11:16 AM
 */
public class PackageVersionHandler<T extends PackageVersion> extends AbstractHandler<T> {
    protected static final int JAVA_SOURCE_PACKAGE_TYPE = 10;
    protected static final int JAVA_BYTECODE_PACKAGE_TYPE = 20;
    protected static final int C_SOURCE_PACKAGE_TYPE = 30;

    public static final String PACKAGE_VERSION_UUID = "package_version_uuid";
    public static final String PACKAGE_UUID = "package_uuid";
    public static final String PLATFORM_UUID = "platform_uuid";
    public static final String VERSION_STRING = "version_string";
    public static final String VERSION_SHARING_STATUS = "version_sharing_status";


    public static final String RELEASE_DATE = "release_date";
    public static final String RETIRE_DATE = "retire_date";
    public static final String NOTES = "notes";


    public static final String PACKAGE_PATH = "package_path";
    public static final String SOURCE_PATH = "source_path";

    public static final String CONFIG_DIR = "config_dir";
    public static final String CONFIG_CMD = "config_cmd";
    public static final String CONFIG_OPT = "config_opt";

    public static final String BUILD_FILE = "build_file";
    public static final String BUILD_SYSTEM = "build_system";
    public static final String BUILD_TARGET = "build_target";


    public static final String BYTECODE_CLASS_PATH = "bytecode_class_path";

    public static final String BYTECODE_AUX_CLASS_PATH = "bytecode_aux_class_path";
    public static final String BYTECODE_SOURCE_PATH = "bytecode_source_path";


    public static final String BUILD_DIR = "build_dir";
    public static final String BUILD_CMD = "build_cmd";
    public static final String BUILD_OPT = "build_opt";

    public final static String FILE_UPLOAD_NAME = "uploaded_file";
    public final static String FILE_UPLOAD_PATH = "path";
    public final static String FILE_UPLOAD_EXTENSION = "extension";
    public final static String FILE_UPLOAD_MIME_TYPE = "mime";
    public final static String FILE_UPLOAD_SIZE = "size";
    public final static String FILE_UPLOAD_ID = "destination_path";

    public final static String FILENAME = "filename";

    /**
     * This is the key in the conversion map for the create command.
     */
    public final static String FILE_UPLOAD_PATH_KEY = "urn:file:path";


    public PackageVersionHandler(Session session) {
        super(session);
    }

    protected T fromJSON(JSONObject json) {
        T packageVersion = (T) new PackageVersion(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {FILENAME, FILE_UPLOAD_NAME, FILE_UPLOAD_PATH, FILE_UPLOAD_EXTENSION, FILE_UPLOAD_MIME_TYPE, FILE_UPLOAD_ID,
                BUILD_CMD, BUILD_DIR, BUILD_OPT,
                BUILD_FILE, BUILD_SYSTEM, BUILD_TARGET,
                CONFIG_CMD, CONFIG_DIR, CONFIG_OPT,
                PACKAGE_PATH, SOURCE_PATH,
                NOTES, VERSION_SHARING_STATUS, VERSION_STRING,
                BYTECODE_AUX_CLASS_PATH, BYTECODE_CLASS_PATH, BYTECODE_SOURCE_PATH};
        String[] uAttrib = {PACKAGE_UUID, PACKAGE_VERSION_UUID, PLATFORM_UUID};
        String[] dAttrib = {RELEASE_DATE, RETIRE_DATE};

        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        packageVersion.setConversionMap(map);
        return packageVersion;
    }

    @Override
    public List<T> getAll() {
        throw new NotImplementedException();
    }

    /**
     * Get all the versions of a package
     *
     * @param packageThing
     * @return
     */
    public List<T> getAll(PackageThing packageThing) {
        String url = createURL("packages/" + packageThing.getUUIDString() + "/versions");
        MyResponse mr = getClient().rawGet(url);
        ArrayList<T> versions = new ArrayList<>();
        if (mr.jsonArray == null || mr.jsonArray.isEmpty()) {
            return versions;
        }
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            PackageVersion pkg_ver = fromJSON(json);
            pkg_ver.setPackageThing(packageThing);
            versions.add((T) pkg_ver);
        }

        return versions;
    }

    public File download(PackageVersion packageVersion, File targetDirectory) {
        String url = createURL("packages/versions/" + packageVersion.getUUIDString() + "/download");
        return getClient().getFile(url, targetDirectory, packageVersion.getFilename());
    }

    @Override
    public String getURL() {
        //return createURL("packages/versions");
        return createURL("packages/versions/store");
    }

    /*
    curl 'https://swa-csaweb-pd-01.mir-swamp.org/packages/versions/upload' -X POST
    -H 'Accept: *//*' -H 'Accept-Encoding: gzip, deflate'
    -H 'Accept-Language: en-US,en;q=0.5'
    -H 'Content-Length: 4188829'
    -H 'Content-Type: multipart/form-data; boundary=---------------------------1978615270641118831918295646'
    -H 'Cookie: swamp_reg_session=eyJpdiI%3D;%3D%3D; swamp_csa_session3D;zJmOWY1ZjYifQ%3D%3D'
    -H 'Host: swa-csaweb-pd-01.mir-swamp.org'
    -H 'Origin: https://www.mir-swamp.org'
     -H 'Referer: https://www.mir-swamp.org/'
      -H 'User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0'
     */
    protected FileHandle upload(PackageThing packageThing, File f) {
        String url = createURL("packages/versions/upload");
        Map<String, Object> map = new HashMap<>();
        map.put("user_uid", getSession().getUserUID());
        map.put("package_uuid", packageThing.getUUIDString());
        map.put("external_url", "");

        //DebugUtil.say(this, ".upload: url = " + url);
        List<File> files = new LinkedList<>();
        files.add(f);
        MyResponse mr = getClient().rawPost(url, map, files);
        //System.out.println(getClass().getSimpleName() + " json=" + mr.json);
        FileHandle fileHandle = new FileHandle(getSession());
        fileHandle.setIdentifier(SWAMPIdentifiers.toIdentifier(mr.json.getString(FILE_UPLOAD_ID)));
        fileHandle.setExtension(mr.json.getString(PackageVersionHandler.FILE_UPLOAD_EXTENSION));
        //fileHandle.setName(f.getName());
        //fileHandle.setName(mr.json.getString(PackageVersionHandler.FILE_UPLOAD_NAME));
        fileHandle.setName(mr.json.getString(PackageVersionHandler.FILENAME));
        fileHandle.setPath(mr.json.getString(PackageVersionHandler.FILE_UPLOAD_PATH));
        fileHandle.setSize(mr.json.getLong(PackageVersionHandler.FILE_UPLOAD_SIZE));
        fileHandle.setMimeType(mr.json.getString(PackageVersionHandler.FILE_UPLOAD_MIME_TYPE));
        return fileHandle;
    }

    @Override
    public SwampThing create(ConversionMapImpl map) {
        throw new NotImplementedException("This method is not supported for package versions. Use create(PackageThing, File, ConversionMapImpl");
    }

    /**
     * There are 4 (four) distinct steps that must be done in sequence to upload a file and create a package version.
     * This does them all. The map contains items (such as build options) that are augmented by the correct information
     * from the file itself. Note that you cannot actually create a package version until the file has been uploaded
     * and certain information is returned by the SWAMP. Think of the version as something that is attached to an uploaded
     * file.
     *
     * @param packageThing
     * @param f
     * @param map
     * @return
     */
    public PackageVersion create(PackageThing packageThing, File f, ConversionMapImpl map) {
        FileHandle fileHandle = upload(packageThing, f); //STEP 1
        //map.put(SOURCE_PATH, uploadNew(fileHandle)); // STEP 2
        //String package_uuid = uploadNew(packageThing.getName(), packageThing.getDescription(), "", 1);
        String package_uuid = packageThing.getUUIDString();
        String packagePath = fileHandle.getUUIDString() + "/" + fileHandle.getName();
        map.put(VERSION_SHARING_STATUS, "protected");
        map.put(PACKAGE_UUID, package_uuid);
        map.put(PACKAGE_PATH, packagePath);
        map.put(FILE_UPLOAD_NAME, fileHandle.getName());
        PackageVersion packageVersion = (PackageVersion) super.create(super.mapToJSON(map)); // STEP 3
        //PackageVersion packageVersion = (PackageVersion) super.create(map); // STEP 3
        // set sharing record-- STEP 4
        if (map.containsKey("project_uuid")) {
        	setVersionSharingStatus(packageVersion, map.getString("project_uuid"));
        }
        packageVersion.setPackageThing(packageThing);
        packageVersion.setFileHandle(fileHandle);
        return packageVersion;
    }

    /**
     * Sets the sharing record for the given version. This record associates the package with a project
     * and a failure to set the sharing record means that, in effect, this version does not exist.
     *
     * @param packageVersion
     * @param project_uuid
     */
    public void setVersionSharingStatus(PackageVersion packageVersion, String project_uuid) {
        HashMap<String, Object> addMap = new HashMap<>();
        //addMap.put("projects[0][project_uid]", project_uuid);
        addMap.put("project_uuids[]", project_uuid);   //2017-06-13 This is the new format
        try {
            getClient().rawPut(createURL("packages/versions/" + packageVersion.getUUIDString() + "/sharing"), addMap);
        } catch (NoJSONReturnedException x) {
            // rock on. This one method does not return JSON.
        }
    }

    /**
     * Sets the sharing record for the given version. This record associates the package with a project
     * and a failure to set the sharing record means that, in effect, this version does not exist.
     *
     *When a package can be part of multiple projects
     * @param packageVersion
     * @param project_uuids
     */      
    public void setVersionSharingStatus(PackageVersion packageVersion, List<String> project_uuids) {
    	ConversionMapImpl map = new ConversionMapImpl();
        map.put("project_uuids", project_uuids);
        try {
            getClient().rawPut(createURL("packages/versions/" + packageVersion.getUUIDString() + "/sharing"), 
            		mapToJSON(map));
        } catch (NoJSONReturnedException x) {
            // rock on. This one method does not return JSON.
        }
    }
    
    /**
     * Gets all the projects shared with this package version.
     * @param packageVersion
     * @return
     */
    public List<Project> getSharedProjects(PackageVersion packageVersion) {
        LinkedList<Project> projects = new LinkedList<>();
        MyResponse response = getClient().rawGet(createURL("packages/versions/" + packageVersion.getUUIDString() + "/sharing"));
        if (response.hasJSON()) {
            if(response.jsonArray == null) return projects; // if no response, return empty list.
            // then we should have gotten an array of project uuids.
            for (int i = 0; i < response.jsonArray.size(); i++) {
                projects.add(HandlerFactoryUtil.getProjectH().get(BasicIdentifier.newID(response.jsonArray.getString(i))));

            }
        }
        return projects;
    }
    
    /**
     * Sets the sharing record for the given version. This record associates the package with a project
     * and a failure to set the sharing record means that, in effect, this version does not exist.
     *
     *When a package can be part of multiple projects
     * @param packageVersion
     * @param project_uuids
     */      
    public void addPackageVersionDependencies(PackageVersion packageVersion, ConversionMapImpl dep_map) {
        try {
        	for (String plat_ver_uuid : dep_map.keySet()) {
        		ConversionMapImpl new_map = new ConversionMapImpl();
        		new_map.put("package_version_uuid", packageVersion.getIdentifierString());
        		new_map.put("dependency_list", dep_map.get(plat_ver_uuid));
        		new_map.put("platform_version_uuid", plat_ver_uuid);
        		getClient().rawPost(createURL("packages/versions/dependencies"), 
        				mapToJSON(new_map));
        	}
        } catch (NoJSONReturnedException x) {
            // rock on. This one method does not return JSON.
        	throw x;
        }
    }
    
    public boolean deletePackageVersion(PackageVersion package_version)  {
    		MyResponse mr = null;
    		mr = getClient().delete(createURL("packages/versions/" + package_version.getUUIDString()));
    	//if (mr.getHttpResponseCode() == HttpStatus.SC_OK) {
    	// This is incorrect, the setHttpResponseCode is not called to set the actual error 
    		if (mr.getHttpResponseCode() == 0) {
    			return true;
    		}else {
    			return false;
    		}
    }

}
