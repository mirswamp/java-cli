/*
*  java-cli
*
*  Copyright 2016 Vamshi Basupalli <vamshi@cs.wisc.edu>, Malcolm Reid <mreid3@wisc.edu>, Jared Sweetland <jsweetland@wisc.edu>
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package org.continuousassurance.swamp.cli;

import edu.uiuc.ncsa.security.util.ssl.SSLConfiguration;
import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.HandlerFactory;
import org.continuousassurance.swamp.session.handlers.PackageHandler;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.session.util.SWAMPConfigurationLoader;
import org.continuousassurance.swamp.util.HandlerFactoryUtil;
import net.sf.json.JSONException;
import org.apache.http.client.CookieStore;
import org.continuousassurance.swamp.cli.exceptions.*;
import org.continuousassurance.swamp.cli.util.SwampPlatform;

import java.io.*;
import java.util.*;


public class SwampApiWrapper {

    private static final String USER_PATH = System.getProperty("user.home");
    private static final String FILE_SEPARATOR_KEY = System.getProperty("file.separator");
    private static final String SWAMP_DIR_NAME = ".SWAMP_SESSION";
    public static final String SWAMP_DIR_PATH = USER_PATH + FILE_SEPARATOR_KEY + SWAMP_DIR_NAME + FILE_SEPARATOR_KEY;
    public static final String SWAMP_HOST_NAME  = HandlerFactoryUtil.PD_ORIGIN_HEADER;

    private String rwsAddress;
    private String csaAddress;
    private String originHeader;
    private String refereHeader;
    private String hostHeader;
    private Map<String, Project> projectMap;
    private Map<String, Integer> packageTypeMap;
    private Map<String, PackageThing> packageMap;
    private Map<String, PackageVersion> packageVersionMap;
    private Map<String, Tool> toolMap;
    private Map<String, Platform> platformMap;
    private String cachedPkgProjectID;
    private String cachedPkgVersionProjectID;
    private String cachedToolProjectID;

    private HandlerFactory handlerFactory;
    
 
    public Properties getProp(String filepath){
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filepath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return prop;
    }

    protected String getRwsAddress() {
        return rwsAddress;
    }

    protected void setRwsAddress(String rwsAddress) {
        this.rwsAddress = rwsAddress;
    }

    protected String getCsaAddress() {
        return csaAddress;
    }

    protected void setCsaAddress(String csaAddress) {
        this.csaAddress = csaAddress;
    }

    protected String getOriginHeader() {
        return originHeader;
    }

    protected void setOriginHeader(String originHeader) {
        this.originHeader = originHeader;
    }

    protected String getRefereHeader() {
        return refereHeader;
    }

    protected void setRefereHeader(String refereHeader) {
        this.refereHeader = refereHeader;
    }

    protected String getHostHeader() {
        return hostHeader;
    }

    protected void setHostHeader(String hostHeader) {
        this.hostHeader = hostHeader;
    }

    public SwampApiWrapper(String host_name) {
        setHost(host_name);
        cachedPkgProjectID = "";
        cachedPkgVersionProjectID = "";
        cachedToolProjectID = "";
    }

    public SwampApiWrapper() throws Exception {
        this(HandlerFactoryUtil.PD_ORIGIN_HEADER);
    }

    public final void setHost(String host_name) {
    	
    	String web_server = SWAMPConfigurationLoader.getWebServiceURL(host_name);
    	if (web_server == null) {
    		web_server = host_name;
    	}
    	
    	//System.out.println("SWAMP FRONT-END SERVER URL: " + host_name);
    	//System.out.println("SWAMP WEB SERVER URL: " + web_server);
    	
    	setHostName(web_server);
    }
    
    public void setHostName(String host_name) {
        if (host_name == null) {
            throw new InvalidIdentifierException("host_name cannot be null");
        }
        setRwsAddress(host_name);
        setCsaAddress(host_name);
        setOriginHeader(host_name);
        setRefereHeader(host_name);
        setHostHeader(host_name);
    }

    public String getPkgTypeString(String pkg_lang,
            String pkg_lang_version,
            String pkg_build_sys,
            String package_type) {

        String pkg_type = null;

        switch (pkg_lang){
        case "Java":
            if(pkg_build_sys.toLowerCase().startsWith("android")) {
                if (pkg_lang_version.toLowerCase().equals("android-apk")) {
                    pkg_type = "Android .apk";
                }else {
                    pkg_type = "Android Java Source Code";
                }
            }else if(pkg_build_sys.toLowerCase().equals("java-bytecode")) {
                if (pkg_lang_version.toLowerCase().startsWith("java-7")) {
                	pkg_type = "Java 7 Bytecode";
                }else {
                	pkg_type = "Java 8 Bytecode";
                }
            }else {
                if (pkg_lang_version.toLowerCase().startsWith("java-7")) {
                	pkg_type = "Java 7 Source Code";
                }else {
                	pkg_type = "Java 8 Source Code";
                }
            }
            break;
        case "C":
        case "C++":
            pkg_type = "C/C++";
            break;
        case "Python-2":
            pkg_type = "Python2";
            break;
        case "Python-2 Python-3":
            pkg_type = "Python3";
            break;
        case "Python-3":
            pkg_type = "Python3";
            break;
        case "Ruby":
            if (package_type == null) {
                pkg_type = "Ruby";
            }else if (package_type.toLowerCase().equals("rails")) {
                pkg_type = "Ruby on Rails";
            }else if (package_type.toLowerCase().equals("sinatra")) {
                pkg_type = "Ruby Sinatra";
            }else if (package_type.toLowerCase().equals("padrino")) {
                pkg_type = "Ruby Padrino";
            }
            break;
        }
        return pkg_type;
    }

    public Integer getPkgTypeId(String pkg_lang,
            String pkg_lang_version,
            String pkg_build_sys,
            String package_type) {
        String pkg_type = getPkgTypeString(pkg_lang, pkg_lang_version, pkg_build_sys, package_type);
        return getPackageTypes().get(pkg_type);
    }

    public String login(String user_name, String password, String host_name) {
        setHost(host_name);
        return login(user_name, password);
    }

    public String login(String user_name, String password) {
        
        SSLConfiguration ssl_config = new SSLConfiguration();
        ssl_config.setTlsVersion("TLSv1.2");
        
        handlerFactory = HandlerFactoryUtil.createHandlerFactory(getRwsAddress(),
                getCsaAddress(),
                getOriginHeader(),
                getRefereHeader(),
                getHostHeader(),
                user_name,
                password,
                ssl_config);

        if (handlerFactory != null){
            return handlerFactory.getUserHandler().getCurrentUser().getIdentifierString();
        }

        //TODO: raise exception
        return null;
    }

    public boolean isLoggedIn () {
        return handlerFactory == null;
    }

    public void logout() {
        HandlerFactoryUtil.shutdown();
        deleteSession();
    }

    public void serialize(Object obj, String filepath) throws IOException{
        FileOutputStream fileOut = new FileOutputStream(filepath);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(obj);
        out.close();
        fileOut.close();
    }

    public Object deserialize(String filepath) throws IOException, ClassNotFoundException{
        Object obj = null;
        FileInputStream fileIn = new FileInputStream(filepath);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        obj = in.readObject();
        in.close();
        fileIn.close();

        return obj;
    }

    public void saveSession() {
        try {
            File dir = new File(SWAMP_DIR_PATH);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new IOException("Failed to create directory " + dir);
                }
            }

            serialize(handlerFactory.getCSASession(), SWAMP_DIR_PATH + "csa_session_object.ser");
            serialize(handlerFactory.getCSASession().getClient().getContext().getCookieStore(),
                    SWAMP_DIR_PATH + "csa_session_cookies.ser");
            serialize(handlerFactory.getRWSSession(), SWAMP_DIR_PATH + "rws_session_object.ser");
            serialize(handlerFactory.getRWSSession().getClient().getContext().getCookieStore(),
                    SWAMP_DIR_PATH + "rws_session_cookies.ser");
        }catch(IOException e){
            throw new SessionSaveException(e);
        }
    }

    private static boolean fileExists(String filepath) {
        // return new File(filepath).exists();
        return new File(filepath).isFile();
    }

    private static boolean stringsAreEqual(String a, String b) {
        // Utility method for comparing strings, either or both of which may be null
        if (a == b) {
            // Due to string interning this will often be true for a.equals(b)
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private void deleteSession() {
        File file = new File(SWAMP_DIR_PATH);
        if (file.isDirectory()){
            File[] fileList = file.listFiles();
            for (File f : fileList) {
                f.delete();
            }
            file.delete();
        }
    }

    /* Restores sessions, returns true if valid, non-expired sessions instantiated */
    public boolean restoreSession() throws SessionExpiredException, SessionRestoreException {
        String csa_object = SWAMP_DIR_PATH + "csa_session_object.ser";
        String rws_object = SWAMP_DIR_PATH + "rws_session_object.ser";
        String csa_cookies = SWAMP_DIR_PATH + "csa_session_cookies.ser";
        String rws_cookies = SWAMP_DIR_PATH + "rws_session_cookies.ser";

        if (!(SwampApiWrapper.fileExists(csa_object) && SwampApiWrapper.fileExists(rws_object) &&
                SwampApiWrapper.fileExists(csa_cookies) && SwampApiWrapper.fileExists(rws_cookies))) {
            throw new SessionRestoreException("Could not locate session objects and cookies to recover the session");
        }

        try {
            Session csa_session = (Session)deserialize(csa_object);
            Session rws_session = (Session)deserialize(rws_object);
            handlerFactory = new HandlerFactory(rws_session, csa_session);

            CookieStore csa_cookie_store = (CookieStore)deserialize(csa_cookies);
            CookieStore rws_cookie_store = (CookieStore)deserialize(rws_cookies);

            Date current_date = new Date();
            if (csa_cookie_store.clearExpired(current_date) || rws_cookie_store.clearExpired(current_date)){
                throw new SessionExpiredException("Session cookies expired");
            }

            handlerFactory.getCSASession().getClient().getContext().setCookieStore(csa_cookie_store);
            handlerFactory.getRWSSession().getClient().getContext().setCookieStore(rws_cookie_store);
            HandlerFactoryUtil.setHandlerFactory(handlerFactory);
        }catch (IOException e){
            throw new SessionRestoreException(e);
        }catch (ClassNotFoundException e){
            throw new SessionRestoreException(e);
        }

        return (handlerFactory.getUserHandler().getCurrentUser() != null);
    }

    public String getConnectedHostName(){
        return handlerFactory.getCSASession().getHost();
    }

    public void printUserInfo() {
        User user = handlerFactory.getUserHandler().getCurrentUser();
        System.out.printf("%s\n", "User:\t" + user.getFirstName() + " " + user.getLastName());
        System.out.printf("%s\n", "Email:\t" + user.getEmail());
        if (user.getPhone().equals("null")){
            System.out.printf("%s\n", "Phone:\t<Not provided>");
        }else{
            System.out.printf("%s\n", "Phone:\t" + user.getPhone());
        }
        System.out.printf("%s\n", "UUID:\t" + user.getUUIDString());
    }

    protected Map<String, Project> getAllProjects() {
        if (projectMap == null) {
            projectMap = new HashMap<String, Project>();
            for (Project proj : handlerFactory.getProjectHandler().getAll()) {
                projectMap.put(proj.getIdentifierString(), proj);
            }
        }
        return projectMap;
    }

    public List<Project> getProjectsList() {
        List<Project> proj_list = new ArrayList<Project>(getAllProjects().values());

        Collections.sort(proj_list, new Comparator<Project>() {
            public int compare(Project i1, Project i2) {
                return (i2.getFullName().compareTo(i1.getFullName()));
            }
        });
        return proj_list;
    }

    public Project getProject(String project_uuid) {
        Project project = getAllProjects().get(project_uuid);
        if (project == null) {
            throw new InvalidIdentifierException("Invalid project UUID: " + project_uuid);
        }
        return project;
    }

    public Project getProjectFromName (String project_name){
        Map<String,Project> proj_list = getAllProjects();
        Iterator<Project> proj_iterator = proj_list.values().iterator();
        while (proj_iterator.hasNext()){
            Project next_proj = proj_iterator.next();
            if (next_proj.getFullName().equals(project_name)){
                return next_proj;
            }
        }
        return null;
    }

    public void printAllProjects() {
        System.out.printf("\n\n%-21s %-38s %26s\n", "Project Name", "Project UUID", "Project Create Date");
        for(Project proj : getProjectsList()) {
            System.out.printf("%-21s %-38s '%26s'\n", proj.getFullName(),
                    proj.getUUIDString(), proj.getCreateDate());
        }
    }

    public Map<String, Integer> getPackageTypes() {

        if (packageTypeMap == null) {
            packageTypeMap = new HashMap<String, Integer>();

            List<String> all_types;

            try {
                all_types = handlerFactory.getPackageHandler().getTypes();
            }catch (JSONException e) {
                all_types = Arrays.asList("C/C++", "Java 7 Source Code", "Java 7 Bytecode",
                        "Python2", "Python3", "Android Java Source Code", "Ruby",
                        "Ruby Sinatra", "Ruby on Rails", "Ruby Padrino",
                        "Android .apk","Java 8 Source Code","Java 8 Bytecode");
            }

            int i = 0;
            for (String pkg_type : all_types) {
                packageTypeMap.put(pkg_type, Integer.valueOf(++i));
            }

        }
        return packageTypeMap;
    }

    public List<String> getPackageTypesList() {
        return new ArrayList<String>(getPackageTypes().keySet());
    }

    protected ConversionMapImpl getPkgConfMap(Properties pkg_conf) {
        ConversionMapImpl map = new ConversionMapImpl();
        map.put("version_string", pkg_conf.getProperty("package-version"));
        map.put("source_path", pkg_conf.getProperty("package-dir"));
        map.put("config_dir", pkg_conf.getProperty("config-dir", null));
        map.put("config_cmd", pkg_conf.getProperty("config-cmd", null));
        map.put("config_opt", pkg_conf.getProperty("config-opt", null));
        map.put("build_dir", pkg_conf.getProperty("build-dir", null));
        map.put("build_system", pkg_conf.getProperty("build-sys", null));
        map.put("build_file", pkg_conf.getProperty("build-file", null));
        map.put("build_target", pkg_conf.getProperty("build-target", null));
        map.put("build_opt", pkg_conf.getProperty("build-opt", null));
        map.put("use_gradle_wrapper", pkg_conf.getProperty("gradle-wrapper", "false"));
        return map;
    }

    public String uploadNewPackage(String pkg_conf_file, String pkg_archive_file, String project_uuid) {
        PackageHandler<? extends PackageThing> pkg_handler = handlerFactory.getPackageHandler();
        Properties pkg_conf = getProp(pkg_conf_file);

        PackageThing pkg_thing = pkg_handler.create(pkg_conf.getProperty("package-short-name"),
                pkg_conf.getProperty("package-description", "No Description Available"),
                getPkgTypeId(pkg_conf.getProperty("package-language"),
                        pkg_conf.getProperty("package-language-version", ""),
                        pkg_conf.getProperty("build-sys"),
                        pkg_conf.getProperty("package-type")));

        ConversionMapImpl map = getPkgConfMap(pkg_conf);
        map.put("project_uuid", project_uuid);

        PackageVersion pkg_version = handlerFactory.getPackageVersionHandler().create(pkg_thing,
                new File(pkg_archive_file),
                map);

        getAllPackageVersions(project_uuid).put(pkg_version.getIdentifierString(), pkg_version);
        getAllPackages(project_uuid).put(pkg_thing.getIdentifierString(), pkg_thing);

        return pkg_version.getUUIDString();
    }

    public String uploadPackageVersion(String pkg_conf_file, String pkg_archive_file, String project_uuid) {
        Properties pkg_conf = getProp(pkg_conf_file);
        PackageThing pkg_thing = null;

        for(PackageThing pkg : getAllPackages(project_uuid).values()){
            if (pkg.getName().equals(pkg_conf.getProperty("package-short-name"))){
                pkg_thing = pkg;
                break;
            }
        }

        if (pkg_thing == null){
            return uploadNewPackage(pkg_conf_file, pkg_archive_file, project_uuid);
        }else{
            ConversionMapImpl map = getPkgConfMap(pkg_conf);
            map.put("project_uuid", project_uuid);

            PackageVersion pkg_version = handlerFactory.getPackageVersionHandler().create(pkg_thing,
                    new File(pkg_archive_file),
                    map);


            getAllPackageVersions(project_uuid).put(pkg_version.getIdentifierString(), pkg_version);

            return pkg_version.getUUIDString();
        }
    }

    public String uploadPackage(String pkg_conf_file, String pkg_archive_file,
            String project_uuid, boolean isNew) throws InvalidIdentifierException {

        getProject(project_uuid);

        if(isNew) {
            return uploadNewPackage(pkg_conf_file, pkg_archive_file, project_uuid);
        }else{
            return uploadPackageVersion(pkg_conf_file, pkg_archive_file, project_uuid);
        }
    }

    public boolean deletePackage(String pkg_uuid, String project_uuid) throws InvalidIdentifierException {

        getProject(project_uuid);

        PackageThing pkg_thing = null;

        for(PackageThing pkg : getAllPackages(project_uuid).values()){
            if (pkg.getUUIDString().equals(pkg_uuid)){
                pkg_thing = pkg;
                break;
            }
        }
        if (pkg_thing == null) {
            throw new InvalidIdentifierException("Invalid package UUID: " + pkg_uuid);
        }
        return handlerFactory.getPackageHandler().deletePackage(pkg_thing);
    }


    protected Map<String, PackageThing> getAllPackages(String project_uuid) {
        if ((packageMap == null) || (!stringsAreEqual(cachedPkgProjectID, project_uuid))) {
            cachedPkgProjectID = project_uuid;
            packageMap = new HashMap<String, PackageThing>();
            if (project_uuid == null) {
                for (PackageThing pkg : handlerFactory.getPackageHandler().getAll()){
                    packageMap.put(pkg.getUUIDString(), pkg);
                }
            }else {
                for (PackageThing pkg : handlerFactory.getPackageHandler().getAll(getProject(project_uuid))){
                    packageMap.put(pkg.getUUIDString(), pkg);
                }
            }
        }
        return packageMap;
    }

    public List<PackageThing> getPackagesList(String project_uuid) {
        List<PackageThing> pkg_list = new ArrayList<PackageThing>(getAllPackages(project_uuid).values());

        Collections.sort(pkg_list, new Comparator<PackageThing>() {
            public int compare(PackageThing i1, PackageThing i2) {
                return (i2.getName().compareTo(i1.getName()));
            }
        });
        return pkg_list;
    }

    protected Map<String, PackageVersion> getAllPackageVersions(String project_uuid) {
        if ((packageVersionMap == null) || (!stringsAreEqual(cachedPkgVersionProjectID, project_uuid))) {
            cachedPkgVersionProjectID = project_uuid;
            packageVersionMap = new HashMap<String, PackageVersion>();
            if (project_uuid == null) {
                for (PackageThing pkg : handlerFactory.getPackageHandler().getAll()){
                    for (PackageVersion pkg_ver : handlerFactory.getPackageVersionHandler().getAll(pkg)) {
                        packageVersionMap.put(pkg_ver.getUUIDString(), pkg_ver);
                    }
                }
            }else {
                for (PackageThing pkg : handlerFactory.getPackageHandler().getAll(getProject(project_uuid))){
                    for (PackageVersion pkg_ver : handlerFactory.getPackageVersionHandler().getAll(pkg)) {
                        packageVersionMap.put(pkg_ver.getUUIDString(), pkg_ver);
                    }
                }
            }
        }
        return packageVersionMap;
    }

    public List<PackageVersion> getPackageVersionsList(String project_uuid) {
        List<PackageVersion> pkg_list = new ArrayList<PackageVersion>(getAllPackageVersions(project_uuid).values());

        Collections.sort(pkg_list, new Comparator<PackageVersion>() {
            public int compare(PackageVersion i1, PackageVersion i2) {
                return (i1.getVersionString().compareTo(i2.getVersionString()));
            }
        });
        return pkg_list;
    }

    public PackageVersion getPackageVersion(String pkg_ver_uuid, String project_uuid) {
        PackageVersion pkg_ver = getAllPackageVersions(project_uuid).get(pkg_ver_uuid);
        if (pkg_ver == null) {
            throw new InvalidIdentifierException("Invalid Package Version UUID: " + pkg_ver_uuid);
        }
        return pkg_ver;
    }

    public void printAllPackagesSummary(String project_uuid) {
        System.out.printf("\n\n%-21s %-38s %-20s %-30s\n", "Package Name", "Package UUID", "Package Versions", "Package Description");
        for(PackageThing pkg : getPackagesList(project_uuid)){
            System.out.printf("%-21s %-38s %-20s %-30s\n", pkg.getName(), pkg.getIdentifierString(),
                    pkg.getVersions(), pkg.getDescription());
        }
    }

    public void printAllPackagesVerbose(String project_uuid) {
        for(PackageThing pkg : getAllPackages(project_uuid).values()){
            System.out.printf("\n\n%-21s %-38s %-30s\n", pkg.getName(), pkg.getIdentifierString(), pkg.getDescription());
            for(PackageVersion pkg_ver : getPackageVersionsList(project_uuid)){
                if (pkg_ver.getPackageThing().getUUIDString().equals(pkg.getUUIDString())) {
                    System.out.printf("\t%-13s %-38s\n", pkg_ver.getVersionString(), pkg_ver.getUUIDString());
                }
            }
        }
    }

    public void printAllPackages(String project_uuid, boolean verbose) {
        if(verbose){
            printAllPackagesVerbose(project_uuid);
        }else {
            printAllPackagesSummary(project_uuid);
        }
    }

    protected Map<String, Tool> getAllTools(String project_uuid) throws InvalidIdentifierException {

        if ((toolMap == null) || (!stringsAreEqual(cachedToolProjectID, project_uuid))) {
            cachedToolProjectID = project_uuid;
            toolMap = new HashMap<String, Tool>();
            for (Tool tool : handlerFactory.getToolHandler().getAll()) {
                if (tool.getPolicyCode() == "null"){    //FIXME: This is temporary
                    toolMap.put(tool.getIdentifierString(), tool);
                }
            }

            if (project_uuid != null){
                Project proj = getProject(project_uuid);

                for (Tool tool : handlerFactory.getToolHandler().getAll(proj)) {
                    if (tool.getPolicyCode() == "null"){    //FIXME: This is temporary
                        toolMap.put(tool.getIdentifierString(), tool);
                    }
                }
            }
        }
        return toolMap;
    }

    /*
     * pkg_type should be one of the Key return by the API getPackageTypes
     */
    public List<Tool> getTools(String pkg_type, String project_uuid) throws InvalidIdentifierException {

        if (!getPackageTypes().containsKey(pkg_type)){
            throw new InvalidIdentifierException(String.format("Package type '%s' is invalid, it must be one of %s",
                    pkg_type, getPackageTypes().keySet()));
        }

        List<Tool> tool_list = new ArrayList<>();
        Map<String, Tool> tool_map = getAllTools(project_uuid);
        for(String tool_uuid : tool_map.keySet()){
            if (tool_map.get(tool_uuid).getSupportedPkgTypes().contains(pkg_type)){
                tool_list.add(tool_map.get(tool_uuid));
            }
        }
        return tool_list;
    }

    public Tool getTool(String tool_uuid, String project_uuid) throws InvalidIdentifierException {
        Tool tool = getAllTools(project_uuid).get(tool_uuid);
        if (tool == null){
            throw new InvalidIdentifierException("Invalid Tool UUID: " + tool_uuid);
        }
        return tool;
    }

    public Tool getToolFromName (String tool_name, String project_uuid) throws InvalidIdentifierException {
        Map<String,Tool> tool_list = getAllTools(project_uuid);
        Iterator<Tool> tool_iterator = tool_list.values().iterator();
        while (tool_iterator.hasNext()){
            Tool next_tool = tool_iterator.next();
            if (next_tool.getName().equals(tool_name)){
                return next_tool;
            }
        }
        return null;
    }

    public void printAllTools(String project_uuid) throws InvalidIdentifierException {
        System.out.printf("\n\n%-21s %-38s %-40s %s\n", "Tool Name", "Tool UUID", "Supported Package Types", "Supported Platforms");
        for(Tool tool : getAllTools(project_uuid).values()){
            System.out.printf("%-21s %-38s %-40s %s\n", tool.getName(), tool.getIdentifierString(),
                    tool.getSupportedPkgTypes(), tool.getSupportedPlatforms());
            List<? extends ToolVersion> tool_versions = handlerFactory.getToolVersionHandler().getAll(tool);
            Collections.sort(tool_versions, 
            		new Comparator<ToolVersion>() {
            	public int compare(ToolVersion i1, ToolVersion i2) {
            		return (i2.getReleaseDate().compareTo(i1.getReleaseDate()));
            	}
            });
            for(ToolVersion tool_version : tool_versions){
                System.out.printf("\t%-38s %-13s\n", tool_version.getIdentifier(),
                        tool_version.getVersion());
            }
        }
    }

    public Map<String, Platform> getAllPlatforms() {
        if (platformMap == null) {
            platformMap = new HashMap<String, Platform>();
            for (Platform plat : handlerFactory.getPlatformHandler().getAll()) {
                platformMap.put(plat.getIdentifierString(), plat);
            }
        }
        return platformMap;
    }

    public List<Platform> getPlatformsList() {
        List<Platform> plat_list = new ArrayList<Platform>(getAllPlatforms().values());

        Collections.sort(plat_list, new Comparator<Platform>() {
            public int compare(Platform i1, Platform i2) {
                return (i1.getName().compareTo(i2.getName()));
            }
        });
        return plat_list;

    }

    public List<SwampPlatform> getSwampPlatformsList() {
    	List<SwampPlatform> swamp_platforms = new ArrayList<SwampPlatform>();
    	
        for(Platform platform : getAllPlatforms().values()){
        	for (PlatformVersion platform_version : handlerFactory.getPlatformVersionHandler().getAll(platform)){
        		try {
					swamp_platforms.add(SwampPlatform.convertToSwampPackage(platform_version));
				} catch (UnkownPlatformException e) {
					// TODO Auto-generated catch block
					System.out.println(e);
				}
        	}
        }
        
        return swamp_platforms;
    }
    
    public Platform getPlatform(String platform_uuid) {
        Platform platform = getAllPlatforms().get(platform_uuid);
        if (platform == null) {
            throw new InvalidIdentifierException("Invalid Platform UUID: " + platform_uuid);
        }
        return platform;
    }

    public Platform getPlatformFromName (String platform_name) throws InvalidIdentifierException  {
        Map<String,Platform> platform_list = getAllPlatforms();
        Iterator<Platform> platform_iterator = platform_list.values().iterator();
        while (platform_iterator.hasNext()){
            Platform next_platform = platform_iterator.next();
            if (next_platform.getName().equals(platform_name)){
                return next_platform;
            }
        }

        throw new InvalidIdentifierException(String.format("Platform %s does not exist.\n", platform_name));
    }

    public List<Platform> getSupportedPlatforms(String tool_uuid, String project_uuid) throws InvalidIdentifierException {
        Tool tool = getTool(tool_uuid, project_uuid);
        List<Platform> supported_plats = new ArrayList<>();
        for (Platform plat : getPlatformsList()){
            for (String plat_name : tool.getSupportedPlatforms()){
                if (plat_name.equals(plat.getName())){
                    supported_plats.add(plat);
                }
            }
        }
        return supported_plats;
    }
    
    /*
     * pkg_type must be one of the values retured by getPackageTypesList()
     */
    public Platform getDefaultPlatform(String pkg_type) {

        if(!getPackageTypesList().contains(pkg_type)) {
            throw new InvalidIdentifierException(String.format("Invalid package type: %s", pkg_type));
        }

        String default_platform_uuid;

        try {
            default_platform_uuid = handlerFactory.getPackageHandler().getDefaultPlatform(pkg_type);
        }catch (JSONException e) {
          if (pkg_type.startsWith("Android")) {
              default_platform_uuid = "48f9a9b0-976f-11e4-829b-001a4a81450b";  // Android Ubuntu-12.04-64
          }else {
              default_platform_uuid = "fc55810b-09d7-11e3-a239-001a4a81450b";  // Red Hat Enterprise Linux 64-bit
          }
        }
        
        Platform default_platform = null;
        if (default_platform_uuid != null) {
            default_platform = getPlatform(default_platform_uuid);
        }
        return default_platform;
    }

    public void printAllPlatforms(String pkg_type) {

        if (pkg_type != null) {
            Platform plat = getDefaultPlatform(pkg_type);
            System.out.printf("\n%-40s %-38s %26s\n", "Project Name", "Platform UUID", "Platform Create Date");
            System.out.printf("%-40s %-38s %s\n", plat.getName(), plat.getIdentifierString(), plat.getCreateDate());
        }else {
            System.out.printf("\n%-40s %-38s %26s\n", "Project Name", "Platform UUID", "Platform Create Date");
            for(Platform plat : getAllPlatforms().values()){
                System.out.printf("%-40s %-38s %s\n", plat.getName(), plat.getIdentifierString(), plat.getCreateDate());
            }
        }
    }

    public AssessmentRun getAssessment(String assess_uuid, Project proj) {
        for (AssessmentRun arun : handlerFactory.getAssessmentHandler().getAllAssessments(proj)){
            if (arun.getIdentifierString().equals(assess_uuid)){
                return arun;
            }
        }
        return null;
    }

    public void printAssessments(String project_uuid, boolean quiet) {
        Project proj = getProject(project_uuid);
        for (AssessmentRun arun : handlerFactory.getAssessmentHandler().getAllAssessments(proj)){
            if (quiet){
                System.out.printf(arun.getUUIDString() + "\n");
            }else{
                System.out.printf("Assessment on " + arun.getFilename() +":\n\tUUID: " + arun.getUUIDString() + "\n");
            }
        }
    }

    public void printAssessment(String assessment_uuid, String project_uuid) {
        AssessmentRun arun = getAssessment(assessment_uuid, getProject(project_uuid));
        if (arun == null){
            System.out.println("Assessment " + assessment_uuid + " not found. Please verify the UUID");
        }else{
            System.out.println("Assessment Results on " + arun.getIdentifierString());
            System.out.println("Package: \t" + (arun.getPkg() == null ? "N/A" : arun.getPkg().getName()));
            System.out.println("Project: \t" + (arun.getProject() == null ? "N/A" : arun.getProject().getFullName()));
            System.out.println("Tool:    \t" + (arun.getTool() == null ? "N/A" : arun.getTool().getName()));
            System.out.println("Platform:\t" + (arun.getPlatform() == null ? "N/A" : arun.getPlatform().getName()));
        }
    }

    public String runAssessment(String pkg_ver_uuid,
            String tool_uuid,
            String project_uuid,
            String platform_uuid) throws IncompatibleAssessmentTupleException, InvalidIdentifierException {

        PackageVersion pkg_ver = getPackageVersion(pkg_ver_uuid, project_uuid);
        Tool tool = getTool(tool_uuid, project_uuid);
        Project proj = getProject(project_uuid);

        Platform plat = null;
        if(platform_uuid == null) {
            plat = getDefaultPlatform(pkg_ver.getPackageThing().getType());
        }else {
            plat = getPlatform(platform_uuid);
        }

        if (!tool.getSupportedPkgTypes().contains(pkg_ver.getPackageThing().getType())) {
            throw new IncompatibleAssessmentTupleException(String.format("%s (%s) does not support this package type \"%s\"",
                    tool.getName(),
                    tool.getSupportedPkgTypes(),
                    pkg_ver.getPackageThing().getType()));
        }

        if (!tool.getSupportedPlatforms().contains(plat.getName())) {
            throw new IncompatibleAssessmentTupleException(String.format("%s (%s) is not supported on this platform \"%s\"",
                    tool.getName(),
                    tool.getSupportedPlatforms(),
                    plat.getName()));
        }

        return runAssessment(pkg_ver, tool, proj, plat);
    }

    public List<String> runAssessment(String pkg_ver_uuid,
    		List<String> tool_uuid_list,
    		String project_uuid,
    		List<String> platform_uuid_list) throws IncompatibleAssessmentTupleException, InvalidIdentifierException {
    	PackageVersion pkg_ver = getPackageVersion(pkg_ver_uuid, project_uuid);
    	Project project = getProject(project_uuid);

    	List<Platform> platforms = new ArrayList<Platform>();
    	
    	if (null == platform_uuid_list || platform_uuid_list.isEmpty()){
    		platform_uuid_list = new ArrayList<String>(); 
    		platform_uuid_list.add(getDefaultPlatform(pkg_ver.getPackageThing().getType()).getUUIDString());
    	}
    	
    	for (String platform_uuid: platform_uuid_list) {
    		Platform platform = null;
			if(platform_uuid == null) {
				platform = getDefaultPlatform(pkg_ver.getPackageThing().getType());
			}else {
				platform = getPlatform(platform_uuid);
			}
    	
    		for (String tool_uuid: tool_uuid_list) {
    			Tool tool = getTool(tool_uuid, project_uuid);
    			
    			if (!tool.getSupportedPkgTypes().contains(pkg_ver.getPackageThing().getType())) {
    				throw new IncompatibleAssessmentTupleException(String.format("%s (%s) does not support this package type \"%s\"",
    						tool.getName(),
    						tool.getSupportedPkgTypes(),
    						pkg_ver.getPackageThing().getType()));
    			}

    			if (!tool.getSupportedPlatforms().contains(platform.getName())) {
    				throw new IncompatibleAssessmentTupleException(String.format("%s (%s) is not supported on this platform \"%s\"",
    						tool.getName(),
    						tool.getSupportedPlatforms(),
    						platform.getName()));
    			}
    		}
    		platforms.add(platform);
    	}

    	List<Tool> tools = new ArrayList<Tool>();
    	for (String tool_uuid: tool_uuid_list) {
    		tools.add(getTool(tool_uuid, project_uuid));
    	}
    	
    	List<AssessmentRun> arun_list = runAssessment(pkg_ver, tools, project, platforms);
    	
    	if (arun_list != null){
    		List<String> arun_uuid_list = new ArrayList<String>();
    		for (AssessmentRun arun : arun_list) {
    			arun_uuid_list.add(arun.getUUIDString());
    		}
    		return arun_uuid_list;
    	}else {
    		return null;
    	}
    }

    protected String runAssessment(PackageVersion pkg, Tool tool, Project project, Platform platform) {
        AssessmentRun arun = handlerFactory.getAssessmentHandler().create(project, pkg, platform, tool);
        handlerFactory.getRunRequestHandler().submitOneTimeRequest(arun, true);
        return arun.getUUIDString();
    }

    protected List<AssessmentRun> runAssessment(PackageVersion pkg, List<Tool> tools, Project project, List<Platform> platforms) {
    	List<AssessmentRun> arun_list = new ArrayList<AssessmentRun>();
	    for (Platform platform : platforms) {
	    	for (Tool tool : tools) {
	    		arun_list.add(handlerFactory.getAssessmentHandler().create(project, pkg, platform, tool));
	    	}
    	}
        if (handlerFactory.getRunRequestHandler().submitOneTimeRequest(arun_list, true)){
        	return arun_list;
        }else {
        	return null;
        }
        
    }

    protected List<? extends AssessmentResults> getAssessmentResults(String project_uuid) {
        Project project = getProject(project_uuid);
        return handlerFactory.getAssessmentResultHandler().getAll(project);
    }

    public void getAssessmentResults(String project_uuid, String asssess_result_uuid, String filepath) throws FileNotFoundException, IOException {

        for(AssessmentResults results : getAssessmentResults(project_uuid)){
            if (results.getUUIDString().equals(asssess_result_uuid)) {
                ByteArrayOutputStream data = (ByteArrayOutputStream)handlerFactory.getAssessmentResultHandler().getScarfResults(results);
                if (data != null) {
                    OutputStream outputStream = new FileOutputStream(filepath);
                    data.writeTo(outputStream);
                    data.close();
                    outputStream.close();
                }
            }
        }
    }

    public List<? extends AssessmentRecord> getAllAssessmentRecords(Project project) {
        return handlerFactory.getassessmentRecordHandler().getExecutionRecords(project);
    }

    public void printAllAssessmentStatus(String project_uuid) {
        Project project = getProject(project_uuid);
        System.out.printf("\n\n%-38s %-38s %-22s %s\n", 
                "ASSESSMENT RUN UUID", "ASSESSMENT RESULT UUID", 
                "STATUS", "WEAKNESS COUNT");
        for(AssessmentRecord assessment_record : getAllAssessmentRecords(project)) {
            System.out.printf("%-38s %-38s %-22s %d\n", assessment_record.getAssessmentRunUUID(),
                    assessment_record.getAssessmentResultUUID(),
                    assessment_record.getStatus(),
                    assessment_record.getWeaknessCount());
        }
    }

    public AssessmentRecord getAssessmentRecord(String project_uuid, String assessment_uuid){
        Project project = getProject(project_uuid);
        
        for(AssessmentRecord assessment_record : getAllAssessmentRecords(project)) {
            if (assessment_record.getAssessmentRunUUID().equals(assessment_uuid)){
                return assessment_record;
            }
         }
        
        throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessment_uuid);
    }
    
    public boolean deleteAssessmentRecord(String project_uuid, String assessment_uuid){
        Project project = getProject(project_uuid);
        
        for(AssessmentRecord assessment_record : getAllAssessmentRecords(project)) {
            if (assessment_record.getAssessmentRunUUID().equals(assessment_uuid)){
                return handlerFactory.getassessmentRecordHandler().deleteAssessmentRecord(assessment_record);
                
            }
         }
        
        throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessment_uuid);
    }
    
    public void printAssessmentStatus(String project_uuid, String assessment_uuid) {
        AssessmentRecord assessment_record = getAssessmentRecord(project_uuid, assessment_uuid);        
        System.out.printf("%s, %d", assessment_record.getStatus(), assessment_record.getWeaknessCount());

        if (assessment_record.getAssessmentResultUUID() == null){
            System.out.printf("\n");
        }else{
            System.out.printf(", %-38s\n", assessment_record.getAssessmentResultUUID());
        }
    }

    public void printAssessmentResultsUUID(String project_uuid, String assessment_uuid) {
        System.out.println(getAssessmentRecord(project_uuid, assessment_uuid).getAssessmentResultUUID());
    }
}
