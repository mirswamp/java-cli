package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.RunRequestSchedule;
import org.continuousassurance.swamp.session.Session;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/22/14 at  3:40 PM
 */
public class RunRequestScheduleHandler<T extends RunRequestSchedule> extends AbstractHandler<T> {
    public static String RUN_REQUEST_SCHEDULE_UUID = "run_request_schedule_uuid";
    /**
     * Key for updating the uuid of the associated run request.
     */
    public static String RUN_REQUEST_UUID = "run_request_uuid";
    public static String RECURRENCE_TYPE = "recurrence_type";
    public static String RECURRENCE_DAY = "recurrence_day";
    public static String RECURRENCE_TIME_OF_DAY = "time_of_day";


    public RunRequestScheduleHandler(Session session) {
        super(session);
    }

    @Override
    protected T fromJSON(JSONObject json) {
        return null;
    }

    @Override
    public List<T> getAll() {
        return null;
    }

    @Override
    public String getURL() {
        return createURL("run_request_schedules");
    }
}
