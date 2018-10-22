package android.dms.aut.ac.nz.myapplication;

import android.telephony.SmsMessage;

/**
 * Listener called when an SMS message has been received
 */
public interface SMSListener {
    void messageReceived(SmsMessage message);
}
