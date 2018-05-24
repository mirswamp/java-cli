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
import org.apache.log4j.Logger;
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author      Vamshi Basupalli vamshi@cs.wisc.edu
 * @version     1.3
 */
public class SwampApiWrapper {

	public static final String SWAMP_HOST_NAME  = HandlerFactoryUtil.PD_ORIGIN_HEADER;
	private final String swampDirPath;
	private final String csaSessionObject;
	private final String rwsSessionObbject;
	private final String csaCookies;
	private final String rwsCookies;

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
	protected static final Logger LOGGER = Logger.getLogger(SwampApiWrapper.class);
	
	public Properties getProp(String filepath){
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(filepath));
		} catch (Exception e) {
		    LOGGER.error(null, e);
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

	private static boolean onWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}
	
	public static String getSwampDirPath() {
		if (onWindows()) {
			return System.getenv("LOCALAPPDATA") + File.separator + "Swamp";
		}else {
			return getPosixSwampDirPath();
		}
	}

	public static  String getPosixSwampDirPath() {
		return System.getProperty("user.home") + File.separator + ".SWAMP_SESSION";
	}

	/**
     * Main constructor
     *
     */
    public SwampApiWrapper() {
        swampDirPath = getSwampDirPath();
        csaSessionObject  = swampDirPath + File.separator + "csa_session_object.ser";
        rwsSessionObbject  = swampDirPath + File.separator + "rws_session_object.ser";
        csaCookies = swampDirPath + File.separator + "csa_session_cookies.ser";
        rwsCookies = swampDirPath + File.separator + "rws_session_cookies.ser";

        cachedPkgProjectID = "";
        cachedPkgVersionProjectID = "";
        cachedToolProjectID = "";

        sslConfig = new SSLConfiguration();
        sslConfig.setTlsVersion("TLSv1.2");
    }


	private void moveCookies() {
		
		if (onWindows()) {
			Path old_dir = Paths.get(getPosixSwampDirPath());
			if (Files.isDirectory(old_dir)) {
				Path csa_session = Paths.get(old_dir.toString(), "csa_session_object.ser");
				Path rws_session = Paths.get(old_dir.toString(), "rws_session_object.ser");
				Path csa_cookies = Paths.get(old_dir.toString(), "csa_session_cookies.ser");
				Path rws_cookies = Paths.get(old_dir.toString(), "rws_session_cookies.ser");
				
				if (Files.exists(csa_session, LinkOption.NOFOLLOW_LINKS) && 
						Files.exists(rws_session, LinkOption.NOFOLLOW_LINKS) &&
						Files.exists(csa_cookies, LinkOption.NOFOLLOW_LINKS) && 
						Files.exists(rws_cookies, LinkOption.NOFOLLOW_LINKS)) {
					
					try {
						Files.move(csa_session, Paths.get(csaSessionObject), StandardCopyOption.ATOMIC_MOVE);
						Files.move(rws_session, Paths.get(rwsSessionObbject), StandardCopyOption.ATOMIC_MOVE);
						Files.move(csa_cookies, Paths.get(csaCookies), StandardCopyOption.ATOMIC_MOVE);
						Files.move(rws_cookies, Paths.get(rwsCookies), StandardCopyOption.ATOMIC_MOVE);
					} catch (IOException e) {
					    LOGGER.error(null, e);
					}
				}
				
				try {
					Files.delete(old_dir);
				} catch (IOException e) {
				    LOGGER.error(null, e);
				}
			}
		}
	}
	
	protected final void setHost(String hostName, Proxy proxy) throws InvalidIdentifierException {

        if (hostName == null) {
            throw new InvalidIdentifierException("host_name cannot be null");
        }

        String web_server = SWAMPConfigurationLoader.getWebServiceURL(hostName, sslConfig, proxy);
		if (web_server == null) {
			web_server = hostName;
		}
		
		setRwsAddress(web_server);
		setCsaAddress(web_server);
		setOriginHeader(web_server);
		setRefereHeader(web_server);
		setHostHeader(web_server);
	}

	public static Proxy getProxy(String scheme, 
	        String host, 
	        String port,
	        String username, 
	        String password) throws MalformedURLException {
	    Proxy proxy = null;
	    
	    if (host == null) {
	       return new Proxy();
	    }
	    
	    if (scheme == null || scheme.isEmpty()) {
	        scheme = "https";
	    }
	    
	    if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) {
	        return new Proxy();
	    }
	    
	    if (port == null ) {
	        port = scheme.equals("https") ? "443" : "80";
	    }
	    
	    proxy = new Proxy(Integer.parseInt(port), host, scheme, true);
	    
	    if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
	        proxy.setUsername(username);
	        proxy.setPassword(password);
	    }
	    return proxy;
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
    public String login(String userName, String password, String hostName) throws MalformedURLException {
        Proxy proxy = getProxy();
        return login(userName, password, hostName, proxy, null); 
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
    public String login(String userName, 
            String password, 
            String hostName, 
            Proxy proxy, 
            String keystore) throws MalformedURLException {
		
        if (keystore != null) {
            sslConfig.setKeystore(keystore);
        }
        
        setHost(hostName, proxy);   
        
		handlerFactory = HandlerFactoryUtil.createHandlerFactory(getRwsAddress(),
				getCsaAddress(),
				getOriginHeader(),
				getRefereHeader(),
				getHostHeader(),
				userName,
				password,
				sslConfig,
				proxy);

		if (handlerFactory != null){
			return handlerFactory.getUserHandler().getCurrentUser().getIdentifierString();
		}

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

	/*
	 * make sure saved credential files have safe permissions, to
	 * prevent other users from hijacking SWAMP sessions.
	 */
	protected void setCredentialFilePermissions(String filepath) throws IOException {
		/*
		 * Setup the file before credentials are written to it so
		 * we can control the permission on the file
		 */
		File file = new File(filepath);
		/* create it so we can control it */
		if (!file.exists()) {
			if (!file.createNewFile()) {
				throw new IOException("Failed to create file: " + file);
			}
		}
		
		if (!onWindows()) {
			/* others can not read */
			if (!file.setReadable(false, false)) {
				throw new IOException("Failed to set o-r perm on file: " + file);
			}
			/* owner can read */
			if (!file.setReadable(true, true)) {
				throw new IOException("Failed to set u+r perm on file: " + file);
			}
			/* XXX this breaks umask */
			/* other can not write */
			if (!file.setWritable(false, false)) {
				throw new IOException("Failed to set o-w perm on file: " + file);
			}
			/* owner can write */
			if (!file.setWritable(true, true)) {
				throw new IOException("Failed to set u+w perm on file: " + file);
			}
			/* XXX this is excessive */
			/* other can not execute */
			if (!file.setExecutable(false, false)) {
				throw new IOException("Failed to set o-x perm on file: " + file);
			}
			/* owner can not execute */
			if (!file.setExecutable(false, true)) {
				throw new IOException("Failed to set u-x perm on file: " + file);
			}
		}
	}

	protected void serialize(Object obj, String filepath) throws IOException{

	    /* make sure file is secure before outputstream gets to it */
		setCredentialFilePermissions(filepath);

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

		/*
		 * Ensure existing permissions are fixed on the fly,
		 * to eliminate extended window of vulnerability.
		 * Wait for here to ensure existing creds are valid. 
		 */
		setCredentialFilePermissions(filepath);

		return obj;
	}

	/**
	 * Save SWAMP session object/cookies in ~/.SWAMP_SESSION/
	 *
	 */
	public void saveSession() {
		try {

			File dir = new File(swampDirPath);
			if (!dir.exists()) {
				if (!dir.mkdir()) {
					throw new IOException("Failed to create directory " + dir);
				}
			}

			serialize(handlerFactory.getCSASession(), csaSessionObject);
			serialize(handlerFactory.getCSASession().getClient().getContext().getCookieStore(),
				csaCookies);
			serialize(handlerFactory.getRWSSession(),  rwsSessionObbject);
			serialize(handlerFactory.getRWSSession().getClient().getContext().getCookieStore(),
				rwsCookies);
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
		File file = new File(swampDirPath);
		if (file.isDirectory()){
			File[] fileList = file.listFiles();
			for (File f : fileList) {
				f.delete();
			}
			//file.delete();
		}
	}

	/**
	 * Reads and starts SWAMP session object/cookies in ~/.SWAMP_SESSION/
	 *
	 * @return true if valid, non-expired sessions instantiated 
	 */
	public boolean restoreSession() throws SessionExpiredException, SessionRestoreException {

		moveCookies();
		
		if (!(SwampApiWrapper.fileExists(csaSessionObject) && SwampApiWrapper.fileExists(rwsSessionObbject) &&
				SwampApiWrapper.fileExists(csaCookies) && SwampApiWrapper.fileExists(rwsCookies))) {
			throw new SessionRestoreException("Could not locate session objects and cookies to recover the session");
		}

		try {
			Session csa_session = (Session)deserialize(csaSessionObject);
			Session rws_session = (Session)deserialize(rwsSessionObbject);
			handlerFactory = new HandlerFactory(rws_session, csa_session);

			CookieStore csa_cookie_store = (CookieStore)deserialize(csaCookies);
			CookieStore rws_cookie_store = (CookieStore)deserialize(rwsCookies);

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
     * @param pkgLang any one of: [Java, C, C++, Ruby, 
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
    public String getPkgTypeString(String pkgLang,
            String pkgLangVersion,
            String pkgBuildSys,
            String packageType) {

        String pkg_type = null;

        if (pkgBuildSys.toLowerCase().equals("android-apk")) {
            pkg_type = "Android .apk";
        }else {
            if (pkgLang != null) {
                pkgLang = pkgLang.split(" ")[0];
            }
            switch (pkgLang){
            case "Java":
                if(pkgBuildSys.toLowerCase().startsWith("android")) {
                    pkg_type = "Android Java Source Code";
                }else if(pkgBuildSys.toLowerCase().equals("java-bytecode")) {
                    if (pkgLangVersion.toLowerCase().startsWith("java-7")) {
                        pkg_type = "Java 7 Bytecode";
                    }else {
                        pkg_type = "Java 8 Bytecode";
                    }
                }else {
                    if (pkgLangVersion.toLowerCase().startsWith("java-7")) {
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
                if (packageType == null) {
                    pkg_type = "Ruby";
                }else if (packageType.toLowerCase().equals("rails")) {
                    pkg_type = "Ruby on Rails";
                }else if (packageType.toLowerCase().equals("sinatra")) {
                    pkg_type = "Ruby Sinatra";
                }else if (packageType.toLowerCase().equals("padrino")) {
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
     * @param pkgLang any one of: [Java, C, C++, Ruby, 
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
    public Integer getPkgTypeId(String pkgLang,
            String pkgLangVersion,
            String pkgBuildSys,
            String packageType) {
        String pkg_type = getPkgTypeString(pkgLang, pkgLangVersion, pkgBuildSys, packageType);
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
     *@param pkgConf properties object of a package.conf file
     *@return hash-map for package version configuration 
     */
    protected ConversionMapImpl getPkgConfMap(Properties pkgConf) {
        ConversionMapImpl map = new ConversionMapImpl();
        map.put("android_sdk_target", pkgConf.getProperty("android-sdk-target", null));
        map.put("android_lint_target", pkgConf.getProperty("android-lint-target", null));
        map.put("android_maven_plugin", pkgConf.getProperty("android-maven-plugin", null));
        map.put("android_redo_build", pkgConf.getProperty("android-redo-build", "false"));
        
        map.put("ant-version", pkgConf.getProperty("ant-version", null));

        map.put("build_cmd", pkgConf.getProperty("build-cmd", null));
        map.put("build_dir", pkgConf.getProperty("build-dir", null));
        map.put("build_file", pkgConf.getProperty("build-file", null));
        map.put("build_opt", pkgConf.getProperty("build-opt", null));
        map.put("build_system", pkgConf.getProperty("build-sys", null));
        map.put("build_target", pkgConf.getProperty("build-target", null));
        
        map.put("config_cmd", pkgConf.getProperty("config-cmd", null));
        map.put("config_opt", pkgConf.getProperty("config-opt", null));
        map.put("config_dir", pkgConf.getProperty("config-dir", null));

        map.put("use_gradle_wrapper", pkgConf.getProperty("gradle-wrapper", "false"));
        map.put("maven_version", pkgConf.getProperty("maven_version", null));
        
        map.put("version_string", pkgConf.getProperty("package-version"));
        map.put("source_path", pkgConf.getProperty("package-dir"));

        map.put("language_version", pkgConf.getProperty("package-language-version", null));
        map.put("bytecode_class_path", pkgConf.getProperty("package-classpath", null));
        map.put("bytecode_aux_class_path", pkgConf.getProperty("package-auxclasspath", null));
        map.put("bytecode_source_path", pkgConf.getProperty("package-srcdir", null));

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
	public Project getProject(String projectUuid) {
		Project project = getAllProjects().get(projectUuid);
		if (project == null) {
			throw new InvalidIdentifierException("Invalid project UUID: " + projectUuid);
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
	public Project getProjectFromName (String projectName){
		for (Project project : getProjectsList()){	
			if (project.getFullName().equals(projectName)){
				return project;
			}
		}
		throw new InvalidNameException("Invalid project name: " + projectName);
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
	public String uploadNewPackage(String pkgConfFile, 
			String pkgArchiveFile, 
			String projectUuid,
			Map<String, String> osDepMap) {
		PackageHandler<? extends PackageThing> pkg_handler = handlerFactory.getPackageHandler();
		Properties pkg_conf = getProp(pkgConfFile);

		PackageThing pkg_thing = pkg_handler.create(pkg_conf.getProperty("package-short-name"),
				pkg_conf.getProperty("package-description", "No Description Available"),
				pkg_conf.getProperty("external-url", null),
				getPkgTypeId(pkg_conf.getProperty("package-language"),
						pkg_conf.getProperty("package-language-version", ""),
						pkg_conf.getProperty("build-sys"),
						pkg_conf.getProperty("package-type")),
				pkg_conf.getProperty("package-language"));

		ConversionMapImpl map = getPkgConfMap(pkg_conf);
		map.put("project_uuid", projectUuid);

		PackageVersion pkg_version = handlerFactory.getPackageVersionHandler().create(pkg_thing,
				new File(pkgArchiveFile),
				map);

		addPackageDependencies(pkg_version, osDepMap);

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
	public String uploadPackageVersion(String pkgConfFile, 
			String pkgArchiveFile, 
			String projectUuid,
			Map<String, String> osDepMap) {
		Properties pkg_conf = getProp(pkgConfFile);
		PackageThing pkg_thing = null;

		for(PackageThing pkg : getAllPackages(projectUuid).values()){
			if (pkg.getName().equals(pkg_conf.getProperty("package-short-name"))){
				pkg_thing = pkg;
				break;
			}
		}

		if (pkg_thing == null){
			return uploadNewPackage(pkgConfFile, pkgArchiveFile, projectUuid, osDepMap);
		}else{
			ConversionMapImpl map = getPkgConfMap(pkg_conf);
			map.put("project_uuid", projectUuid);

			PackageVersion pkg_version = handlerFactory.getPackageVersionHandler().create(pkg_thing,
					new File(pkgArchiveFile),
					map);
			addPackageDependencies(pkg_version, osDepMap);
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

	public String uploadPackage(String pkgConfFile, 
			String pkgArchiveFile,
			String projectUuid,
			Map<String, String> osDepMap,
			boolean isNew) throws InvalidIdentifierException {

		getProject(projectUuid);

		if(isNew) {
			return uploadNewPackage(pkgConfFile, pkgArchiveFile, projectUuid, osDepMap);
		}else{
			return uploadPackageVersion(pkgConfFile, pkgArchiveFile, projectUuid, osDepMap);
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
	protected void addPackageDependencies(PackageVersion packageVersion, Map<String, String> osDepMap) {
		if (osDepMap != null){
			
			ConversionMapImpl dep_map = new ConversionMapImpl();

			for (PlatformVersion platform_version : getAllPlatformVersionsList()) {
				String deps = osDepMap.get(platform_version.getDisplayString());
				if (deps != null) {
					dep_map.put(platform_version.getIdentifierString(), deps);
				}
			}
			handlerFactory.getPackageVersionHandler().addPackageVersionDependencies(packageVersion, dep_map);
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
	public boolean deletePackageVersion(String pkgVerUuid, String projectUuid) throws InvalidIdentifierException {

		getProject(projectUuid);

		for(PackageVersion pkg_ver : getAllPackageVersions(projectUuid).values()){
			if (pkg_ver.getUUIDString().equals(pkgVerUuid)){
				return deletePackageVersion(pkg_ver);
			}
		}

		throw new InvalidIdentifierException("Invalid package version UUID: " + pkgVerUuid);
	}

	/**
	 * Delete a version of a package
	 *
	 * @param pkg_ver_uuid: package version UUID
	 * 
	 * @return status: deleted or not deleted
	 * @throws InvalidIdentifierException if UUID provided is not a valid one
	 */
	public boolean deletePackageVersion(String pkgVerUuid) throws InvalidIdentifierException {

		for(PackageVersion pkg_ver : getAllPackageVersions().values()){
			if (pkg_ver.getUUIDString().equals(pkgVerUuid)){
				return deletePackageVersion(pkg_ver);
			}
		}

		throw new InvalidIdentifierException("Invalid package version UUID: " + pkgVerUuid);
	}

	/**
	 * Delete a version of a package
	 *
	 * @param pkg_ver: package version object
	 * 
	 * @return status: deleted or not deleted
	 */
	public boolean deletePackageVersion(PackageVersion pkgVer) throws InvalidIdentifierException {
		boolean ret_val = handlerFactory.getPackageVersionHandler().deletePackageVersion(pkgVer);
		if(ret_val) {
			//packageVersionMap = null;
			packageVersionMap.remove(pkgVer.getIdentifierString());
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
	public boolean deletePackage(String pkgUuid, String projectUuid) throws InvalidIdentifierException {

		getProject(projectUuid);

		for(PackageThing pkg : getAllPackages(projectUuid).values()){
			if (pkg.getUUIDString().equals(pkgUuid)){
				return deletePackage(pkg);
			}
		}

		throw new InvalidIdentifierException("Invalid package UUID: " + pkgUuid);
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
	protected Map<String, PackageThing> getAllPackages(String projectUuid) {
		if ((packageMap == null) || (!stringsAreEqual(cachedPkgProjectID, projectUuid))) {
			cachedPkgProjectID = projectUuid;
			packageMap = new HashMap<String, PackageThing>();
			if (projectUuid == null) {
				for (PackageThing pkg : handlerFactory.getPackageHandler().getAll()){
					packageMap.put(pkg.getUUIDString(), pkg);
				}
			}else {
				for (PackageThing pkg : handlerFactory.getPackageHandler().getAll(getProject(projectUuid))){
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
	public List<PackageThing> getPackagesList(String projectUuid) {
		List<PackageThing> pkg_list = new ArrayList<PackageThing>(getAllPackages(projectUuid).values());

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
	protected Map<String, PackageVersion> getAllPackageVersions(String projectUuid) {
		if ((packageVersionMap == null) || (!stringsAreEqual(cachedPkgVersionProjectID, projectUuid))) {
			cachedPkgVersionProjectID = projectUuid;
			packageVersionMap = new HashMap<String, PackageVersion>();
			if (projectUuid == null) {
				for (PackageThing pkg : handlerFactory.getPackageHandler().getAll()){
					for (PackageVersion pkg_ver : handlerFactory.getPackageVersionHandler().getAll(pkg)) {
						packageVersionMap.put(pkg_ver.getUUIDString(), pkg_ver);
					}
				}
			}else {
				for (PackageThing pkg : handlerFactory.getPackageHandler().getAll(getProject(projectUuid))){
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
	public List<PackageVersion> getPackageVersionsList(String projectUuid) {
		List<PackageVersion> pkg_list = new ArrayList<PackageVersion>(getAllPackageVersions(projectUuid).values());

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
	public PackageVersion getPackageVersion(String pkgVerUuid, String projectUuid) {
		PackageVersion pkg_ver = getAllPackageVersions(projectUuid).get(pkgVerUuid);

		if (pkg_ver == null) {
			throw new InvalidIdentifierException("Invalid Package Version UUID: " + pkgVerUuid);
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
	protected Map<String, Tool> getAllTools(String projectUuid) throws InvalidIdentifierException {

		if ((toolMap == null) || (!stringsAreEqual(cachedToolProjectID, projectUuid))) {
			cachedToolProjectID = projectUuid;
			toolMap = new HashMap<String, Tool>();
			for (Tool tool : handlerFactory.getToolHandler().getAll()) {
				//if (tool.getPolicyCode() == null){    //FIXME: This is temporary
					toolMap.put(tool.getIdentifierString(), tool);
				//}
			}

			if (projectUuid != null){
				Project proj = getProject(projectUuid);

				for (Tool tool : handlerFactory.getToolHandler().getAll(proj)) {
					//if (tool.getPolicyCode() == null){    //FIXME: This is temporary
						toolMap.put(tool.getIdentifierString(), tool);
					//}
				}
			}else {
			    for (Tool tool : handlerFactory.getToolHandler().getAll()) {
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
	public List<Tool> getTools(String pkgType, String projectUuid) throws InvalidIdentifierException {

		if (!getPackageTypes().containsKey(pkgType)){
			throw new InvalidIdentifierException(String.format("Package type '%s' is invalid, it must be one of %s",
					pkgType, getPackageTypes().keySet()));
		}

		List<Tool> tool_list = new ArrayList<>();
		Map<String, Tool> tool_map = getAllTools(projectUuid);
		for(String tool_uuid : tool_map.keySet()){
			if (tool_map.get(tool_uuid).getSupportedPkgTypes().contains(pkgType)){
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
	public Tool getTool(String toolUuid, String projectUuid) throws InvalidIdentifierException {
		Tool tool = getAllTools(projectUuid).get(toolUuid);
		if (tool == null){
			throw new InvalidIdentifierException("Invalid Tool UUID: " + toolUuid);
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
	public Tool getToolFromName (String toolName, String projectUuid) throws InvalidIdentifierException {
		Map<String,Tool> tool_list = getAllTools(projectUuid);
		Iterator<Tool> tool_iterator = tool_list.values().iterator();
		while (tool_iterator.hasNext()){
			Tool next_tool = tool_iterator.next();
			if (next_tool.getName().equals(toolName)){
				return next_tool;
			}
		}
		return null;
	}

	//Code copied from https://stackoverflow.com/questions/41198379/sorting-version-numbers
	public int mycompare(String o1, String o2) {

	    if (o1 == null && o2 == null) {
	        return 0;
	    } else if (o1 == null && o2 != null) {
	        return -1;
	    } else if (o1 != null && o2 == null) {
	        return 1;
	    } else {
	        if (o1.length() == 0 && o2.length() == 0) {
	            return 0;
	        } else if (o1.length() == 0 && o2.length() > 0) {
	            return -1;
	        } else if (o1.length() > 0 && o2.length() == 0) {
	            return 1;
	        } else {
	            return compareVersion(o1, o2);
	        }
	    }
	}

	//Code copied from https://stackoverflow.com/questions/41198379/sorting-version-numbers
	public static int compareVersion(String version1, String version2) {
	    if (version1.contains(" ")) {
	        version1 = version1.substring(0, version1.indexOf(" "));
	    }

	    if (version2.contains(" ")) {
	        version2 = version2.substring(0, version2.indexOf(" "));
	    }

	    String[] arr1 = version1.split("\\.");
	    String[] arr2 = version2.split("\\.");

	    try {

	        int i = 0;
	        while (i < arr1.length || i < arr2.length) {
	            if (i < arr1.length && i < arr2.length) {
	                if (Integer.parseInt(arr1[i]) < Integer.parseInt(arr2[i])) {
	                    return -1;
	                } else if (Integer.parseInt(arr1[i]) > Integer.parseInt(arr2[i])) {
	                    return 1;
	                } else if (Integer.parseInt(arr1[i]) == Integer.parseInt(arr2[i])) {
	                    int result = specialCompare(version1, version2);
	                    if (result != 0) {
	                        return result;
	                    }
	                }
	            } else if (i < arr1.length) {
	                if (Integer.parseInt(arr1[i]) != 0) {
	                    return 1;
	                }
	            } else if (i < arr2.length) {
	                if (Integer.parseInt(arr2[i]) != 0) {
	                    return -1;
	                }
	            }

	            i++;
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return 0;
	}

    //Code copied from https://stackoverflow.com/questions/41198379/sorting-version-numbers
	public static int specialCompare(String str1, String str2) {	    
	    String[] arr1 = str1.split("\\.");
	    String[] arr2 = str2.split("\\.");
	    for (int i = 1; i < arr1.length; i++) {
	        if (Integer.parseInt(arr1[i]) != 0) {
	            return 0;
	        }
	    }
	    for (int j = 1; j < arr2.length; j++) {
	        if (Integer.parseInt(arr2[j]) != 0) {
	            return 0;
	        }
	    }
	    if (arr1.length < arr2.length) {
	        return -1;
	    } else {
	        return 1;
	    }
	}

	/**
     * Get a list of tools provided package type and project uuid (for project specific tools)
     *  
     *  @param pkg_type: should be one of the Key return by the API getPackageTypes
     *  
     *  @return list of tool objects
     */
    @SuppressWarnings("unchecked")
    public List<ToolVersion> getToolVersions(Tool tool) throws InvalidIdentifierException {
        List<ToolVersion>  tool_versions = (List<ToolVersion>)handlerFactory.getToolVersionHandler().getAll(tool);
        
        Collections.sort(tool_versions, new Comparator<ToolVersion>() {
            public int compare(ToolVersion i1, ToolVersion i2) {
                //return (i2.getVersion().compareTo(i1.getVersion()));
                return mycompare(i2.getVersion(), i1.getVersion());
            }
        });
        
        return tool_versions;
    }


	/**
	 * Get the platform object given the platform UUID
	 *  
	 *  @param platform_uuid: platform UUID
	 *  
	 *  @return platform object
	 */
	public Platform getPlatform(String platformUuid) {
		return getAllPlatforms().get(platformUuid);
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
	public PlatformVersion getPlatformVersion(String platformVersionUuid) {
		PlatformVersion platform = getAllPlatformVersions().get(platformVersionUuid);
		if (platform == null) {
			throw new InvalidIdentifierException("Invalid Platform UUID: " + platformVersionUuid);
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

	public PlatformVersion getPlatformVersionFromName (String platformVersionName) throws InvalidNameException  {
		Map<String, PlatformVersion> platform_version_list = getAllPlatformVersions();
		Iterator<PlatformVersion> platform_iterator = platform_version_list.values().iterator();
		while (platform_iterator.hasNext()){
			PlatformVersion next_platform = platform_iterator.next();
			if (next_platform.getDisplayString().equals(platformVersionName)){
				return next_platform;
			}
		}

		throw new InvalidNameException(String.format("Platform %s does not exist.\n", platformVersionName));
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
	public List<PlatformVersion> getSupportedPlatformVersions(String toolUuid, 
			String projectUuid) {
		Tool tool = getTool(toolUuid, projectUuid);
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
	public PlatformVersion getDefaultPlatformVersion(String pkgType) {

		if(!getPackageTypesList().contains(pkgType)) {
			throw new InvalidIdentifierException(String.format("Invalid package type: %s", pkgType));
		}

		String default_platform_uuid;

		try {
			default_platform_uuid = handlerFactory.getPackageHandler().getDefaultPlatform(pkgType);
		}catch (JSONException e) {
			throw new NoDefaultPlatformException("No default platform set for package type: " + pkgType);
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
	public List<AssessmentRun> getAllAssessments(String projectUuid) {
		Project project = getProject(projectUuid);
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
	public AssessmentRun getAssessment(String assessUuid, String projectUuid) {
		for (AssessmentRun arun : getAllAssessments(projectUuid)){
			if (arun.getIdentifierString().equals(assessUuid)){
				return arun;
			}
		}
		throw new InvalidIdentifierException("Invalid assessment UUID: " + assessUuid);
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
	public boolean deleteAssessment(String assessUuid, String projectUuid) {

		for (AssessmentRun arun : getAllAssessments(projectUuid)){
			if (arun.getIdentifierString().equalsIgnoreCase(assessUuid)) {
				return deleteAssessment(arun);
			}
		}
		throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessUuid);
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
	public List<String> runAssessment(String pkgVerUuid,
			List<String> toolUuidList,
			String projectUuid,
			List<String> platformUuidList) throws IncompatibleAssessmentTupleException {
		
		PackageVersion pkg_ver = getPackageVersion(pkgVerUuid, projectUuid);
		Project project = getProject(projectUuid);

		List<PlatformVersion> platforms = new ArrayList<PlatformVersion>();

		if (null == platformUuidList || platformUuidList.isEmpty()){
			platformUuidList = new ArrayList<String>(); 
			platformUuidList.add(getDefaultPlatformVersion(pkg_ver.getPackageThing().getType()).getUUIDString());
		}

		for (String platform_uuid: platformUuidList) {
			PlatformVersion platform_version = null;
			if(platform_uuid == null) {
				platform_version = getDefaultPlatformVersion(pkg_ver.getPackageThing().getType());
			}else {
				platform_version = getPlatformVersion(platform_uuid);
			}

			for (String tool_uuid: toolUuidList) {
				Tool tool = getTool(tool_uuid, projectUuid);
				
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
		for (String tool_uuid: toolUuidList) {
			tools.add(getTool(tool_uuid, projectUuid));
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
			Project project, List<PlatformVersion> platformVersions) {
		List<AssessmentRun> arun_list = new ArrayList<AssessmentRun>();
		for (PlatformVersion platform_version : platformVersions) {
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
    public List<AssessmentRun> runAssessment(PackageVersion pkg, ToolVersion toolVersion, Project project, 
            List<PlatformVersion> platformVersions) {
        
        List<AssessmentRun> arun_list = new ArrayList<AssessmentRun>(platformVersions.size());
        for (PlatformVersion platform_version : platformVersions) {
            arun_list.add(handlerFactory.getAssessmentHandler().create(project, pkg, platform_version, toolVersion));
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
	protected List<? extends AssessmentResults> getAllAssessmentResults(String projectUuid) {
		Project project = getProject(projectUuid);
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
	public boolean getAssessmentResults(String projectUuid, String asssessResultUuid, String filepath) 
			throws IOException {

		for(AssessmentResults results : getAllAssessmentResults(projectUuid)){
			if (results.getUUIDString().equals(asssessResultUuid)) {
				ByteArrayOutputStream data = (ByteArrayOutputStream)handlerFactory.getAssessmentResultHandler().getScarfResults(results);
				if (data != null) {
					OutputStream outputStream = new FileOutputStream(filepath);
					data.writeTo(outputStream);
					data.close();
					outputStream.close();
				}
				return true;
			}
		}
		
		throw new InvalidIdentifierException("Invalid Assessment Results UUID: " + asssessResultUuid);
	}

	   /**
     * Write SCARF results from an assessment into a file
     *  
     *  @param asssess_result_uuid: asssess_result_uuid UUID
     *  @param filepath: filepath to write to  
     *  
     *  @throws IOException Exceptions when writing SCARF to a file
     */
    public boolean getAssessmentResults(String asssessResultUuid, String filepath) 
            throws IOException {
        
        for (Project project : getProjectsList()) {
            for(AssessmentResults results : getAllAssessmentResults(project.getIdentifierString())){
                if (results.getUUIDString().equals(asssessResultUuid)) {
                    ByteArrayOutputStream data = (ByteArrayOutputStream)handlerFactory.getAssessmentResultHandler().getScarfResults(results);
                    if (data != null) {
                        OutputStream outputStream = new FileOutputStream(filepath);
                        data.writeTo(outputStream);
                        data.close();
                        outputStream.close();
                    }
                    return true;
                }
            }
        }
        
        throw new InvalidIdentifierException("Invalid Assessment Results UUID: " + asssessResultUuid);
    }
	/**
	 * Get a list of all the assessment execution records associated with a project
	 *  
	 *  @param project_uuid: project UUID
	 *  
	 *  @return list of assessment record objects  
	 *  
	 */
	public List<? extends AssessmentRecord> getAllAssessmentRecords(String projectUuid) {
		Project project = getProject(projectUuid);
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
	public AssessmentRecord getAssessmentRecord(String projectUuid, String assessmentUuid){

		for(AssessmentRecord assessment_record : getAllAssessmentRecords(projectUuid)) {
			if (assessment_record.getAssessmentRunUUID().equals(assessmentUuid)){
				return assessment_record;
			}
		}

		throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessmentUuid);
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
	public List<AssessmentRecord> getAssessmentRecords(String projectUuid, String assessmentUuid){
		List<AssessmentRecord> execution_record_list = new ArrayList<AssessmentRecord>();
		boolean found = false;
		
		for(AssessmentRecord assessment_record : getAllAssessmentRecords(projectUuid)) {
			if (assessment_record.getAssessmentRunUUID().equals(assessmentUuid)){
				execution_record_list.add(assessment_record);
			}
		}

		if (!found) {
			throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessmentUuid);
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
	public boolean deleteAssessmentRecord(AssessmentRecord assessmentRecord){
		return handlerFactory.getassessmentRecordHandler().deleteAssessmentRecord(assessmentRecord);
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
	public boolean deleteAssessmentRecord(String assessmentUuid, String projectUuid){
		//TODO: public boolean deleteAssessmentRecord(String project_uuid, String assessment_uuid){
		boolean found = false;
		boolean not_deleted = false;
		
		for(AssessmentRecord assessment_record : getAllAssessmentRecords(projectUuid)) {
			if (assessment_record.getAssessmentRunUUID().equals(assessmentUuid)){
				if (!deleteAssessmentRecord(assessment_record)) {
					not_deleted = true;
				};
				found = true;
			}
		}

		if (!found) {
			throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessmentUuid);
		}
		return not_deleted;
	}

}
