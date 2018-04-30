package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

import static org.continuousassurance.swamp.session.handlers.RunRequestScheduleHandler.RUN_REQUEST_SCHEDULE_UUID;

/**
 * This models the schedule for an assessment run. Properties are
 * <ul>
 *     <li>{@link RunRequest} this schedule refers to</li>
 *     <li>Recurrence type</li>
 *     <li>Recurrence day</li>
 *     <li>Recurrence time of day - the time of day this should run.</li>
 * </ul>
 * <p>Created by Jeff Gaynor<br>
 * on 12/22/14 at  3:35 PM
 */
public class RunRequestSchedule extends SwampThing{
    public RunRequestSchedule(Session session) {
        super(session);
    }
    public RunRequestSchedule(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new RunRequestSchedule(getSession());
    }

    @Override
    public String getIDKey() {
        return RUN_REQUEST_SCHEDULE_UUID;
    }

    public RunRequest getRunRequest() {
        return runRequest;
    }

    public void setRunRequest(RunRequest runRequest) {
        this.runRequest = runRequest;
    }

    RunRequest runRequest;
    String recurrenceType;
    String recurrenceTimeOfDay;
    String recurrenceDay;

    public String getRecurrenceDay() {
        return recurrenceDay;
    }

    public void setRecurrenceDay(String recurrenceDay) {
        this.recurrenceDay = recurrenceDay;
    }

    public String getRecurrenceTimeOfDay() {
        return recurrenceTimeOfDay;
    }

    public void setRecurrenceTimeOfDay(String recurrenceTimeOfDay) {
        this.recurrenceTimeOfDay = recurrenceTimeOfDay;
    }

    public String getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }
}
