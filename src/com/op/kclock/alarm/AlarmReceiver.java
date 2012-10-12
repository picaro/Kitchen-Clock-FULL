package com.op.kclock.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;
import com.op.kclock.*;


/**
This class is used for receiving alarm broadcasts 
 */

public class AlarmReceiver extends BroadcastReceiver{
   
    /**
     * This method receives Intents during the wake up interval, and either schedules a new broadcast or starts up the alarm.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(MainActivity.TAG, "A-L-A-R-M");
    }

    private void startAlarm(Context context) {
        //Intent newIntent = new Intent(context, AlarmActivity.class);
        //newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        //context.startActivity(newIntent);
    }

}
