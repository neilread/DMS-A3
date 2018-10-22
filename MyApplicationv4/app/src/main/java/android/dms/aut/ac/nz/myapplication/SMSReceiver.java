package android.dms.aut.ac.nz.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Listens for received SMS messages and calls listener
 */
public class SMSReceiver extends BroadcastReceiver {
    private static SMSListener listener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null){
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    for(Object pdu : pdus){
                        listener.messageReceived(SmsMessage.createFromPdu((byte[]) pdu));
                    }
                } catch(Exception ex) {
                    Log.e("", ex.toString());
                }
            }
        }
    }

    public static void bindListener(SMSListener l){
        listener = l;
    }
}
