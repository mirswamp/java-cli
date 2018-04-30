package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.handlers.AssessmentRecordHandler;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.PackageHandler;
import org.continuousassurance.swamp.session.handlers.ToolHandler;

import java.util.Map;

/**
 * This models an assessment. Assessments also point to other objects, in particular
 * each assessment will point to a corresponding<br/>
 * <ul>
 *     <li>project</li>
 *     <li>package</li>
 *     <li>platform</li>
 *     <li>tool</li>
 * </ul>
 * Each of these are first class objects.
 * <p>Created by Jeff Gaynor<br>
 * on 11/18/14 at  3:06 PM
 */
public class AssessmentRecord extends SwampThing{

	Project project;
    PackageThing pkg;
    Platform platform;
    Tool tool;
    String status;

    public AssessmentRecord(Session session) {
        super(session);
    }

    public AssessmentRecord(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new AssessmentRecord(getSession());
    }

    @Override
    public String getIDKey() {
        return AssessmentRecordHandler.EXECUTION_RECORD_UUID;
    }


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public PackageThing getPkg() {
        return pkg;
    }

    public void setPkg(PackageThing pkg) {
        this.pkg = pkg;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public String getAssessmentRunUUID() {
        return getConversionMap().getString(AssessmentRecordHandler.ASSESSMENT_RUN_UUID);
    }

    public String getAssessmentResultUUID() {
        return getConversionMap().getString(AssessmentRecordHandler.ASSESSMENT_RESULT_UUID);
    }

    public String getStatus() {
        return getConversionMap().getString(AssessmentRecordHandler.STATUS_STRING);
    }

    public int getWeaknessCount() {
        return (Integer)getConversionMap().get(AssessmentRecordHandler.WEAKNESS_COUNT);
    }

    public String getProjectUUID() {
        return getConversionMap().getString(AssessmentRecordHandler.PROJECT_UUID);
    }

    public String getPackageUUID() {
        return getConversionMap().getString(PackageHandler.PACKAGE_UUID_KEY);
    }

    public String getPackageVersionUUID() {
        return getConversionMap().getString(AssessmentRecordHandler.PACKAGE_VERSION_UUID);
    }

    public String getToolUUID() {
        return getConversionMap().getString(ToolHandler.TOOL_UUID_KEY);
    }

    public String getToolVersionUUID() {
        return getConversionMap().getString(AssessmentRecordHandler.TOOL_VERSION_UUID);
    }

    public String getPlatformUUID() {
        return getConversionMap().getString(AssessmentRecordHandler.PLATFORM_VERSION_UUID);
    }
    
    @Override
    public String toString() {
        return "AssessmentRecord[" +
                "uuid=" + getIdentifier() +
                ", project=" + (project==null?"none":getProject().getFullName())+
                ", pkg=" + (pkg==null?"none":getPkg().getName()) +
                ", platform=" + (platform==null?"none":getPlatform().getName()) +
                ", tool=" + (tool==null?"none":getTool().getName()) +
                "]";
    }
}
