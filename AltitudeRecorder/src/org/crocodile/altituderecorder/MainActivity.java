
package org.crocodile.altituderecorder;

import org.crocodile.altituderecorder.AltitudeRecordService.LocalBinder;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.*;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity
{
    boolean                   bound;
    AltitudeRecordService     serv;

    private ServiceConnection connection = new ServiceConnection() {

                                             public void onServiceDisconnected(ComponentName name)
                                             {
                                                 Log.d(Constants.LOGTAG, "Service is disconnected");
                                                 bound = false;
                                                 serv = null;
                                             }

                                             @Override
                                             public void onServiceConnected(ComponentName name, IBinder service)
                                             {
                                                 Log.d(Constants.LOGTAG, "Service is connected");
                                                 bound = true;
                                                 LocalBinder mLocalBinder = (LocalBinder) service;
                                                 serv = mLocalBinder.getService();

                                             }
                                         };

    private BroadcastReceiver breceiver  = new BroadcastReceiver() {

                                             @Override
                                             public void onReceive(Context context, Intent intent)
                                             {
                                                 String action = intent.getAction();
                                                 if(Constants.BROADCAST_TAG.equals(action))
                                                 {
                                                     int token = (int) intent.getExtras().getInt(
                                                             Constants.BROADCAST_TAG);
                                                     String fname = intent.getExtras().getString(
                                                             Constants.BROADCAST_FNAME);
                                                     broadcastReceived(token, fname);
                                                 } else
                                                 {
                                                     Log.e(Constants.LOGTAG, "Unknown broadcast tag " + action);
                                                 }
                                             }
                                         };

    private IntentFilter      bfilter    = new IntentFilter(Constants.BROADCAST_TAG);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = new Intent(this, AltitudeRecordService.class);
        bindService(i, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(connection);
    }

    protected void broadcastReceived(int token, String fname)
    {
        int msgid;
        if(token == Constants.SERVICE_STARTED_TOKEN)
        {
            msgid = R.string.start_notification;
        } else if(token == Constants.SERVICE_STOPPED_TOKEN)
        {
            msgid = R.string.stop_notification;
        } else
        {
            msgid = R.string.unknown_notification;
            Log.e(Constants.LOGTAG, "Uknown token received " + token);
        }
        TextView l = (TextView) findViewById(R.id.statusLabel);
        l.setText(msgid);
        if(fname == null)
            fname = "";
        l = (TextView) findViewById(R.id.filenameLabel);
        l.setText(fname);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onStartClicked(View view)
    {
        serv.startRecording();
    }

    public void onStopClicked(View view)
    {
        serv.stopRecording();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        this.unregisterReceiver(breceiver);
    }

    @Override
    protected void onStart()
    {
        this.registerReceiver(breceiver, bfilter);
        super.onStart();
    };

}
