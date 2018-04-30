package org.continuousassurance.swamp.util;

import edu.uiuc.ncsa.security.util.ssl.SSLConfiguration;
import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.session.SWAMPHttpClient;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.*;
import org.continuousassurance.swamp.session.util.Proxy;

public class HandlerFactoryUtil {
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

    protected static HandlerFactory handlerFactory;

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

    /**
     * Create a handler factory with no proxy.
     * @param rwsServer
     * @param csaServer
     * @param originHeader
     * @param refererHeader
     * @param hostHeader
     * @param username
     * @param password
     * @param sslConfiguration
     * @return
     */
    public static HandlerFactory createHandlerFactory(String rwsServer,
                                                        String csaServer,
                                                        String originHeader,
                                                        String refererHeader,
                                                        String hostHeader,
                                                        String username,
                                                        String password,
                                                        SSLConfiguration sslConfiguration) {
        Proxy proxy = new Proxy();
        //proxy.configured = false;
        return createHandlerFactory(rwsServer,
                csaServer,
                originHeader,
                refererHeader,
                hostHeader,
                username,
                password,
                sslConfiguration,
                proxy);
      }

    public static HandlerFactory createHandlerFactory(String rwsServer,
                                                      String csaServer,
                                                      String originHeader,
                                                      String refererHeader,
                                                      String hostHeader,
                                                      String username,
                                                      String password,
                                                      SSLConfiguration sslConfiguration,
                                                      Proxy proxy) {
        Session rwsSession = realLogon(rwsServer, hostHeader, originHeader, refererHeader, username, password, sslConfiguration, proxy);
        Session csaSession = realLogon(csaServer, hostHeader, originHeader, refererHeader, username, password, sslConfiguration, proxy);

        HandlerFactory hf = new HandlerFactory(rwsSession, csaSession);
        setHandlerFactory(hf);
        return hf;

    }


    public static ProjectHandler<? extends Project> getProjectH() {
        return getHandlerFactory().getProjectHandler();
    }

    public static HandlerFactory getHandlerFactory() {
        if(handlerFactory == null){
            throw new IllegalStateException("The handler factory has not been initialized yet.");
        }
        return handlerFactory;
    }

    public static void setHandlerFactory(HandlerFactory handlerFactory) {
        HandlerFactoryUtil.handlerFactory = handlerFactory;
    }

    public static AssessmentRunHandler<? extends AssessmentRun> getAssessmentH() {
        return getHandlerFactory().getAssessmentHandler();
    }

    public static ToolHandler<? extends Tool> getToolH() {
        return getHandlerFactory().getToolHandler();
    }

    public static RunRequestHandler<? extends RunRequest> getRunRequestH() {
        return getHandlerFactory().getRunRequestHandler();
    }

    public static RunRequestScheduleHandler<? extends RunRequestSchedule> getRunRequestScheduleH() {
        return getHandlerFactory().getRunRequestScheduleHandler();
    }

    public static void shutdown() {
        if (getHandlerFactory().getRWSSession() != null) {
            getHandlerFactory().getRWSSession().logout();
        }
        if (getHandlerFactory().getCSASession() != null) {
            getHandlerFactory().getCSASession().logout();
        }

    }

    public static PlatformHandler<? extends Platform> getPlatformH() {
        return HandlerFactoryUtil.getHandlerFactory().getPlatformHandler();
    }

    public static UserHandler<? extends User> getUserH() {
        return HandlerFactoryUtil.getHandlerFactory().getUserHandler();
    }

    public static PackageHandler<? extends PackageThing> getPackageH() {
        return HandlerFactoryUtil.getHandlerFactory().getPackageHandler();
    }

    public static PackageVersionHandler<? extends PackageVersion> getPackageVersionH() {
        return HandlerFactoryUtil.getHandlerFactory().getPackageVersionHandler();
    }
}