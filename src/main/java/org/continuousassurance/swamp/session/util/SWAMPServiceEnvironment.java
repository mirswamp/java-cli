package org.continuousassurance.swamp.session.util;

import edu.uiuc.ncsa.security.core.util.AbstractEnvironment;
import edu.uiuc.ncsa.security.util.ssl.SSLConfiguration;
import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.session.handlers.HandlerFactory;
import org.continuousassurance.swamp.session.storage.*;
import org.continuousassurance.swamp.util.HandlerFactoryUtil;

import java.net.URI;
import java.util.Map;


/**
 * This represents the current runtime environment for a SWAMP instance. There may be multiple
 * SWAMP instances active and each will have its own service enviornment.
 * <p>Created by Jeff Gaynor<br>
 * on 4/11/16 at  10:05 AM
 */
public class SWAMPServiceEnvironment extends AbstractEnvironment {
    public SSLConfiguration getSslConfiguration() {
        return sslConfiguration;
    }

    public void setSslConfiguration(SSLConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    SSLConfiguration sslConfiguration;
    HandlerFactory handlerFactory;
    String username;
    String password;
    URI rwsAddress;
    URI csaAddress;
    Map<String, String> headers;
    ProjectStore<? extends Project> projectStore;
    PackageStore<? extends PackageThing> packageStore;
    PackageVersionStore<? extends PackageVersion> packageVersionStore;
    ToolStore<? extends Tool> toolStore;

    public SWAMPServiceEnvironment(URI rwsAddress,
                                   URI csaAddress,
                                   String username,
                                   String password,
                                   Map<String, String> headers,
                                   SSLConfiguration sslConfiguration,
                                   Proxy proxy
    ) {
        this.csaAddress = csaAddress;
        this.headers = headers;
        this.password = password;
        this.rwsAddress = rwsAddress;
        this.username = username;
        this.sslConfiguration = sslConfiguration;
        this.proxy = proxy;
    }


    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    Proxy proxy;
    /**
     * This can only be invoked once the class has been created with all of the correct arguments.
     *
     * @return
     */
    public HandlerFactory getHandlerFactory() {
        if (handlerFactory == null) {
            handlerFactory = HandlerFactoryUtil.createHandlerFactory(
                    getRwsAddress().toString(),
                    getCsaAddress().toString(),
                    getHeaders().get(SWAMPConfigTags.ORIGIN_HEADER_TAG),
                    getHeaders().get(SWAMPConfigTags.REFERER_HEADER_TAG),
                    getHeaders().get(SWAMPConfigTags.HOST_HEADER_TAG),
                    getUsername(),
                    getPassword(),
                    sslConfiguration,
                    getProxy()
            );
        }
        return handlerFactory;
    }

    public URI getCsaAddress() {
        return csaAddress;
    }

    public void setCsaAddress(URI csaAddress) {
        this.csaAddress = csaAddress;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public URI getRwsAddress() {
        return rwsAddress;
    }

    public void setRwsAddress(URI rwsAddress) {
        this.rwsAddress = rwsAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ProjectStore<? extends Project> getProjectStore() {
        if (projectStore == null) {
            projectStore = new ProjectStore<>(getHandlerFactory().getProjectHandler());
        }
        return projectStore;
    }

    public PackageStore<? extends PackageThing> getPackageStore() {
        if (packageStore == null) {
            packageStore = new PackageStore<>(getHandlerFactory().getPackageHandler());
        }
        return packageStore;
    }

    public PackageVersionStore<? extends PackageVersion> getPackageVersionStore() {
        if (packageVersionStore == null) {
            packageVersionStore = new PackageVersionStore<>(getHandlerFactory().getPackageVersionHandler());
        }
        return packageVersionStore;
    }

    public ToolStore<? extends Tool> getToolStore() {
        if (toolStore == null) {
            toolStore = new ToolStore<>(getHandlerFactory().getToolHandler());
        }
        return toolStore;
    }

    PlatformStore platformStore = null;

    public PlatformStore<? extends Platform> getPlatformStore() {
        if (platformStore == null) {
            platformStore = new PlatformStore(getHandlerFactory().getPlatformHandler());
        }
        return platformStore;
    }
}
