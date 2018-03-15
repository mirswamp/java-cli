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
import net.sf.json.JSONException;
import org.apache.http.client.CookieStore;
import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.cli.exceptions.*;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.HandlerFactory;
import org.continuousassurance.swamp.session.handlers.PackageHandler;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.session.util.Proxy;
import org.continuousassurance.swamp.session.util.SWAMPConfigurationLoader;
import org.continuousassurance.swamp.util.HandlerFactoryUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @author      Vamshi Basupalli vamshi@cs.wisc.edu
 * @version     1.3
 */
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
	private Map<String, PlatformVersion> platformMap;
	private String cachedPkgProjectID;
	private String cachedPkgVersionProjectID;
	private String cachedToolProjectID;
	private SSLConfiguration sslConfig;

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

	/**
	 * Main constructor
	 *
	 */
	public SwampApiWrapper() {
		cachedPkgProjectID = "";
		cachedPkgVersionProjectID = "";
		cachedToolProjectID = "";

		sslConfig = new SSLConfiguration();
		sslConfig.setTlsVersion("TLSv1.2");
	}

	protected final void setHost(String host_name, Proxy proxy) throws MalformedURLException {

        if (host_name == null) {
            throw new InvalidIdentifierException("host_name cannot be null");
        }

        String web_server = SWAMPConfigurationLoader.getWebServiceURL(host_name, sslConfig, proxy);
		if (web_server == null) {
			web_server = host_name;
		}
		
		setRwsAddress(web_server);
		setCsaAddress(web_server);
		setOriginHeader(web_server);
		setRefereHeader(web_server);
		setHostHeader(web_server);
	}


	public static Proxy getProxy() throws MalformedURLException {
		
		String env_https_proxy = System.getenv("https_proxy");
		String env_http_proxy = System.getenv("http_proxy");
		
		String https_proxy_host = System.getProperty("https.proxyHost", null);
		int https_proxy_port = Integer.parseInt(System.getProperty("https.proxyPort", "443"));
		
		String http_proxy_host = System.getProperty("http.proxyHost", null);
		int http_proxy_port = Integer.parseInt(System.getProperty("http.proxyPort", "80"));

		Proxy proxy = null;
		
		if (env_https_proxy != null || env_http_proxy != null) {
		    URL url = null;
		    
		    if (env_https_proxy != null) {
		        if (!env_https_proxy.startsWith("https://")) {
		            env_https_proxy = "https://" + env_https_proxy;
		        }
		        url = new URL(env_https_proxy);
		    }else {
		        if (!env_http_proxy.startsWith("http://")) {
                    env_http_proxy = "http://" + env_http_proxy;
                }
		        url = new URL(env_http_proxy);
		    }
			
		    if (url.getPort() != -1) {
		        proxy = new Proxy(url.getPort(), url.getHost(), url.getProtocol(), true);
		    }else {
		        proxy = new Proxy(url.getDefaultPort(), url.getHost(), url.getProtocol(), true);
		    }
			String userinfo = url.getUserInfo();
			if (userinfo != null) {
			    proxy.setUsername(userinfo.substring(0, userinfo.indexOf(':')));
			    proxy.setPassword(userinfo.substring(userinfo.indexOf(':') + 1));
			}
		}else if (https_proxy_host != null && https_proxy_port != -1) {
			proxy = new Proxy(https_proxy_port, https_proxy_host, "https", true);
			proxy.setUsername(System.getProperty("https.proxyUser", null));
            proxy.setPassword(System.getProperty("http.proxyPassword", null));
		}else if (http_proxy_host != null && http_proxy_port != -1){
			proxy = new Proxy(http_proxy_port, http_proxy_host, "http", true);
			proxy.setUsername(System.getProperty("http.proxyUser", null));
            proxy.setPassword(System.getProperty("http.proxyPassword", null));
		}else {
			proxy = new Proxy();
		}
		
		return proxy;
	}
	
    
    /**
     * Login into SWAMP
     *
     * @param user_name: SWAMP Instance's user name
     * @param password: SWAMP Instance's password 
     * @param host_name: SWAMP Instance's host name
     *   
     * @return SWAMP user-id
     * @throws MalformedURLException 
     */
    public String login(String user_name, String password, String host_name) throws MalformedURLException {
        Proxy proxy = getProxy();
        return login(user_name, password, host_name, proxy); 
    }
    

    /**
     * Login into SWAMP
     *
     * @param user_name: SWAMP Instance's user name
     * @param password: SWAMP Instance's password 
     * @param host_name: SWAMP Instance's host name
     * @param proxy: http[s] proxy setting to connect to SWAMP
     * @return SWAMP user-id
     * @throws MalformedURLException 
     */
    public String login(String user_name, String password, String host_name, Proxy proxy) throws MalformedURLException {
        setHost(host_name, proxy);   
		
		handlerFactory = HandlerFactoryUtil.createHandlerFactory(getRwsAddress(),
				getCsaAddress(),
				getOriginHeader(),
				getRefereHeader(),
				getHostHeader(),
				user_name,
				password,
				sslConfig,
				proxy);

		if (handlerFactory != null){
			return handlerFactory.getUserHandler().getCurrentUser().getIdentifierString();
		}

		//TODO: raise exception
		return null;
	}

	public boolean isLoggedIn () {
		return handlerFactory == null;
	}

	/**
	 * Logout from SWAMP
	 *
	 */
	public void logout() {
		HandlerFactoryUtil.shutdown();
		deleteSession();
	}

	protected void serialize(Object obj, String filepath) throws IOException{
		FileOutputStream fileOut = new FileOutputStream(filepath);
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		out.writeObject(obj);
		out.close();
		fileOut.close();
	}

	protected Object deserialize(String filepath) throws IOException, ClassNotFoundException{
		Object obj = null;
		FileInputStream fileIn = new FileInputStream(filepath);
		ObjectInputStream in = new ObjectInputStream(fileIn);
		obj = in.readObject();
		in.close();
		fileIn.close();

		return obj;
	}

	/**
	 * Save SWAMP session object/cookies in ~/.SWAMP_SESSION/
	 *
	 */
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

	/**
	 * Deletes SWAMP session object/cookies in ~/.SWAMP_SESSION/
	 *
	 */
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

	/**
	 * Reads and starts SWAMP session object/cookies in ~/.SWAMP_SESSION/
	 *
	 * @return true if valid, non-expired sessions instantiated 
	 */
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

	/**
	 * Gets currently connected SWAMP host name
	 *
	 * @return SWAMP host name
	 */
	public String getConnectedHostName(){
		return handlerFactory.getCSASession().getHost();
	}

	/**
	 * Gets currently logged-in user's information
	 *
	 * @return User object
	 */
	public User getUserInfo() {
		return handlerFactory.getUserHandler().getCurrentUser();
	}


    /**
     * Converts package.conf values to a package type
     * <p>
     * Takes attributes in package.conf and converts it into package types
     * that SWAMP UI understands
     * <p>
     *
     * @param pkg_lang any one of: [Java, C, C++, Ruby, 
     * Python-2, Python-3, Javascript, CSS, XML, HTML, PHP]
     * @param pkg_lang_version: The version of language 
     * required at build time Example: java-7, java-8, ruby-2.0.0
     * @param pkg_build_sys: Package build system, see package.conf documentation
     * @param package_type: Package application type, valid only for ruby: Any one of [sinatra, rails, padrino]  
     * @return One of ["C/C++", "Java 7 Source Code", "Java 7 Bytecode",
                        "Python2", "Python3", "Android Java Source Code", "Ruby",
                        "Ruby Sinatra", "Ruby on Rails", "Ruby Padrino",
                        "Android .apk","Java 8 Source Code","Java 8 Bytecode"].
     */
    public String getPkgTypeString(String pkg_lang,
            String pkg_lang_version,
            String pkg_build_sys,
            String package_type) {

        String pkg_type = null;

        if (pkg_build_sys.toLowerCase().equals("android-apk")) {
            pkg_type = "Android .apk";
        }else {
            if (pkg_lang != null) {
                pkg_lang = pkg_lang.split(" ")[0];
            }
            switch (pkg_lang){
            case "Java":
                if(pkg_build_sys.toLowerCase().startsWith("android")) {
                    pkg_type = "Android Java Source Code";
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
            case "PHP":
            case "JavaScript":
            case "HTML":
            case "CSS":
            case "XML":
                pkg_type = "Web Scripting";
                break;
            }
        }
        return pkg_type;
    }

    /**
     * Converts package.conf values to a package type id
     * <p>
     * Takes attributes in package.conf and converts it into a package type id
     * that SWAMP UI understands
     * <p>
     *
     * @param pkg_lang any one of: [Java, C, C++, Ruby, 
     * Python-2, Python-3, Javascript, CSS, XML, HTML, PHP]
     * @param pkg_lang_version: The version of language 
     * required at build time Example: java-7, java-8, ruby-2.0.0
     * @param pkg_build_sys: Package build system, see package.conf documentation
     * @param package_type: Package application type, valid only for ruby: Any one of [sinatra, rails, padrino]  
     * @return One of ["C/C++": 1 , "Java 7 Source Code": 2, "Java 7 Bytecode": 3,
                        "Python2": 4, "Python3": 5, "Android Java Source Code": 6, "Ruby": 7,
                        "Ruby Sinatra": 8, "Ruby on Rails": 9, "Ruby Padrino": 10,
                        "Android .apk": 11, "Java 8 Source Code": 12, 
                        "Java 8 Bytecode": 13, "Web Scripting": 14].
     */
    public Integer getPkgTypeId(String pkg_lang,
            String pkg_lang_version,
            String pkg_build_sys,
            String package_type) {
        String pkg_type = getPkgTypeString(pkg_lang, pkg_lang_version, pkg_build_sys, package_type);
        return getPackageTypes().get(pkg_type);
    }

    /**
     * Gets a hashmap (package type, package type id) from SWAMP
     *
     *@return hashmap (package type, package type id)
     */
    public Map<String, Integer> getPackageTypes() {

        if (packageTypeMap == null) {

            try {
                packageTypeMap = handlerFactory.getPackageHandler().getTypes();
            }catch (JSONException e) {

                packageTypeMap = new HashMap<String, Integer>();

                List<String> all_types = Arrays.asList("C/C++", "Java 7 Source Code", "Java 7 Bytecode",
                        "Python2", "Python3", "Android Java Source Code", "Ruby",
                        "Ruby Sinatra", "Ruby on Rails", "Ruby Padrino",
                        "Android .apk","Java 8 Source Code","Java 8 Bytecode");
                int i = 0;
                for (String pkg_type : all_types) {
                    packageTypeMap.put(pkg_type, Integer.valueOf(++i));
                }
            }

        }
        return packageTypeMap;
    }

    /**
     * Gets a list of package types from SWAMP
     *
     *@return list of package types
     */
    public List<String> getPackageTypesList() {
        return new ArrayList<String>(getPackageTypes().keySet());
    }

    /**
     * Creates a hash-map of package configuration attributes from package.conf hash-map
     * <p>
     * Creates a hash-map of package configuration attributes from package.conf hash-map.
     * The keys in the map are what SWAMP API understands 
     * <p>
     *
     *@param pkg_conf properties object of a package.conf file
     *@return hash-map for package version configuration 
     */
    protected ConversionMapImpl getPkgConfMap(Properties pkg_conf) {
        ConversionMapImpl map = new ConversionMapImpl();
        map.put("android_sdk_target", pkg_conf.getProperty("android-sdk-target", null));
        map.put("android_lint_target", pkg_conf.getProperty("android-lint-target", null));
        map.put("android_maven_plugin", pkg_conf.getProperty("android-maven-plugin", null));
        map.put("android_redo_build", pkg_conf.getProperty("android-redo-build", "false"));
        
        map.put("ant-version", pkg_conf.getProperty("ant-version", null));

        map.put("build_cmd", pkg_conf.getProperty("build-cmd", null));
        map.put("build_dir", pkg_conf.getProperty("build-dir", null));
        map.put("build_file", pkg_conf.getProperty("build-file", null));
        map.put("build_opt", pkg_conf.getProperty("build-opt", null));
        map.put("build_system", pkg_conf.getProperty("build-sys", null));
        map.put("build_target", pkg_conf.getProperty("build-target", null));
        
        map.put("config_cmd", pkg_conf.getProperty("config-cmd", null));
        map.put("config_opt", pkg_conf.getProperty("config-opt", null));
        map.put("config_dir", pkg_conf.getProperty("config-dir", null));

        map.put("use_gradle_wrapper", pkg_conf.getProperty("gradle-wrapper", "false"));
        map.put("maven_version", pkg_conf.getProperty("maven_version", null));
        
        map.put("version_string", pkg_conf.getProperty("package-version"));
        map.put("source_path", pkg_conf.getProperty("package-dir"));

        map.put("language_version", pkg_conf.getProperty("package-language-version", null));
        map.put("bytecode_class_path", pkg_conf.getProperty("package-classpath", null));
        map.put("bytecode_aux_class_path", pkg_conf.getProperty("package-auxclasspath", null));
        map.put("bytecode_source_path", pkg_conf.getProperty("package-srcdir", null));

        return map;
    }
    
	/**
	 * Gets currently logged-in user's projects in a hash-map
	 *
	 * @return hash-map of (project name, project object)
	 */
	protected Map<String, Project> getAllProjects() {
		if (projectMap == null) {
			projectMap = new HashMap<String, Project>();
			for (Project proj : handlerFactory.getProjectHandler().getAll()) {
				projectMap.put(proj.getIdentifierString(), proj);
			}
		}
		return projectMap;
	}

	/**
	 * Gets currently logged-in user's projects as a list
	 *
	 * @return list of project objects
	 */
	public List<Project> getProjectsList() {
		List<Project> proj_list = new ArrayList<Project>(getAllProjects().values());

		Collections.sort(proj_list, new Comparator<Project>() {
			public int compare(Project i1, Project i2) {
				return (i2.getFullName().compareTo(i1.getFullName()));
			}
		});
		return proj_list;
	}

	/**
	 * Gets a project object from a project UUID string
	 *
	 * @param project_uuid: project UUID string
	 * @return project object
	 * @throws InvalidIdentifierException if UUID provided is not a valid one
	 */
	public Project getProject(String project_uuid) {
		Project project = getAllProjects().get(project_uuid);
		if (project == null) {
			throw new InvalidIdentifierException("Invalid project UUID: " + project_uuid);
		}
		return project;
	}

	/**
	 * Gets a project object from a project name string
	 *
	 * @param project_name: project name
	 * @return project object
	 * @throws InvalidNameException if UUID provided is not a valid one
	 */
	public Project getProjectFromName (String project_name){
		for (Project project : getProjectsList()){	
			if (project.getFullName().equals(project_name)){
				return project;
			}
		}
		throw new InvalidNameException("Invalid project name: " + project_name);
	}

	/**
	 * Upload a new software package
	 *
	 * @param pkg_conf_file: Path for the package.conf file for the package
	 * @param pkg_archive_file: Path to the package archive 
	 * @param project_uuid: UUID for the project that this package must be associated with
	 * @param os_dep_map: hash-map of the OS dependencies 
	 * Example: (key, value) = (ubuntu-16.04-64=libsqlite3-dev libmysqlclient-dev)
	 * 
	 * @return the new package's version UUID
	 */
	public String uploadNewPackage(String pkg_conf_file, 
			String pkg_archive_file, 
			String project_uuid,
			Map<String, String> os_dep_map) {
		PackageHandler<? extends PackageThing> pkg_handler = handlerFactory.getPackageHandler();
		Properties pkg_conf = getProp(pkg_conf_file);

		PackageThing pkg_thing = pkg_handler.create(pkg_conf.getProperty("package-short-name"),
				pkg_conf.getProperty("package-description", "No Description Available"),
				pkg_conf.getProperty("external-url", null),
				getPkgTypeId(pkg_conf.getProperty("package-language"),
						pkg_conf.getProperty("package-language-version", ""),
						pkg_conf.getProperty("build-sys"),
						pkg_conf.getProperty("package-type")),
				pkg_conf.getProperty("package-language"));

		ConversionMapImpl map = getPkgConfMap(pkg_conf);
		map.put("project_uuid", project_uuid);

		PackageVersion pkg_version = handlerFactory.getPackageVersionHandler().create(pkg_thing,
				new File(pkg_archive_file),
				map);

		addPackageDependencies(pkg_version, os_dep_map);

		packageVersionMap = null;
		packageMap = null;
		//getAllPackageVersions(project_uuid).put(pkg_version.getIdentifierString(), pkg_version);
		//getAllPackages(project_uuid).put(pkg_thing.getIdentifierString(), pkg_thing);

		return pkg_version.getUUIDString();
	}

	/**
	 * Upload a package version
	 * 
	 * <p>
	 * If a package with the same name as the this package does not exist then
	 * a new package is created
	 * <p>
	 *
	 * @param pkg_conf_file: Path for the package.conf file for the package
	 * @param pkg_archive_file: Path to the package archive 
	 * @param project_uuid: UUID for the project that this package must be associated with
	 * @param os_dep_map: hash-map of the OS dependencies 
	 * Example: (key, value) = (ubuntu-16.04-64=libsqlite3-dev libmysqlclient-dev)
	 * 
	 * @return the new package's version UUID
	 */
	public String uploadPackageVersion(String pkg_conf_file, 
			String pkg_archive_file, 
			String project_uuid,
			Map<String, String> os_dep_map) {
		Properties pkg_conf = getProp(pkg_conf_file);
		PackageThing pkg_thing = null;

		for(PackageThing pkg : getAllPackages(project_uuid).values()){
			if (pkg.getName().equals(pkg_conf.getProperty("package-short-name"))){
				pkg_thing = pkg;
				break;
			}
		}

		if (pkg_thing == null){
			return uploadNewPackage(pkg_conf_file, pkg_archive_file, project_uuid, os_dep_map);
		}else{
			ConversionMapImpl map = getPkgConfMap(pkg_conf);
			map.put("project_uuid", project_uuid);

			PackageVersion pkg_version = handlerFactory.getPackageVersionHandler().create(pkg_thing,
					new File(pkg_archive_file),
					map);
			addPackageDependencies(pkg_version, os_dep_map);
			packageVersionMap = null;
			//getAllPackageVersions(project_uuid);

			return pkg_version.getUUIDString();
		}
	}

	/**
	 * Upload a package
	 * 
	 * <p>
	 * If a package with the same name as the this package does not exist then
	 * a new package is created
	 * <p>
	 *
	 * @param pkg_conf_file: Path for the package.conf file for the package
	 * @param pkg_archive_file: Path to the package archive 
	 * @param project_uuid: UUID for the project that this package must be associated with
	 * @param os_dep_map: hash-map of the OS dependencies
	 * Example: (key, value) = (ubuntu-16.04-64=libsqlite3-dev libmysqlclient-dev)
	 * @param is_new: flag to explictly say that this should be stored as a new package 
	 * 
	 * @return the new package's version UUID
	 */

	public String uploadPackage(String pkg_conf_file, 
			String pkg_archive_file,
			String project_uuid,
			Map<String, String> os_dep_map,
			boolean is_new) throws InvalidIdentifierException {

		getProject(project_uuid);

		if(is_new) {
			return uploadNewPackage(pkg_conf_file, pkg_archive_file, project_uuid, os_dep_map);
		}else{
			return uploadPackageVersion(pkg_conf_file, pkg_archive_file, project_uuid, os_dep_map);
		}
	}

	/**
	 * Add package version's OS dependencies
	 * <p>
	 * A package may require OS packages to be pre-installed/available.
	 * Additional OS packages can be installed before the build 
	 * by specifying OS dependencies
	 * <p>
	 *
	 * @param package_version: PackageVersion object for the package
	 * @param os_dep_map: hash-map of the OS dependencies
	 * Example: (key, value) = (ubuntu-16.04-64=libsqlite3-dev libmysqlclient-dev)
	 * 
	 * @throws InvalidIdentifierException if UUID provided is not a valid one
	 */
	protected void addPackageDependencies(PackageVersion package_version, Map<String, String> os_dep_map) {
		if (os_dep_map != null){
			
			ConversionMapImpl dep_map = new ConversionMapImpl();

			for (PlatformVersion platform_version : getAllPlatformVersionsList()) {
				String deps = os_dep_map.get(platform_version.getDisplayString());
				if (deps != null) {
					dep_map.put(platform_version.getIdentifierString(), deps);
				}
			}
			handlerFactory.getPackageVersionHandler().addPackageVersionDependencies(package_version, dep_map);
		}
	}

	/**
	 * Delete a version of a package
	 *
	 * @param pkg_ver_uuid: package version UUID
	 * @param project_uuid: project UUID
	 * 
	 * @return status: deleted or not deleted
	 * @throws InvalidIdentifierException if UUID provided is not a valid one
	 */
	public boolean deletePackageVersion(String pkg_ver_uuid, String project_uuid) throws InvalidIdentifierException {

		getProject(project_uuid);

		for(PackageVersion pkg_ver : getAllPackageVersions(project_uuid).values()){
			if (pkg_ver.getUUIDString().equals(pkg_ver_uuid)){
				return deletePackageVersion(pkg_ver);
			}
		}

		throw new InvalidIdentifierException("Invalid package version UUID: " + pkg_ver_uuid);
	}

	/**
	 * Delete a version of a package
	 *
	 * @param pkg_ver_uuid: package version UUID
	 * 
	 * @return status: deleted or not deleted
	 * @throws InvalidIdentifierException if UUID provided is not a valid one
	 */
	public boolean deletePackageVersion(String pkg_ver_uuid) throws InvalidIdentifierException {

		for(PackageVersion pkg_ver : getAllPackageVersions().values()){
			if (pkg_ver.getUUIDString().equals(pkg_ver_uuid)){
				return deletePackageVersion(pkg_ver);
			}
		}

		throw new InvalidIdentifierException("Invalid package version UUID: " + pkg_ver_uuid);
	}

	/**
	 * Delete a version of a package
	 *
	 * @param pkg_ver: package version object
	 * 
	 * @return status: deleted or not deleted
	 */
	public boolean deletePackageVersion(PackageVersion pkg_ver) throws InvalidIdentifierException {
		boolean ret_val = handlerFactory.getPackageVersionHandler().deletePackageVersion(pkg_ver);
		if(ret_val) {
			//packageVersionMap = null;
			packageVersionMap.remove(pkg_ver.getIdentifierString());
		}
		return ret_val;
	}

	/**
	 * Delete a package with all its versions
	 *
	 * @param pkg_uuid: package UUID
	 * @param project_uuid: project UUID
	 * 
	 * @return status: deleted or not deleted
	 * @throws InvalidIdentifierException if UUID provided is not a valid one
	 */
	public boolean deletePackage(String pkg_uuid, String project_uuid) throws InvalidIdentifierException {

		getProject(project_uuid);

		for(PackageThing pkg : getAllPackages(project_uuid).values()){
			if (pkg.getUUIDString().equals(pkg_uuid)){
				return deletePackage(pkg);
			}
		}

		throw new InvalidIdentifierException("Invalid package UUID: " + pkg_uuid);
	}

	/**
	 * Delete a package with all its versions
	 *
	 * @param pkg: package object
	 * 
	 * @return status: deleted or not deleted
	 */
	public boolean deletePackage(PackageThing pkg) throws InvalidIdentifierException {
		boolean ret_val = handlerFactory.getPackageHandler().deletePackage(pkg);
		if(ret_val) {
			packageVersionMap = null;
		}
		return ret_val;
	}

	/**
	 * Get a hash-map of all the packages of a project
	 *
	 *  @param project_uuid: project UUID
	 *  
	 *  @return hash-map of package-uuid, package object
	 */
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

	/**
	 * Get a hash-map of all the packages uploaded by a user or accessible to a user
	 *  
	 *  @return hash-map of package-uuid, package object
	 */
	protected Map<String, PackageThing> getAllPackages() {
		if (packageMap == null) {
			packageMap = new HashMap<String, PackageThing>();
			
			for (PackageThing pkg : handlerFactory.getPackageHandler().getAll()){
				packageMap.put(pkg.getUUIDString(), pkg);
			}
		}
		return packageMap;
	}

	/**
	 * Get a list of all the packages in a project
	 * 
	 *  @param project_uuid: project UUID
	 *  @return hash-map of package-uuid, package object
	 */
	public List<PackageThing> getPackagesList(String project_uuid) {
		List<PackageThing> pkg_list = new ArrayList<PackageThing>(getAllPackages(project_uuid).values());

		Collections.sort(pkg_list, new Comparator<PackageThing>() {
			public int compare(PackageThing i1, PackageThing i2) {
				return (i2.getName().compareTo(i1.getName()));
			}
		});
		return pkg_list;
	}

	/**
	 * Get a list of all the packages uploaded by a user or accessible to a user
	 *  
	 *  @return hash-map of package-uuid, package object
	 */
	public List<PackageThing> getPackagesList() {
		List<PackageThing> pkg_list = new ArrayList<PackageThing>(getAllPackages().values());

		Collections.sort(pkg_list, new Comparator<PackageThing>() {
			public int compare(PackageThing i1, PackageThing i2) {
				return (i2.getName().compareTo(i1.getName()));
			}
		});
		return pkg_list;
	}

	/**
	 * Get a hash-map of all the package versions in a project
	 *  
	 *  @param project_uuid: project UUID
	 *  @return hash-map of package-version-uuid, package version object
	 */
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

	/**
	 * Get a hash-map of all the package versions uploaded by a user or accessible to a user
	 *
	 *  @return hash-map of package-version-uuid, package version object
	 */
	protected Map<String, PackageVersion> getAllPackageVersions() {
		if (packageVersionMap == null) {
			packageVersionMap = new HashMap<String, PackageVersion>();
			
			for (PackageThing pkg : handlerFactory.getPackageHandler().getAll()){
				for (PackageVersion pkg_ver : handlerFactory.getPackageVersionHandler().getAll(pkg)) {
					packageVersionMap.put(pkg_ver.getUUIDString(), pkg_ver);
				}
			}
		}
		return packageVersionMap;
	}
	
	/**
	 * Get a list of all the package versions in a project
	 *  
	 *  @param project_uuid: project UUID
	 *  @return list of package version objects
	 */
	public List<PackageVersion> getPackageVersionsList(String project_uuid) {
		List<PackageVersion> pkg_list = new ArrayList<PackageVersion>(getAllPackageVersions(project_uuid).values());

		Collections.sort(pkg_list, new Comparator<PackageVersion>() {
			public int compare(PackageVersion i1, PackageVersion i2) {
				return (i1.getVersionString().compareTo(i2.getVersionString()));
			}
		});
		return pkg_list;
	}

	/**
	 * Get a list of all the package versions uploaded by a user or accessible to a user
	 *  
	 *  @return list of package version objects
	 */
	public List<PackageVersion> getPackageVersionsList() {
		List<PackageVersion> pkg_list = new ArrayList<PackageVersion>(getAllPackageVersions().values());

		Collections.sort(pkg_list, new Comparator<PackageVersion>() {
			public int compare(PackageVersion i1, PackageVersion i2) {
				return (i1.getVersionString().compareTo(i2.getVersionString()));
			}
		});
		return pkg_list;
	}
	
	/**
     * Get a hash-map of all the package versions uploaded by a user or accessible to a user
     *
     *  @return hash-map of package-version-uuid, package version object
     */
    protected List<PackageVersion> getPackageVersions(PackageThing pkg) {
        if (packageVersionMap == null) {
            packageVersionMap = new HashMap<String, PackageVersion>();
        }   
        
        List <PackageVersion> package_versions = (List <PackageVersion>)handlerFactory.getPackageVersionHandler().getAll(pkg);
        for (PackageVersion pkg_ver : package_versions) {
            packageVersionMap.put(pkg_ver.getUUIDString(), pkg_ver);
        }
        
        return package_versions;
    }


	/**
	 * Get a package version object
	 *  
	 *  @param pkg_ver_uuid: package version UUID
	 *  @param project_uuid: project UUID
	 *  
	 *  @return package version object
	 */
	public PackageVersion getPackageVersion(String pkg_ver_uuid, String project_uuid) {
		PackageVersion pkg_ver = getAllPackageVersions(project_uuid).get(pkg_ver_uuid);

		if (pkg_ver == null) {
			throw new InvalidIdentifierException("Invalid Package Version UUID: " + pkg_ver_uuid);
		}
		return pkg_ver;
	}

	/**
	 * Get a hash-map of all the tools along with any project specific tools
	 *  
	 *  @param project_uuid: project UUID (for project specific tools)
	 *  
	 *  @return hash-map of tool-uuid, tool object
	 */
	protected Map<String, Tool> getAllTools(String project_uuid) throws InvalidIdentifierException {

		if ((toolMap == null) || (!stringsAreEqual(cachedToolProjectID, project_uuid))) {
			cachedToolProjectID = project_uuid;
			toolMap = new HashMap<String, Tool>();
			for (Tool tool : handlerFactory.getToolHandler().getAll()) {
				//if (tool.getPolicyCode() == null){    //FIXME: This is temporary
					toolMap.put(tool.getIdentifierString(), tool);
				//}
			}

			if (project_uuid != null){
				Project proj = getProject(project_uuid);

				for (Tool tool : handlerFactory.getToolHandler().getAll(proj)) {
					//if (tool.getPolicyCode() == null){    //FIXME: This is temporary
						toolMap.put(tool.getIdentifierString(), tool);
					//}
				}
			}
		}
		return toolMap;
	}

	/**
	 * Get a list of tools provided package type and project uuid (for project specific tools)
	 *  
	 *  @param pkg_type: should be one of the Key return by the API getPackageTypes
	 *  @param project_uuid: project UUID (for project specific tools)
	 *  
	 *  @return list of tool objects
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

	/**
	 * Get a tool object
	 *  
	 *  @param tool_uuid: tool UUID
	 *  @param project_uuid: project UUID (for project specific tools)
	 *  
	 *  @return tool object
	 */
	public Tool getTool(String tool_uuid, String project_uuid) throws InvalidIdentifierException {
		Tool tool = getAllTools(project_uuid).get(tool_uuid);
		if (tool == null){
			throw new InvalidIdentifierException("Invalid Tool UUID: " + tool_uuid);
		}
		return tool;
	}

	/**
	 * Get a tool object from tool name
	 *  
	 *  @param tool_name: name of a tool
	 *  @param project_uuid: project UUID (for project specific tools)
	 *  
	 *  @return tool object
	 */
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

	/**
     * Get a list of tools provided package type and project uuid (for project specific tools)
     *  
     *  @param pkg_type: should be one of the Key return by the API getPackageTypes
     *  @param project_uuid: project UUID (for project specific tools)
     *  
     *  @return list of tool objects
     */
    @SuppressWarnings("unchecked")
    public List<ToolVersion> getToolVersions(Tool tool) throws InvalidIdentifierException {
        return (List<ToolVersion>)handlerFactory.getToolVersionHandler().getAll(tool);
    }


	/**
	 * Get the platform object given the platform UUID
	 *  
	 *  @param platform_uuid: platform UUID
	 *  
	 *  @return platform object
	 */
	public Platform getPlatform(String platform_uuid) {
		return getAllPlatforms().get(platform_uuid);
	}

	/**
	 * Get hash-map of all the platforms
	 *  
	 *  @return hash-map of platform UUID, platform object
	 */
	public Map<String, Platform> getAllPlatforms() {
		HashMap<String, Platform> platforms = new HashMap<String, Platform>();

		for (Platform plat : handlerFactory.getPlatformHandler().getAll()) {
			platforms.put(plat.getIdentifierString(), plat);
		}
		return platforms;
	}

	/**
	 * Get hash-map of all the platform versions
	 *  
	 *  @return hash-map of platform version UUID, platform version object
	 */
	public Map<String, PlatformVersion> getAllPlatformVersions() {
		if (platformMap == null) {
			platformMap = new HashMap<String, PlatformVersion>();
			for (Platform platform : handlerFactory.getPlatformHandler().getAll()) {
				for (PlatformVersion platform_version : handlerFactory.getPlatformVersionHandler().getAll(platform) )
					platformMap.put(platform_version.getIdentifierString(), platform_version);
			}
		}
		return platformMap;
	}	

	/**
	 * Get list of all the platform versions
	 *  
	 *  @return list of platform version objects
	 */
	public List<PlatformVersion> getAllPlatformVersionsList() {
		List<PlatformVersion> platform_versions = new ArrayList<PlatformVersion>(getAllPlatformVersions().values());

		Collections.sort(platform_versions, new Comparator<PlatformVersion>() {
			public int compare(PlatformVersion i1, PlatformVersion i2) {
				return (i1.getDisplayString().compareTo(i2.getDisplayString()));
			}
		});
		return platform_versions;
	}

	/**
	 * Get the platform version object given the platform UUID
	 *  
	 *  @param platform_version_uuid: platform version UUID
	 *  
	 *  @return platform version object
	 */
	public PlatformVersion getPlatformVersion(String platform_version_uuid) {
		PlatformVersion platform = getAllPlatformVersions().get(platform_version_uuid);
		if (platform == null) {
			throw new InvalidIdentifierException("Invalid Platform UUID: " + platform_version_uuid);
		}
		return platform;
	}

	/**
	 * Get the platform version object given the platform name
	 *  
	 *  @param platform_version_name: platform version name
	 *  
	 *  @return platform version object
	 */

	public PlatformVersion getPlatformVersionFromName (String platform_version_name) throws InvalidNameException  {
		Map<String, PlatformVersion> platform_version_list = getAllPlatformVersions();
		Iterator<PlatformVersion> platform_iterator = platform_version_list.values().iterator();
		while (platform_iterator.hasNext()){
			PlatformVersion next_platform = platform_iterator.next();
			if (next_platform.getDisplayString().equals(platform_version_name)){
				return next_platform;
			}
		}

		throw new InvalidNameException(String.format("Platform %s does not exist.\n", platform_version_name));
	}

	/**
	 * Get the platform version objects that a tool supports
	 * <p>
	 * Not all the tools run on all the platforms, this method returns 
	 * a list of platform version objects that a given tool can run on
	 * <p>
	 *  
	 *  @param tool_uuid: tool UUID
	 *  @param project_uuid: project UUID
	 *  
	 *  @return list of platform version object
	 */
	public List<PlatformVersion> getSupportedPlatformVersions(String tool_uuid, 
			String project_uuid) {
		Tool tool = getTool(tool_uuid, project_uuid);
		List<PlatformVersion> supported_platforms = new ArrayList<>();
		for (PlatformVersion platform_version : new ArrayList<PlatformVersion>(getAllPlatformVersions().values())){
			for (String supp_plat_name : tool.getSupportedPlatforms()){
				if (supp_plat_name.equals(platform_version.getPlatform().getName())){
					supported_platforms.add(platform_version);
				}
			}
		}
		return supported_platforms;
	}

	/**
	 * Get the default platform version a package type
	 * <p>
	 * Each package type has a default platform version assigned to it 
	 * <p>
	 *  
	 *  @param pkg_type: must be one of the values retured by getPackageTypesList()
	 *  
	 *  @return platform version object
	 */
	public PlatformVersion getDefaultPlatformVersion(String pkg_type) {

		if(!getPackageTypesList().contains(pkg_type)) {
			throw new InvalidIdentifierException(String.format("Invalid package type: %s", pkg_type));
		}

		String default_platform_uuid;

		try {
			default_platform_uuid = handlerFactory.getPackageHandler().getDefaultPlatform(pkg_type);
		}catch (JSONException e) {
			throw new NoDefaultPlatformException("No default platform set for package type: " + pkg_type);
		}

		if (default_platform_uuid != null) {
			Platform platform = getPlatform(default_platform_uuid);
			List<PlatformVersion> platform_versions = (List<PlatformVersion>) handlerFactory.getPlatformVersionHandler().getAll(platform);
			Collections.sort(platform_versions, new Comparator<PlatformVersion>() {
				public int compare(PlatformVersion i1, PlatformVersion i2) {
					return (i2.getName().compareTo(i1.getName()));
				}
			});

			return platform_versions.get(0);
		}else {
			return null;
		}
	}

	/**
	 * Gets the list of assessment objects that are assigned to a project
	 *  
	 *  @param project_uuid: project UUID
	 *  
	 *  @return list of assessments objects
	 */
	public List<AssessmentRun> getAllAssessments(String project_uuid) {
		Project project = getProject(project_uuid);
		return (List<AssessmentRun>) handlerFactory.getAssessmentHandler().getAllAssessments(project);
	}

	/**
	 * Gets the list of assessment objects that are assigned to a project
	 *  
	 *  @param assess_uuid: assessment UUID
	 *  @param project_uuid: project UUID
	 *  
	 *  @return assessments objects
	 *  @throws InvalidIdentifierException Invalid assessment UUI
	 */
	public AssessmentRun getAssessment(String assess_uuid, String project_uuid) {
		for (AssessmentRun arun : getAllAssessments(project_uuid)){
			if (arun.getIdentifierString().equals(assess_uuid)){
				return arun;
			}
		}
		throw new InvalidIdentifierException("Invalid assessment UUID: " + assess_uuid);
	}

	/**
	 * Delete an assessment
	 *  
	 *  @param arun: assessment run object
	 *  
	 *  @return status: deleted or not
	 *  
	 */
	public boolean deleteAssessment(AssessmentRun arun) {
		return handlerFactory.getAssessmentHandler().delete(arun);
	}

	/**
	 * Delete an assessment
	 *  
	 *  @param assess_uuid: assessment run UUID
	 *  @param project_uuid: project UUID
	 *  
	 *  @return status: deleted or not
	 *  @throws InvalidIdentifierException Invalid Assessment UUID
	 */
	public boolean deleteAssessment(String assess_uuid, String project_uuid) {

		for (AssessmentRun arun : getAllAssessments(project_uuid)){
			if (arun.getIdentifierString().equalsIgnoreCase(assess_uuid)) {
				return deleteAssessment(arun);
			}
		}
		throw new InvalidIdentifierException("Invalid Assessment UUID: " + assess_uuid);
	}

	/**
	 * Run assessments on a package with a set of tools on a set of platforms
	 *  <p>
	 *  This method creates multiple assessments on a package with 
	 *  multiple tools on multiple platforms
	 *  
	 *  @param pkg_ver_uuid: package version UUID
	 *  @param tool_uuid_list: list of tool UUIDs
	 *  @param project_uuid: project UUID
	 *  @param platform_uuid_list: list of platform UUIDs
	 *  
	 *  @return list of assessment UUIDs
	 *  @throws IncompatibleAssessmentTupleException if a tool does not support a package type
	 *  
	 */
	public List<String> runAssessment(String pkg_ver_uuid,
			List<String> tool_uuid_list,
			String project_uuid,
			List<String> platform_uuid_list) throws IncompatibleAssessmentTupleException {
		
		PackageVersion pkg_ver = getPackageVersion(pkg_ver_uuid, project_uuid);
		Project project = getProject(project_uuid);

		List<PlatformVersion> platforms = new ArrayList<PlatformVersion>();

		if (null == platform_uuid_list || platform_uuid_list.isEmpty()){
			platform_uuid_list = new ArrayList<String>(); 
			platform_uuid_list.add(getDefaultPlatformVersion(pkg_ver.getPackageThing().getType()).getUUIDString());
		}

		for (String platform_uuid: platform_uuid_list) {
			PlatformVersion platform_version = null;
			if(platform_uuid == null) {
				platform_version = getDefaultPlatformVersion(pkg_ver.getPackageThing().getType());
			}else {
				platform_version = getPlatformVersion(platform_uuid);
			}

			for (String tool_uuid: tool_uuid_list) {
				Tool tool = getTool(tool_uuid, project_uuid);
				
				if (!tool.getSupportedPkgTypes().contains(pkg_ver.getPackageThing().getType())) {
					throw new IncompatibleAssessmentTupleException(String.format("%s (%s) does not support this package type \"%s\"",
							tool.getName(),
							tool.getSupportedPkgTypes(),
							pkg_ver.getPackageThing().getType()));
				}

				if (!tool.getSupportedPlatforms().contains(platform_version.getPlatform().getName())) {
					throw new IncompatibleAssessmentTupleException(String.format("%s (%s) is not supported on this platform \"%s\"",
							tool.getName(),
							tool.getSupportedPlatforms(),
							platform_version.getName()));
				}
			}
			platforms.add(platform_version);
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

	/**
	 * Run a single assessment, on a package with a tool on a platform
	 *  
	 *  
	 *  @param pkg: package version object
	 *  @param tool: tool object
	 *  @param project: project object
	 *  @param platform: platform object
	 *  
	 *  @return assessment run object
	 *  
	 */
	public AssessmentRun runAssessment(PackageVersion pkg, Tool tool, Project project, PlatformVersion platform) {
		AssessmentRun arun = handlerFactory.getAssessmentHandler().create(project, pkg, platform, tool);
		if (handlerFactory.getRunRequestHandler().submitOneTimeRequest(arun, true)) {
			return arun;
		}else{
			return null;
		}
	}

	/**
	 * Run multiple assessments, on a package with a set of tools on a set of platforms
	 *  
	 *  
	 *  @param pkg: package version object
	 *  @param tools: list of tool object
	 *  @param project: project object
	 *  @param platform_versions: list of platform object
	 *  
	 *  @return list of assessment run objects
	 *  
	 */
	protected List<AssessmentRun> runAssessment(PackageVersion pkg, List<Tool> tools, 
			Project project, List<PlatformVersion> platform_versions) {
		List<AssessmentRun> arun_list = new ArrayList<AssessmentRun>();
		for (PlatformVersion platform_version : platform_versions) {
			for (Tool tool : tools) {
				arun_list.add(handlerFactory.getAssessmentHandler().create(project, pkg, platform_version, tool));
			}
		}
		if (handlerFactory.getRunRequestHandler().submitOneTimeRequest(arun_list, true)){
			return arun_list;
		}else {
			return null;
		}

	}

	   /**
     * Run a single assessment, on a package with a tool on a platform
     *  
     *  
     *  @param pkg: package version object
     *  @param tool: tool object
     *  @param project: project object
     *  @param platform: platform object
     *  
     *  @return assessment run object
     *  
     */
    public List<AssessmentRun> runAssessment(PackageVersion pkg, ToolVersion tool_version, Project project, 
            List<PlatformVersion> platform_versions) {
        
        List<AssessmentRun> arun_list = new ArrayList<AssessmentRun>();
        for (PlatformVersion platform_version : platform_versions) {
            arun_list.add(handlerFactory.getAssessmentHandler().create(project, pkg, platform_version, tool_version));
        }
        if (handlerFactory.getRunRequestHandler().submitOneTimeRequest(arun_list, true)) {
            return arun_list;
        }else{
            return null;
        }
    }


	/**
	 * Get all assessment results objects in a project
	 *  
	 *  
	 *  @param project_uuid: project UUID
	 *  
	 *  @return list of assessment run objects
	 *  
	 */
	protected List<? extends AssessmentResults> getAllAssessmentResults(String project_uuid) {
		Project project = getProject(project_uuid);
		return handlerFactory.getAssessmentResultHandler().getAll(project);
	}

	/**
	 * Write SCARF results from an assessment into a file
	 *  
	 *  
	 *  @param project_uuid: project UUID
	 *  @param asssess_result_uuid: asssess_result_uuid UUID
	 *  @param filepath: filepath to write to  
	 *  
	 *  @throws IOException Exceptions when writing SCARF to a file
	 */
	public void getAssessmentResults(String project_uuid, String asssess_result_uuid, String filepath) 
			throws IOException {

		for(AssessmentResults results : getAllAssessmentResults(project_uuid)){
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

	/**
	 * Get a list of all the assessment execution records associated with a project
	 *  
	 *  @param project_uuid: project UUID
	 *  
	 *  @return list of assessment record objects  
	 *  
	 */
	public List<? extends AssessmentRecord> getAllAssessmentRecords(String project_uuid) {
		Project project = getProject(project_uuid);
		return handlerFactory.getassessmentRecordHandler().getExecutionRecords(project);
	}

	/**
	 * Get a single assessment execution record of an assessment run
	 *  
	 *  @param project_uuid: project UUID
	 *  @param assessment_uuid: assessment UUID 
	 *  
  	 * @return assessment execution record
	 *  
	 */
	public AssessmentRecord getAssessmentRecord(String project_uuid, String assessment_uuid){

		for(AssessmentRecord assessment_record : getAllAssessmentRecords(project_uuid)) {
			if (assessment_record.getAssessmentRunUUID().equals(assessment_uuid)){
				return assessment_record;
			}
		}

		throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessment_uuid);
	}
	
	/**
	 * Get all the assessment execution records of an assessment run
	 *  
	 *  @param project_uuid: project UUID
	 *  @param assessment_uuid: assessment UUID 
	 *  
  	 * @return list of assessment execution records
	 *  
	 */
	public List<AssessmentRecord> getAssessmentRecords(String project_uuid, String assessment_uuid){
		List<AssessmentRecord> execution_record_list = new ArrayList<AssessmentRecord>();
		boolean found = false;
		
		for(AssessmentRecord assessment_record : getAllAssessmentRecords(project_uuid)) {
			if (assessment_record.getAssessmentRunUUID().equals(assessment_uuid)){
				execution_record_list.add(assessment_record);
			}
		}

		if (!found) {
			throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessment_uuid);
		}
		
		return execution_record_list;
	}
	
	/**
	 * Delete an assessment execution record
	 *  
	 *  @param assessment_record: assessment execution record object
	 *  
	 *  @return status: deleted or not  
	 *  
	 */
	public boolean deleteAssessmentRecord(AssessmentRecord assessment_record){
		return handlerFactory.getassessmentRecordHandler().deleteAssessmentRecord(assessment_record);
	}
	
	/**
	 * Delete assessment execution records of an assessment run
	 *  
	 *  @param project_uuid: project UUID
	 *  @param assessment_uuid: assessment UUID 
	 *  
	 *  @return status: deleted or not  
	 *  
	 */
	public boolean deleteAssessmentRecord(String assessment_uuid, String project_uuid){
		//TODO: public boolean deleteAssessmentRecord(String project_uuid, String assessment_uuid){
		boolean found = false;
		boolean not_deleted = false;
		
		for(AssessmentRecord assessment_record : getAllAssessmentRecords(project_uuid)) {
			if (assessment_record.getAssessmentRunUUID().equals(assessment_uuid)){
				if (!deleteAssessmentRecord(assessment_record)) {
					not_deleted = true;
				};
				found = true;
			}
		}

		if (!found) {
			throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessment_uuid);
		}
		return not_deleted;
	}

}
