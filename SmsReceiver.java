package com.example.radiostreamsms;

import android.app.DirectAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

//==================================================
//
// SmsReceiver Class
//
//==================================================
public class SmsReceiver extends BroadcastReceiver {

    final static String LOG_TAG = "SMS Recever";
    public static String SMSMessage = "";

    @Override
    @SuppressWarnings("deprecation")
    public void onReceive (Context context, Intent intent) {
        MainActivity rsagente = new MainActivity(); // Class instance MainActivity
        Bundle intentExtras = intent.getExtras();

        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {

            Log.d(LOG_TAG, "== == == ==| RadioSTREAMSMS SMS received |== == == ==");
            Log.d(LOG_TAG, "== == == ==| RadioSTREAMSMS SMS received |== == == ==");
            Log.d(LOG_TAG, "== == == ==| RadioSTREAMSMS SMS received |== == == ==");
            Log.d(LOG_TAG, "== == == ==| RadioSTREAMSMS SMS received |== == == ==");
            Log.d(LOG_TAG, "== == == ==| RadioSTREAMSMS SMS received |== == == ==");
            Log.d(LOG_TAG, "== == == ==| RadioSTREAMSMS SMS received |== == == ==");

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[])bundle.get("pdus");
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                String format = bundle.getString("format");
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                Log.d(LOG_TAG, "== == == ==| PDUS Lenght |== == == == " + String.valueOf(pdus.length));

                if (messages.length > -1) {
                    // Toast.makeText(context, "Message recieved: " + messages[0].getMessageBody(), Toast.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "== == == ==| SMS message Body |== == == == " + messages[0].getMessageBody().toString());
                    SMSMessage = messages[0].getMessageBody().toString();
                    rsagente.DecodeSMS(SMSMessage);
                }
            }



            return;
        }

        //read sms

        //// onData("sms received");
    }

    ////nrotected abstract void onData(String data);
}
