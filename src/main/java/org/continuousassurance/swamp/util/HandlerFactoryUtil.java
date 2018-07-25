package org.continuousassurance.swamp.util;

import org.continuousassurance.swamp.api.AssessmentRun;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.PackageVersion;
import org.continuousassurance.swamp.api.Platform;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.api.RunRequest;
import org.continuousassurance.swamp.api.RunRequestSchedule;
import org.continuousassurance.swamp.api.Tool;
import org.continuousassurance.swamp.api.User;
import org.continuousassurance.swamp.session.SWAMPHttpClient;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.AssessmentRunHandler;
import org.continuousassurance.swamp.session.handlers.HandlerFactory;
import org.continuousassurance.swamp.session.handlers.PackageHandler;
import org.continuousassurance.swamp.session.handlers.PackageVersionHandler;
import org.continuousassurance.swamp.session.handlers.PlatformHandler;
import org.continuousassurance.swamp.session.handlers.ProjectHandler;
import org.continuousassurance.swamp.session.handlers.RunRequestHandler;
import org.continuousassurance.swamp.session.handlers.RunRequestScheduleHandler;
import org.continuousassurance.swamp.session.handlers.ToolHandler;
import org.continuousassurance.swamp.session.handlers.UserHandler;
import org.continuousassurance.swamp.session.util.Proxy;

import edu.uiuc.ncsa.security.util.ssl.SSLConfiguration;

public class HandlerFactoryUtil {
    /*
    // Production addresses and headers
    public static final String PD_RWS_ADDRESS = "https://swa-rws-pd-01.mir-swamp.org/";
    public static final String PD_CSA_ADDRESS = "https://swa-csaweb-pd-01.mir-swamp.org/";
    public static final String PD_ORIGIN_HEADER = "https://www.mir-swamp.org";
    public static final String PD_HOST_HEADER = "swa-rws-pd-01.mir-swamp.org";
    public static final String PD_REFERER_HEADER = "https://www.mir-swamp.org/";

    // Development addresses and headers
    public static final String DT_RWS_ADDRESS = "https://swa-rws-dt-01.cosalab.org";
    public static final String DT_CSA_ADDRESS = "https://swa-csaweb-dt-02.cosalab.org";
    public static final String DT_ORIGIN_HEADER = "https://dt.cosalab.org/";
    public static final String DT_HOST_HEADER = "swa-rws-dt-01.cosalab.org";
    public static final String DT_REFERER_HEADER = "https://dt.cosalab.org/";

    // Integration addresses and headers.
    public static final String IT_RWS_ADDRESS = "https://swa-rws-it-01.cosalab.org/";
    public static final String IT_CSA_ADDRESS = "https://swa-csaweb-it-01.cosalab.org/";
    public static final String IT_ORIGIN_HEADER = "https://it.cosalab.org/";
    public static final String IT_HOST_HEADER = "swa-rws-it-01.cosalab.org";
    public static final String IT_REFERER_HEADER = "https://it.cosalab.org/";
     */
    protected static HandlerFactory handlerFactory;

    public static final String PD_ORIGIN_HEADER = "https://www.mir-swamp.org";

    /**
     * Create a handler factory with no proxy.
     * @param rwsServer
     * @param apiServer
     * @param originHeader
     * @param refererHeader
     * @param hostHeader
     * @param username
     * @param password
     * @param sslConfiguration
     * @return
     */
    public static HandlerFactory createHandlerFactory(String apiServer,
            String originHeader,
            String refererHeader,
            String hostHeader,
            String username,
            String password,
            SSLConfiguration sslConfiguration) {
        Proxy proxy = new Proxy();
        //proxy.configured = false;
        return createHandlerFactory(apiServer,
                originHeader,
                refererHeader,
                hostHeader,
                username,
                password,
                sslConfiguration,
                proxy);
    }

    public static HandlerFactory createHandlerFactory(String apiServer,
            String originHeader,
            String refererHeader,
            String hostHeader,
            String username,
            String password,
            SSLConfiguration sslConfiguration,
            Proxy proxy) {
        Session csaSession = realLogon(apiServer, hostHeader, originHeader, refererHeader, username, password, sslConfiguration, proxy);

        HandlerFactory hf = new HandlerFactory(csaSession);
        setHandlerFactory(hf);
        return hf;

    }

    public static AssessmentRunHandler<? extends AssessmentRun> getAssessmentH() {
        return getHandlerFactory().getAssessmentHandler();
    }

    public static HandlerFactory getHandlerFactory() {
        if(handlerFactory == null){
            throw new IllegalStateException("The handler factory has not been initialized yet.");
        }
        return handlerFactory;
    }

    public static PackageHandler<? extends PackageThing> getPackageH() {
        return HandlerFactoryUtil.getHandlerFactory().getPackageHandler();
    }


    public static PackageVersionHandler<? extends PackageVersion> getPackageVersionH() {
        return HandlerFactoryUtil.getHandlerFactory().getPackageVersionHandler();
    }

    public static PlatformHandler<? extends Platform> getPlatformH() {
        return HandlerFactoryUtil.getHandlerFactory().getPlatformHandler();
    }

    public static ProjectHandler<? extends Project> getProjectH() {
        return getHandlerFactory().getProjectHandler();
    }

    public static RunRequestHandler<? extends RunRequest> getRunRequestH() {
        return getHandlerFactory().getRunRequestHandler();
    }

    public static RunRequestScheduleHandler<? extends RunRequestSchedule> getRunRequestScheduleH() {
        return getHandlerFactory().getRunRequestScheduleHandler();
    }

    public static ToolHandler<? extends Tool> getToolH() {
        return getHandlerFactory().getToolHandler();
    }

    public static UserHandler<? extends User> getUserH() {
        return HandlerFactoryUtil.getHandlerFactory().getUserHandler();
    }

    /**
     * This will create a session without a proxy.
     * @param host
     * @param hostHeader
     * @param originHeader
     * @param refererHeader
     * @param username
     * @param password
     * @param requireSecureCookies
     * @param sslConfiguration
     * @return
     */
    public static Session realLogon(String host,
            String hostHeader,
            String originHeader,
            String refererHeader,
            String username,
            String password,
            boolean requireSecureCookies,
            SSLConfiguration sslConfiguration){
        return  realLogon(host,hostHeader,originHeader,refererHeader,username,password,requireSecureCookies, sslConfiguration, null);
    }

    public static Session realLogon(String host,
            String hostHeader,
            String originHeader,
            String refererHeader,
            String username,
            String password,
            boolean requireSecureCookies,
            SSLConfiguration sslConfiguration,
            Proxy proxy) {
        Session session = new Session(host);
        session.setClient(new SWAMPHttpClient(host, sslConfiguration, proxy));
        session.getClient().setHostHeader(hostHeader);
        session.getClient().setOriginHeader(originHeader);
        session.getClient().setRefererHeader(refererHeader);
        session.setRequireSecureCookies(requireSecureCookies); // FIXME!!! This is because CSA-2187 has not been fixed.
        session.logon(username, password);
        return session;
    }

    public static Session realLogon(String host,
            String hostHeader,
            String originHeader,
            String refererHeader,
            String username,
            String password,
            SSLConfiguration sslConfiguration,
            Proxy proxy) {

        return realLogon(host,hostHeader,originHeader,refererHeader,username, password, false, sslConfiguration,proxy);
    }

    public static void setHandlerFactory(HandlerFactory handlerFactory) {
        HandlerFactoryUtil.handlerFactory = handlerFactory;
    }

    public static void shutdown() {

        if (getHandlerFactory().getCSASession() != null) {
            getHandlerFactory().getCSASession().logout();
        }

    }
}