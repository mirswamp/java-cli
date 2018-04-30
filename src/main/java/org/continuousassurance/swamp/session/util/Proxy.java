package org.continuousassurance.swamp.session.util;

import java.io.Serializable;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 2/8/18 at  10:02 AM
 */

public class Proxy implements Serializable{
    private int port = -1;
    private String host = "";
    private String scheme = "";
    private boolean configured = false;
	private String username = null;
	private String password = null;
	
    public Proxy() {
    		this(-1, "", "", false);
	}
    
    public Proxy(int port, String host, String scheme, boolean configured) {
		super();
		this.port = port;
		this.host = host;
		this.scheme = scheme;
		this.configured = configured;
		username = null;
		password = null;
	}

    public Proxy(int port, String host, String scheme, String username, String password, boolean configured) {
        super();
        this.port = port;
        this.host = host;
        this.scheme = scheme;
        this.configured = configured;
        this.username = username;
        this.password = password;        
    }
    
	public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public boolean isConfigured() {
		return configured;
	}

	public void setConfigured(boolean configured) {
		this.configured = configured;
	}

    public String toString() {
        if (isConfigured()) {
            return String.format("%s://%s:%s@%s:%d", 
                    getScheme(),
                    getUsername(),
                    "*****", //getPassword(),
                    getHost(),
                    getPort());
        }else {
            return "";
        }
        
    }
}
