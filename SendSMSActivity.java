package com.example.neilr.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class SendSMSActivity extends Activity {
    public static final int SEND_SMS_REQUEST = 1;
    public static final int REQUEST_CONTACT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_sms);
        getPermissions();
    }
    //212556333

    public void onSelectContactPressed(View view){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACT);
        } else {
            selectContact();
        }
    }

    private void selectContact(){
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    private String readPhoneNumber(Intent data) {
        Uri contactData = data.getData();
        Cursor c = managedQuery(contactData, null, null, null, null);
        if (c.moveToFirst()) {
            String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            if (hasPhone.equals("1")) {
                Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,null, null);
                phones.moveToFirst();
                return phones.getString(phones.getColumnIndex("data1"));
            } else {
                Toast.makeText(getBaseContext(), name + " has no phone number", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "No contact selected", Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case (REQUEST_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {
                    String pNumber = readPhoneNumber(data);
                    if(pNumber != null){
                        EditText pNumberField = findViewById(R.id.phoneText);
                        pNumberField.setText(pNumber, TextView.BufferType.EDITABLE);
                    }
                }
                break;
        }
    }

    public void onSendPressed(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_REQUEST);
        } else {
            sendMessage();
        }
    }

    public void sendMessage(){
        try {
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            Toast.makeText(getBaseContext(), getString(R.string.sms_sent), Toast.LENGTH_SHORT).show();
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
            EditText phoneNumber = findViewById(R.id.phoneText);
            EditText message = findViewById(R.id.messageText);
            if (message.getText().toString().isEmpty()) {
                Toast.makeText(getBaseContext(), getString(R.string.error_message_empty), Toast.LENGTH_SHORT).show();
            } else {
                smsManager.sendTextMessage(parsePhoneNumber(phoneNumber.getText().toString()), null, message.getText().toString(), PendingIntent.getBroadcast(this, 0, new Intent(getString(R.string.pi_sms_sent)), 0), PendingIntent.getBroadcast(this, 0, new Intent(getString(R.string.pi_sms_delivered)), 0));
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
                    Toast.makeText(getBaseContext(), "Permission denied", Toast.LENGTH_LONG).show();
                }

                break;
            }
            case REQUEST_CONTACT:
            {
                int i = Arrays.binarySearch(permissions, Manifest.permission.READ_CONTACTS);
                if(i >= 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    selectContact();
                } else {
                    Toast.makeText(getBaseContext(), "Permission denied", Toast.LENGTH_LONG).show();
                }

                break;
            }
            default:
            {
                throw new IllegalArgumentException(getString(R.string.error_invalid_request_code, requestCode));
            }
        }
    }

    public boolean getPermissions(){
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, SEND_SMS_REQUEST);
        }
        return hasPermission;
    }

    public String parsePhoneNumber(String pNumber){
        String parsed = pNumber.replaceAll(" ", "");
        if(!parsed.startsWith("+")){
            parsed = getText(R.string.default_country_code) + pNumber;
        }
        if(!PhoneNumberUtils.isGlobalPhoneNumber(pNumber)){
            throw new IllegalArgumentException(getString(R.string.error_invalid_ph_number, pNumber));
        }

        return parsed;
    }
}
