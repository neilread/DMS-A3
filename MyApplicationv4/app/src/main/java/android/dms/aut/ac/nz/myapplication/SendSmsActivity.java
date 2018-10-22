package android.dms.aut.ac.nz.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.dms.aut.ac.nz.myapplication.R.string.error_invalid_ph_number;

/**
 * Activity for sending and receiving messages via SMS
 */
public class SendSmsActivity extends Activity implements SMSListener {
    public static final int SEND_SMS_REQUEST = 1;
    public static final int REQUEST_CONTACT = 2;

    private ArrayAdapter<String> messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sms);

        ListView messages = (ListView)findViewById(R.id.messagesList);
        messagesAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, new ArrayList<String>());
        messages.setAdapter(messagesAdapter);

        EditText pNumText = (EditText) findViewById(R.id.phoneText);

        pNumText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            /**
             * Populates messages list with all texts received from associated phone number
             */
            public void afterTextChanged(Editable s){
                EditText pNumText = (EditText) findViewById(R.id.phoneText);
                Editable pNum = pNumText.getText();
                updateMessagesList(pNum.toString());
            }
        });

        /**
         * Notifies user when text is received and adds it to the messages list
         * if it is from the currently displayed phone number
         */
        SMSReceiver.bindListener(this);
    }

    /**
     * Populates messages list with received messages from passed
     * phone number
     * @param phoneNum
     */
    private void updateMessagesList(String phoneNum){
        messagesAdapter.clear();
        // Gets all SMS messages in the inbox from the passed address, sorted by date in ascending order
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{Telephony.TextBasedSmsColumns.BODY}, "address = ?", new String[]{phoneNum}, "date ASC");
        if (cursor.moveToFirst()) { // Checks result is not empty to avoid exception
            do {
                messagesAdapter.add(cursor.getString(0));
            } while (cursor.moveToNext());
        } else {
            messagesAdapter.add("No messages");
        }
    }

    // Checks read contacts permission and opens contacts
    public void onSelectContactPressed(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT);
        } else {
            selectContact();
        }
    }

    // Opens contacts application to select a contact's phone number
    private void selectContact(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    // Returns the phone number of a selected contact
    private String readPhoneNumber(Intent data) {
        Uri contactData = data.getData();
        Cursor c = managedQuery(contactData, null, null, null, null);
        if (c.moveToFirst()) {
            String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

            if (hasPhone.equals("1")) {
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,null, null);
                phones.moveToFirst();
                return phones.getString(phones.getColumnIndex("data1"));
            } else {
                Toast.makeText(getBaseContext(), getString(R.string.error_contact_no_p_num), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.error_no_contact_selected), Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // Contact selected
            case (REQUEST_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {
                    String pNumber = readPhoneNumber(data);

                    // Enters retrieved phone number in phone number field
                    if(pNumber != null){
                        EditText pNumberField = (EditText) findViewById(R.id.phoneText);
                        pNumberField.setText(pNumber, TextView.BufferType.EDITABLE);
                    }
                }
                break;
        }
    }

    // Checks send sms permission and sends sms message
    public void onSendPressed(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_REQUEST);
        } else {
            sendMessage();
        }
    }

    // Sends message (in message field) to selected phone number (in phone number field) via sms
    private void sendMessage(){
        try {
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), getString(R.string.sms_sent), Toast.LENGTH_SHORT).show();

                            // Empties message text field
                            TextView messageText = (TextView) findViewById(R.id.messageText);
                            messageText.setText("");
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Toast.makeText(getBaseContext(), getString(R.string.error_send_failed), Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Toast.makeText(getBaseContext(), getString(R.string.error_no_service), Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Toast.makeText(getBaseContext(), getString(R.string.error_null_pdu), Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Toast.makeText(getBaseContext(), getString(R.string.error_radio_off), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(getString(R.string.pi_sms_sent)));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), getString(R.string.sms_delivered), Toast.LENGTH_SHORT).show();
                            break;
                        case Activity.RESULT_CANCELED:
                            Toast.makeText(getBaseContext(), getString(R.string.error_sms_not_delivered), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }, new IntentFilter(getString(R.string.pi_sms_delivered)));

            SmsManager smsManager = SmsManager.getDefault();
            String phoneNumber = ((EditText) findViewById(R.id.phoneText)).getText().toString();
            String message = ((EditText) findViewById(R.id.messageText)).getText().toString();
            if (message.isEmpty()) {
                Toast.makeText(getBaseContext(), getString(R.string.error_message_empty), Toast.LENGTH_SHORT).show();
            } else {
                smsManager.sendTextMessage(parsePhoneNumber(phoneNumber), null, message, PendingIntent.getBroadcast(this, 0, new Intent(getString(R.string.pi_sms_sent)), 0), PendingIntent.getBroadcast(this, 0, new Intent(getString(R.string.pi_sms_delivered)), 0));
            }
        } catch (IllegalArgumentException ex){
            Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch(requestCode){
            case SEND_SMS_REQUEST:
            {
                int i = Arrays.binarySearch(permissions, Manifest.permission.SEND_SMS);
                if(i >= 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    sendMessage();
                } else {
                    Toast.makeText(getBaseContext(), getString(R.string.error_permission_denied), Toast.LENGTH_LONG).show();
                }

                break;
            }
            case REQUEST_CONTACT:
            {
                int i = Arrays.binarySearch(permissions, Manifest.permission.READ_CONTACTS);
                if(i >= 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    selectContact();
                } else {
                    Toast.makeText(getBaseContext(), getString(R.string.error_permission_denied), Toast.LENGTH_LONG).show();
                }
                break;
            }
            default:
            {
                throw new IllegalArgumentException(getString(R.string.error_invalid_request_code, requestCode));
            }
        }
    }

    // Validates passed phone number
    private String parsePhoneNumber(String pNumber) throws IllegalArgumentException {
        if(pNumber.startsWith("+")|| pNumber.startsWith("0")){
            return pNumber;
        }
        else{
            throw new IllegalArgumentException(getString(error_invalid_ph_number, pNumber));
        }
    }

    @Override
    public void messageReceived(SmsMessage message) {
        EditText pNumText = (EditText) findViewById(R.id.phoneText);
        String pNum = pNumText.getText().toString();
        Toast.makeText(getBaseContext(), getString(R.string.sms_received, message.getDisplayOriginatingAddress()), Toast.LENGTH_SHORT).show();
        if(pNum.equalsIgnoreCase(message.getOriginatingAddress())){
            messagesAdapter.add(message.getDisplayMessageBody());
        }
    }
}
