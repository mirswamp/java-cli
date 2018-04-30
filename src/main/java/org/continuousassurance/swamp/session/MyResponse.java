package org.continuousassurance.swamp.session;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.cookie.Cookie;

import java.io.OutputStream;
import java.util.List;

/**
 * A basic response object for http calls.
 * <p>Created by Jeff Gaynor<br>
 * on 10/8/14 at  11:24 AM
 */
public class MyResponse {
    public JSONObject json;
    public JSONArray jsonArray;
    public List<Cookie> cookies;
    public OutputStream outputStream;

   public boolean hasJSON(){return json != null;}

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    int httpResponseCode = 0;

    public MyResponse(OutputStream outputStream, List<Cookie> cookies) {
        this.outputStream = outputStream;
        this.cookies = cookies;
    }

    boolean streamable = false;
    /**
     * Returns if this response should be interpreted as a stream rather than a JSON object.
     * @return
     */
    public boolean isStreamable(){return streamable;}
    public void setStreamable(boolean streamable){this.streamable = streamable;}
     public OutputStream getOutputStream(){return outputStream;}

    public MyResponse(JSON json, List<Cookie> cookies) {
        this.cookies = cookies;
        if(json == null) return;
        if (json.isArray()) {
            jsonArray = (JSONArray) json;
        }
        if (json instanceof JSONObject) {
            this.json = (JSONObject) json;
        }
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
}
