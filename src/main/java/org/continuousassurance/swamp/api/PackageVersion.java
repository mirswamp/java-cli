package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Date;
import java.util.Map;

import static org.continuousassurance.swamp.session.handlers.PackageVersionHandler.*;

/**
 * This models a version of a package. The supported properties are
 * <ul>
 * <li>Build command</li>
 * <li>Build directory</li>
 * <li>Build options</li>
 * <li>Build system</li>
 * <li>Build target</li>
 * <li>Bytecode auxiliary class path</li>
 * <li>Bytecode class path</li>
 * <li>Bytecode source path</li>
 * <li>Configuration command</li>
 * <li>Configuration directory</li>
 * <li>File handle - the {@link FileHandle} to the uploaded source code </li>
 * <li>Notes - Human readable notes about the version</li>
 * <li>Package - the {@link PackageThing} for this version</li>
 * <li>Platform - the {@link Platform} for this version</li>
 * <li>Release date</li>
 * <li>Retire date</li>
 * <li>Sharing status - the sharing status of this version (which can be different from the package itself.)</li>
 * <li>Version String - the human readable string that identifies the version.</li>
 * </ul>
 * <p>Created by Jeff Gaynor<br>
 * on 1/13/15 at  1:26 PM
 */
public class PackageVersion extends SwampThing {
    public PackageVersion(Session session) {
        super(session);
    }
    public PackageVersion(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new PackageVersion(getSession());
    }

    @Override
    public String getIDKey() {
        return PACKAGE_VERSION_UUID;
    }

    public PackageThing getPackageThing() {
        return packageThing;
    }

    public void setPackageThing(PackageThing packageThing) {
        this.packageThing = packageThing;
    }

    PackageThing packageThing;

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    Platform platform;

    /**
     * The file handle contains most of the internal information about the file that has been uploaded.
     *
     * @return
     */
    public FileHandle getFileHandle() {
        return fileHandle;
    }

    public void setFileHandle(FileHandle fileHandle) {
        this.fileHandle = fileHandle;
    }

    FileHandle fileHandle;

    public String getVersionString() {
        return getString(VERSION_STRING);
    }

    public void setVersionString(String versionString) {
        put(VERSION_STRING, versionString);
    }

    public String getSharingStatus() {
        return getString(VERSION_SHARING_STATUS);
    }

    public void setSharingStatus(String sharingStatus) {
        put(VERSION_SHARING_STATUS, sharingStatus);
    }

    public Date getReleaseDate() {
        return getDate(RELEASE_DATE);
    }

    public void setReleaseDate(Date releaseDate) {
        put(RELEASE_DATE, releaseDate);
    }

    public Date getRetireDate() {
        return getDate(RETIRE_DATE);
    }

    public void setRetireDate(Date retireDate) {
        put(RETIRE_DATE, retireDate);
    }

    public String getNotes() {
        return getString(NOTES);
    }

    public void setNotes(String notes) {
        put(NOTES, notes);
    }

    public String getConfigurationDirectory() {
        return getString(CONFIG_DIR);
    }

    public void setConfigurationDirectory(String configDir) {
        put(CONFIG_DIR, configDir);
    }

    public String getConfigurationCommand() {
        return getString(CONFIG_CMD);
    }

    public void setConfigurationCommand(String configurationCommand) {
        put(CONFIG_CMD, configurationCommand);
    }

    public String getBuildCommand() {
        return getString(BUILD_CMD);
    }

    public void setBuildCommand(String x) {
        put(BUILD_CMD, x);
    }

    public String getBuildFile() {
        return getString(BUILD_FILE);
    }

    public void setBuildFile(String x) {
        put(BUILD_FILE, x);
    }

    public String getBuildTarget() {
        return getString(BUILD_TARGET);
    }

    public void setBuildTarget(String x) {
        put(BUILD_TARGET, x);
    }

    public String getBuildDirectory() {
        return getString(BUILD_DIR);
    }

    public void setBuildDirectory(String x) {
        put(BUILD_DIR, x);
    }

    public String getBuildOptions() {
        return getString(BUILD_OPT);
    }

    public void setBuildOptions(String x) {
        put(BUILD_OPT, x);
    }

    public String getBuildSystem() {
        return getString(BUILD_SYSTEM);
    }

    public void setBuildSystem(String x) {
        put(BUILD_SYSTEM, x);
    }

    public String getBytecodeClassPath() {
        return getString(BYTECODE_CLASS_PATH);
    }

    public void setBytecodeClassPath(String x) {
        put(BYTECODE_CLASS_PATH, x);
    }

    public String getBytecodeAuxClassPath() {
        return getString(BYTECODE_AUX_CLASS_PATH);
    }

    public void setBytecodeAuxClassPath(String x) {
        put(BYTECODE_AUX_CLASS_PATH, x);
    }

    public String getBytecodeSourcePath() {
        return getString(BYTECODE_SOURCE_PATH);
    }

    public void setBytecodeSourcePath(String x) {
        put(BYTECODE_SOURCE_PATH, x);
    }

    public String getPackagePath(){return getString(PACKAGE_PATH);}
    public void setPackagePath(String packagePath){put(PACKAGE_PATH, packagePath);}
}
