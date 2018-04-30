package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.session.Session;

import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  4:32 PM
 */
public class HandlerFactory {
    public HandlerFactory(Session RWSSession, Session CSASession) {
        this.RWSSession = RWSSession;
        this.CSASession = CSASession;
    }

    /**
     * Convenience constructor to take a list of (at least) 2 sessions. The 0th element is assumed
     * to be the RWS session and the 1st element is assumed to the CSA session.
     * @param sessions
     */
    public HandlerFactory(List<Session> sessions){
        if(sessions.size()< 2){
            throw new IllegalArgumentException("Error: You must supply at least 2 sessions");
        }
        this.RWSSession = sessions.get(0);
        this.CSASession = sessions.get(1);
    }
    Session RWSSession;
    Session CSASession;

    public Session getRWSSession() {
        return RWSSession;
    }

    public void setRWSSession(Session RWSSession) {
        this.RWSSession = RWSSession;
    }

    public Session getCSASession() {
        return CSASession;
    }

    public void setCSASession(Session CSASession) {
        this.CSASession = CSASession;
    }

    public AssessmentRunHandler<? extends AssessmentRun> getAssessmentHandler() {
        if(assessmentHandler == null){
            assessmentHandler = new AssessmentRunHandler<>(getCSASession());
            assessmentHandler.setProjectHandler(getProjectHandler());
        }
        return assessmentHandler;
    }

    public void setAssessmentHandler(AssessmentRunHandler<? extends AssessmentRun> assessmentHandler) {
        this.assessmentHandler = assessmentHandler;
    }

    public PackageHandler<? extends PackageThing> getPackageHandler() {
        if(packageHandler == null){
            packageHandler = new PackageHandler<>(getCSASession());
        }
        return packageHandler;
    }

    public void setPackageHandler(PackageHandler<? extends PackageThing> packageHandler) {
        this.packageHandler = packageHandler;
    }

    public PlatformHandler<? extends Platform> getPlatformHandler() {
        if(platformHandler == null){
            platformHandler = new PlatformHandler<>(getCSASession());
        }
        return platformHandler;
    }

    public void setPlatformVersionHandler(PlatformVersionHandler<? extends PlatformVersion> platformVersionHandler) {
        this.platformVersionHandler = platformVersionHandler;
    }

    public PlatformVersionHandler<? extends PlatformVersion> getPlatformVersionHandler() {
        if(platformVersionHandler == null){
            platformVersionHandler = new PlatformVersionHandler<>(getCSASession());
        }
        return platformVersionHandler;
    }

    public void setPlatformHandler(PlatformHandler<? extends Platform> platformHandler) {
        this.platformHandler = platformHandler;
    }
    
    public ProjectHandler<? extends Project> getProjectHandler() {
        if(projectHandler == null){
            projectHandler = new ProjectHandler<>(getRWSSession());
        }
        return projectHandler;
    }

    public void setProjectHandler(ProjectHandler<? extends Project> projectHandler) {
        this.projectHandler = projectHandler;
    }

    public ToolHandler<? extends Tool> getToolHandler() {
        if(toolHandler == null){
            toolHandler = new ToolHandler<>(getCSASession());
        }
        return toolHandler;
    }

    protected ToolVersionHandler<? extends ToolVersion> toolVersionHandler;

    public ToolVersionHandler<? extends ToolVersion> getToolVersionHandler() {
          if(toolVersionHandler== null){
              toolVersionHandler = new ToolVersionHandler<>(getCSASession());
          }
          return toolVersionHandler;
      }
    public void setToolHandler(ToolHandler<? extends Tool> toolHandler) {
        this.toolHandler = toolHandler;
    }

    AdministrationHandler administrationHandler;
    public AdministrationHandler getAdminHandler(){
       if(administrationHandler == null){
                                administrationHandler = new AdministrationHandler(getRWSSession());
       }
        return administrationHandler;
    }
    public UserHandler<? extends User> getUserHandler() {
        if(userHandler == null){
            userHandler = new UserHandler<>(getRWSSession());
        }
        return userHandler;
    }

    public void setUserHandler(UserHandler<? extends User> userHandler) {
        this.userHandler = userHandler;
    }

    AssessmentRunHandler<? extends AssessmentRun> assessmentHandler;
    PackageHandler<? extends PackageThing> packageHandler;

    AssessmentResultHandler<? extends AssessmentResults> assessmentResultHandler;

    public AssessmentResultHandler<? extends AssessmentResults> getAssessmentResultHandler() {
         if(assessmentResultHandler == null){
             assessmentResultHandler = new AssessmentResultHandler<>(getCSASession());
         }
         return assessmentResultHandler;
     }


    public PackageVersionHandler<? extends PackageVersion> getPackageVersionHandler() {
        if(packageVersionHandler == null){
            packageVersionHandler = new PackageVersionHandler<>(getCSASession());
        }
        return packageVersionHandler;
    }

    public void setPackageVersionHandler(PackageVersionHandler<? extends PackageVersion> packageVersionHandler) {
        this.packageVersionHandler = packageVersionHandler;
    }

    PackageVersionHandler<? extends PackageVersion> packageVersionHandler;
    PlatformHandler<? extends Platform> platformHandler;
    PlatformVersionHandler<? extends PlatformVersion> platformVersionHandler;
    ProjectHandler<? extends Project> projectHandler;
    ToolHandler<? extends Tool> toolHandler;
    UserHandler<? extends User> userHandler;
    RunRequestHandler<? extends RunRequest> runRequestHandler;
    RunRequestScheduleHandler<? extends RunRequestSchedule> runRequestScheduleHandler;

    public RunRequestHandler<? extends RunRequest> getRunRequestHandler() {
        if(runRequestHandler == null){
            runRequestHandler = new RunRequestHandler<>(getCSASession());
        }
        return runRequestHandler;
    }


    public void setRunRequestHandler(RunRequestHandler<? extends RunRequest> runRequestHandler) {
        this.runRequestHandler = runRequestHandler;
    }

    public RunRequestScheduleHandler<? extends RunRequestSchedule> getRunRequestScheduleHandler() {
        if(runRequestScheduleHandler == null){
            runRequestScheduleHandler = new RunRequestScheduleHandler<>(getCSASession());
        }
        return runRequestScheduleHandler;
    }

    public void setRunRequestScheduleHandler(RunRequestScheduleHandler<? extends RunRequestSchedule> runRequestScheduleHandler) {
        this.runRequestScheduleHandler = runRequestScheduleHandler;
    }

    
    AssessmentRecordHandler<? extends AssessmentRecord> assessmentRecordHandler;
    
    public AssessmentRecordHandler<? extends AssessmentRecord> getassessmentRecordHandler() {
         if(assessmentRecordHandler == null){
        	 assessmentRecordHandler = new AssessmentRecordHandler<>(getCSASession());
         }
         return assessmentRecordHandler;
     }
}
