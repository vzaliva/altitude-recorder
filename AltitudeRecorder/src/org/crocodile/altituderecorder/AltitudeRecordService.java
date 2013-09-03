
package org.crocodile.altituderecorder;

import java.io.File;
import java.io.IOException;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus.NmeaListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AltitudeRecordService extends Service implements NmeaListener
{
    private static final String DATA_FILE_SUFFIX   = ".txt";
    private static final String DATA_FILE_PREFIX   = "alt-";

    private int                 START_NOTIFICATION = R.string.start_notification;
    private int                 STOP_NOTIFICATION  = R.string.stop_notification;
    private boolean             recording          = false;

    private String              fname;

    IBinder                     binder             = new LocalBinder();

    public class LocalBinder extends Binder
    {
        AltitudeRecordService getService()
        {
            return AltitudeRecordService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        startRecording();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        stopRecording();
        super.onDestroy();
    }

    private void sendBroadcast(int token, String extra)
    {
        Intent i = new Intent(Constants.BROADCAST_TAG);
        i.putExtra(Constants.BROADCAST_TAG, token);
        i.putExtra(Constants.BROADCAST_FNAME, extra);
        this.sendBroadcast(i);
    }

    public void startRecording()
    {
        if(!recording)
        {
            try
            {
                fname = getNewLogFile();
                Log.d(Constants.LOGTAG, "Will write data to file " + fname);

                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                lm.addNmeaListener(this);
                recording = true;

                showNotification(START_NOTIFICATION);
                sendBroadcast(Constants.SERVICE_STARTED_TOKEN, fname);
            } catch(IOException e)
            {
                recording = false;
                Log.e(Constants.LOGTAG, "Error starting service", e);
            }
        }
    }

    public void stopRecording()
    {
        if(recording)
        {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            lm.removeNmeaListener(this);

            recording = false;
            showNotification(STOP_NOTIFICATION);
            sendBroadcast(Constants.SERVICE_STOPPED_TOKEN, fname);
        }
    }

    public boolean isRecording()
    {
        return recording;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(int n)
    {
        CharSequence text = getText(n);

        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Notification notification = new Notification.Builder(this).setContentTitle(getText(R.string.app_name))
                .setContentText(text).setSmallIcon(R.drawable.ic_launcher).setContentIntent(contentIntent).build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(n, notification);
    }

    private String getNewLogFile() throws IOException
    {
        File fdir = this.getFilesDir();
        File f = File.createTempFile(DATA_FILE_PREFIX, DATA_FILE_SUFFIX, fdir);
        return f.getAbsolutePath();
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea)
    {
        // TODO Auto-generated method stub

    }

}
