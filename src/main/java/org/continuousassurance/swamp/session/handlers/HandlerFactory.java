package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.session.Session;

import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  4:32 PM
 */
public class HandlerFactory {
    AdministrationHandler administrationHandler;
    AssessmentRunHandler<? extends AssessmentRun> assessmentHandler;
    AssessmentRecordHandler<? extends AssessmentRecord> assessmentRecordHandler;
    AssessmentResultHandler<? extends AssessmentResults> assessmentResultHandler;

    Session CSASession;
    
    PackageHandler<? extends PackageThing> packageHandler;
    
    PackageVersionHandler<? extends PackageVersion> packageVersionHandler;

    PlatformHandler<? extends Platform> platformHandler;

    PlatformVersionHandler<? extends PlatformVersion> platformVersionHandler;

    ProjectHandler<? extends Project> projectHandler;

    RunRequestHandler<? extends RunRequest> runRequestHandler;

    RunRequestScheduleHandler<? extends RunRequestSchedule> runRequestScheduleHandler;

    ToolHandler<? extends Tool> toolHandler;

    protected ToolVersionHandler<? extends ToolVersion> toolVersionHandler;

    UserHandler<? extends User> userHandler;

    /**
     * Convenience constructor to take a list of (at least) 2 sessions. The 0th element is assumed
     * to be the RWS session and the 1st element is assumed to the CSA session.
     * @param sessions
     */
    public HandlerFactory(List<Session> sessions){
        if(sessions.size()< 1){
            throw new IllegalArgumentException("Error: You must supply at least 2 sessions");
        }
        this.CSASession = sessions.get(1);
    }

    public HandlerFactory(Session CSASession) {
        this.CSASession = CSASession;
    }

    public AdministrationHandler getAdminHandler(){
        if(administrationHandler == null){
            administrationHandler = new AdministrationHandler(getCSASession());
        }
        return administrationHandler;
    }

    public AssessmentRunHandler<? extends AssessmentRun> getAssessmentHandler() {
        if(assessmentHandler == null){
            assessmentHandler = new AssessmentRunHandler<>(getCSASession());
            assessmentHandler.setProjectHandler(getProjectHandler());
        }
        return assessmentHandler;
    }

    public AssessmentRecordHandler<? extends AssessmentRecord> getassessmentRecordHandler() {
        if(assessmentRecordHandler == null){
            assessmentRecordHandler = new AssessmentRecordHandler<>(getCSASession());
        }
        return assessmentRecordHandler;
    }

    public AssessmentResultHandler<? extends AssessmentResults> getAssessmentResultHandler() {
        if(assessmentResultHandler == null){
            assessmentResultHandler = new AssessmentResultHandler<>(getCSASession());
        }
        return assessmentResultHandler;
    }
    public Session getCSASession() {
        return CSASession;
    }

    public PackageHandler<? extends PackageThing> getPackageHandler() {
        if(packageHandler == null){
            packageHandler = new PackageHandler<>(getCSASession());
        }
        return packageHandler;
    }
    public PackageVersionHandler<? extends PackageVersion> getPackageVersionHandler() {
        if(packageVersionHandler == null){
            packageVersionHandler = new PackageVersionHandler<>(getCSASession());
        }
        return packageVersionHandler;
    }
    public PlatformHandler<? extends Platform> getPlatformHandler() {
        if(platformHandler == null){
            platformHandler = new PlatformHandler<>(getCSASession());
        }
        return platformHandler;
    }

    public PlatformVersionHandler<? extends PlatformVersion> getPlatformVersionHandler() {
        if(platformVersionHandler == null){
            platformVersionHandler = new PlatformVersionHandler<>(getCSASession());
        }
        return platformVersionHandler;
    }

    public ProjectHandler<? extends Project> getProjectHandler() {
        if(projectHandler == null){
            projectHandler = new ProjectHandler<>(getCSASession());
        }
        return projectHandler;
    }


    public RunRequestHandler<? extends RunRequest> getRunRequestHandler() {
        if(runRequestHandler == null){
            runRequestHandler = new RunRequestHandler<>(getCSASession());
        }
        return runRequestHandler;
    }

    public RunRequestScheduleHandler<? extends RunRequestSchedule> getRunRequestScheduleHandler() {
        if(runRequestScheduleHandler == null){
            runRequestScheduleHandler = new RunRequestScheduleHandler<>(getCSASession());
        }
        return runRequestScheduleHandler;
    }

    public ToolHandler<? extends Tool> getToolHandler() {
        if(toolHandler == null){
            toolHandler = new ToolHandler<>(getCSASession());
        }
        return toolHandler;
    }
    public ToolVersionHandler<? extends ToolVersion> getToolVersionHandler() {
        if(toolVersionHandler== null){
            toolVersionHandler = new ToolVersionHandler<>(getCSASession());
        }
        return toolVersionHandler;
    }
    public UserHandler<? extends User> getUserHandler() {
        if(userHandler == null){
            userHandler = new UserHandler<>(getCSASession());
        }
        return userHandler;
    }
    public void setAssessmentHandler(AssessmentRunHandler<? extends AssessmentRun> assessmentHandler) {
        this.assessmentHandler = assessmentHandler;
    }
    public void setCSASession(Session CSASession) {
        this.CSASession = CSASession;
    }
    public void setPackageHandler(PackageHandler<? extends PackageThing> packageHandler) {
        this.packageHandler = packageHandler;
    }
    public void setPackageVersionHandler(PackageVersionHandler<? extends PackageVersion> packageVersionHandler) {
        this.packageVersionHandler = packageVersionHandler;
    }
    public void setPlatformHandler(PlatformHandler<? extends Platform> platformHandler) {
        this.platformHandler = platformHandler;
    }

    public void setPlatformVersionHandler(PlatformVersionHandler<? extends PlatformVersion> platformVersionHandler) {
        this.platformVersionHandler = platformVersionHandler;
    }


    public void setProjectHandler(ProjectHandler<? extends Project> projectHandler) {
        this.projectHandler = projectHandler;
    }

    public void setRunRequestHandler(RunRequestHandler<? extends RunRequest> runRequestHandler) {
        this.runRequestHandler = runRequestHandler;
    }

    public void setRunRequestScheduleHandler(RunRequestScheduleHandler<? extends RunRequestSchedule> runRequestScheduleHandler) {
        this.runRequestScheduleHandler = runRequestScheduleHandler;
    }


    public void setToolHandler(ToolHandler<? extends Tool> toolHandler) {
        this.toolHandler = toolHandler;
    }

    public void setUserHandler(UserHandler<? extends User> userHandler) {
        this.userHandler = userHandler;
    }
}
