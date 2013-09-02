
package org.crocodile.altituderecorder;

import java.io.*;

import android.app.*;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AltitudeRecordService extends Service
{
    private static final String DATA_FILE_SUFFIX   = ".txt";
    private static final String DATA_FILE_PREFIX   = "alt-";
    private static final String MONITOR_CMD        = "nohup /data/tmp/monitor_proximity -v &>>/data/tmp/monitor.log &";

    private int                 START_NOTIFICATION = R.string.start_notification;
    private int                 STOP_NOTIFICATION  = R.string.stop_notification;
    private boolean             recording          = false;

    private int                 pid1               = -1;
    private int                 pid2               = -1;

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

    private void killDaemon() throws IOException
    {
        Process suProcess = Runtime.getRuntime().exec("su");
        try
        {
            DataOutputStream stdin = new DataOutputStream(suProcess.getOutputStream());

            // Kill recorder
            killCMD(stdin, pid1);
            pid1 = -1;

            // Kill monitor
            killCMD(stdin, pid2);
            pid2 = -1;

            Log.d(Constants.LOGTAG, "Exiting SU");
            stdin.writeBytes("exit\n");
            stdin.flush();
            Log.d(Constants.LOGTAG, "Waiting for SU");
            try
            {
                suProcess.waitFor();
                Log.d(Constants.LOGTAG, "Done waiting for SU");
            } catch(InterruptedException e)
            {
                Log.e(Constants.LOGTAG, "Error waiting for SU");
            }
        } finally
        {
            Log.d(Constants.LOGTAG, "Destroying SU");
            suProcess.destroy();
        }
        Log.d(Constants.LOGTAG, "SU done");
    }

    private void killCMD(DataOutputStream stdin, int pid) throws IOException
    {
        if(pid != -1)
        {
            String cmd = "kill " + pid;
            Log.d(Constants.LOGTAG, "Sending cmd: " + cmd);
            stdin.writeBytes(cmd + "\n");
            stdin.flush();
        }
    }

    private void lauchDaemon() throws IOException
    {
        fname = getNewLogFile();
        Log.d(Constants.LOGTAG, "Will write data to file " + fname);

        String cmd1 = "nohup getevent -t -q /dev/input/event3 " + " >> " + fname + " &";
        String cmd2 = MONITOR_CMD;

        Process suProcess = Runtime.getRuntime().exec("su");
        try
        {
            DataOutputStream stdin = new DataOutputStream(suProcess.getOutputStream());
            BufferedReader stdout = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));

            pid2 = runCMD(cmd2, stdin, stdout);
            try
            {
                pid1 = runCMD(cmd1, stdin, stdout);
            } catch(IOException ex)
            {
                // Error starting second command. Clean up by killing first one
                throw ex;
            }

            Log.d(Constants.LOGTAG, "Exiting SU");
            stdin.writeBytes("exit\n");
            stdin.flush();
            Log.d(Constants.LOGTAG, "Waiting for SU");
            try
            {
                suProcess.waitFor();
                Log.d(Constants.LOGTAG, "Done waiting for SU");
            } catch(InterruptedException e)
            {
                Log.e(Constants.LOGTAG, "Error waiting for SU");
            }
        } finally
        {
            Log.d(Constants.LOGTAG, "Destroying SU");
            suProcess.destroy();
        }
        Log.d(Constants.LOGTAG, "SU done");
    }

    private int runCMD(String cmd, DataOutputStream stdin, BufferedReader stdout) throws IOException
    {
        Log.d(Constants.LOGTAG, "Sending cmd: " + cmd);
        stdin.writeBytes(cmd + "\n");
        stdin.flush();
        stdin.writeBytes("echo $!\n");
        stdin.flush();
        String pids = stdout.readLine();
        try
        {
            int pid = Integer.parseInt(pids);
            Log.d(Constants.LOGTAG, "Daemon started with PID=" + pid);
            return pid;
        } catch(NumberFormatException nex)
        {
            throw new IOException("Invalid PID: " + pids);
        }
    }

    public void startRecording()
    {
        if(!recording)
        {
            try
            {
                lauchDaemon();

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
            try
            {
                killDaemon();

                recording = false;
                showNotification(STOP_NOTIFICATION);
                sendBroadcast(Constants.SERVICE_STOPPED_TOKEN, fname);
            } catch(IOException e)
            {
                recording = true;
                Log.e(Constants.LOGTAG, "Error stopping service", e);
            }
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

}
