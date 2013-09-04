
package org.crocodile.altituderecorder;

public interface Constants
{

    static final String LOGTAG                = "AltitudeRecorder";

    static final String BROADCAST_TAG         = "AltitudeRecordService";
    static final String BROADCAST_FNAME       = "Filename";

    static final int    SERVICE_STARTED_TOKEN = 1;
    static final int    SERVICE_STOPPED_TOKEN = 0;

    static final int    UPDATE_INTERVAL       = 1000;

    static final String BARO_PREFIX           = "#BARO";

    static final String TIMESTAMP_SEPARATOR   = ";";

    /**
     * Used for notifications in task bar.
     */
    static final int    NOTIFICATION_ID       = 0;

}
