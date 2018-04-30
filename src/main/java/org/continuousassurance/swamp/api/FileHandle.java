package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

import static org.continuousassurance.swamp.session.handlers.PackageVersionHandler.*;

/**
 * Used in uploading a file to a package version. Internal to the SWAMP a file handle is used.
 * Mostly you do not have to worry about this, since access to the file is handled for you.
 * <p>Created by Jeff Gaynor<br>
 * on 1/13/15 at  2:51 PM
 */
public class FileHandle extends SwampThing{
    public FileHandle(Session session) {
        super(session);
    }
    public FileHandle(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new FileHandle(getSession());
    }

    @Override
    public String getIDKey() {
        return FILE_UPLOAD_ID;
    }

    public String getName(){return getString(FILE_UPLOAD_NAME);}
    public void setName(String name){put(FILE_UPLOAD_NAME, name);}
    public String getPath(){return getString(FILE_UPLOAD_PATH);}
    public void setPath(String path){put(FILE_UPLOAD_PATH, path);}
    public String getMimeType(){return getString(FILE_UPLOAD_MIME_TYPE);}
    public void setMimeType(String mimeType){put(FILE_UPLOAD_MIME_TYPE, mimeType);}
    public String getExtension(){return getString(FILE_UPLOAD_EXTENSION);}
    public void setExtension(String extension){put(FILE_UPLOAD_EXTENSION, extension);}
    public long getSize(){return getLong(FILE_UPLOAD_SIZE);}
    public void setSize(long size){put(FILE_UPLOAD_SIZE, size);}
    /*
    public final static String FILE_UPLOAD_EXTENSION = "extension";
    public final static String FILE_UPLOAD_SIZE = "size";
     */
}
