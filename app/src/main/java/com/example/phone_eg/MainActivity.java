package com.example.phone_eg;

/*
* Zadania nierozwiązane:
* Zd.3.  Napisz aplikację, która na SMSa zaczynającego się od danego prefixu  (np. [*@*]) automatycznie odpowie SMSem o dowolnej treści.
* Zd.5.  Napisz aplikację, która na SMS otrzymany z zadanego telefonu (można zaszyć go w kodzie) automatycznie odpowie.
* Zd.6.  Napisz aplikację, która w przypadku nieodebrania połączenia wyśle SMSa o treści „Oddzwonię później”.
* Zd.7.  Napisz aplikację, która SMS otrzymany z zadanego numeru wyśle na inny numer.
* Zd.8.  Napisz aplikację, która odrzuci połączenie przychodzące z zadanego numeru. (w zależności czy da się odrzucić)
* Zd.11. Napisz aplikację, która pozwala na wysyłanie SMSów seryjnych. (mamy tablicę z numerami, na które wysyłamy SMS o tej samej treści np. życzenia świąteczne)
* */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.SEND_SMS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
import static android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED;
import static android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER;
import static android.telephony.TelephonyManager.EXTRA_STATE;
import static android.telephony.TelephonyManager.EXTRA_STATE_IDLE;
import static android.telephony.TelephonyManager.EXTRA_STATE_OFFHOOK;
import static android.telephony.TelephonyManager.EXTRA_STATE_RINGING;

public class MainActivity extends AppCompatActivity {

    static final String[] RequiredPermissions = {CALL_PHONE, READ_PHONE_STATE, RECEIVE_SMS, SEND_SMS};

    static final String
            NUMBER = "NUMBER",
            MESSAGE = "MESSAGE";

    @BindView(R.id.lastRingingTime_TextEdit)
    TextView lastRingingTime_TextEdit;
    @BindView(R.id.lastCallTime_TextEdit)
    TextView lastCallTime_TextEdit;

    @BindView(R.id.lastReceivedSmsOriginNumber_TextView)
    TextView lastReceivedSmsOriginNumber_TextView;
    @BindView(R.id.lastReceivedSmsBody_TextView)
    TextView lastReceivedSmsMsg_TextView;

    @BindView(R.id.number_editText)
    EditText number_editText;
    @BindView(R.id.msg_editText)
    EditText msg_editText;

    private BroadcastReceiver telephonyBroadcastReceiver = new BroadcastReceiver() {

        private static final String NULLINTENT = "NULLINTENT";

        /* Phone stuff */
        private static final String APPSTART = "APPSTART";

        private String lastPhoneState = APPSTART;
        private long lastPhoneEventMoment = System.nanoTime();

        /* Sms stuff */
        private static final String SMS_EXTRA_NAME = "pdus";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? NULLINTENT : intent.getAction();
            if(action == null)
                return;

            if(action.equals(ACTION_PHONE_STATE_CHANGED)) {
                long now = System.nanoTime(); // SystemClock.elapsedRealtimeNanos() - is better (but requires API 17)
                String state = intent.getStringExtra(EXTRA_STATE);
                String incomingNumber = intent.getStringExtra(EXTRA_INCOMING_NUMBER);

                // Zd.1  Napisz aplikację, która w przypadku nadchodzącego połączenia telefonicznego wypisze komunikat (toast) o tym, że nadchodzi połączenie z konkretnego numeru.
                callInfo(context, incomingNumber, state);

                // Zd.2 Napisz aplikację, która mierzy czas trwania połączenia.
                measureRingingTime(now);
                measureOffhookTime(now);

                // Zd.10 Napisz aplikację, która wypisuje informacje przy każdej zmianie stanu telefonu. (IDLE, OFFHOOK, RINGING)
                currentPhoneState(context, state);

                lastPhoneState = state;
                lastPhoneEventMoment = System.nanoTime(); // SystemClock.elapsedRealtimeNanos()
            }

            else if(action.equals(SMS_RECEIVED_ACTION)) {
                Bundle bundle;
                Object[] pdus;

                bundle = intent.getExtras();
                if(bundle == null)
                    return;
                pdus = (Object[]) bundle.get(SMS_EXTRA_NAME);
                if(pdus == null)
                    return;

                for(Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    lastReceivedSmsMsg_TextView.setText(getString(
                            R.string.lastReceivedSmsBody,
                            smsMessage.getMessageBody()
                    ));
                    lastReceivedSmsOriginNumber_TextView.setText(getString(
                            R.string.lastReceivedSmsOriginNumber,
                            smsMessage.getOriginatingAddress()
                    ));
                }
            }
        }

        private void currentPhoneState(Context context, String state) {
            Toast.makeText(context, context.getString(R.string.reportState_toastText, lastPhoneState, state), Toast.LENGTH_SHORT).show();
        }

        private void callInfo(Context context, String incomingNumber, String state) {
            if(state.equals(EXTRA_STATE_RINGING))
                Toast.makeText(
                        context,
                        context.getString(R.string.reportStateRinging_toastText, incomingNumber, lastPhoneState, state),
                        Toast.LENGTH_SHORT
                ).show();
            else if(lastPhoneState.equals(EXTRA_STATE_RINGING) && state.equals(EXTRA_STATE_IDLE))
                Toast.makeText(
                        context,
                        context.getString(R.string.unansweredCall_toastText),
                        Toast.LENGTH_SHORT
                ).show();
        }

        private void measureRingingTime(long now) {
            if(lastPhoneState.equals(EXTRA_STATE_RINGING))
                lastRingingTime_TextEdit.setText(getString(R.string.lastRingingTime, (now - lastPhoneEventMoment)*10e-10));
        }
        private void measureOffhookTime(long now) {
            if(lastPhoneState.equals(EXTRA_STATE_OFFHOOK))
                lastCallTime_TextEdit.setText(getString(R.string.lastCallTime, (now - lastPhoneEventMoment)*10e-10));
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putCharSequence(NUMBER, number_editText.getText());
        savedInstanceState.putCharSequence(MESSAGE, msg_editText.getText());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> neededPermissions = new ArrayList<>();
        for(String permission : RequiredPermissions) {
            int selfPermission = ActivityCompat.checkSelfPermission(this, permission);
            if (selfPermission != PERMISSION_GRANTED)
                neededPermissions.add(permission);
        }
        if(!neededPermissions.isEmpty())
            ActivityCompat.requestPermissions(
                    this,
                    neededPermissions.toArray(new String[neededPermissions.size()]),
                    PERMISSION_GRANTED);

        ButterKnife.bind(this);

        if(savedInstanceState != null) {
            number_editText.setText(savedInstanceState.getCharSequence(NUMBER));
            msg_editText.setText(savedInstanceState.getCharSequence(MESSAGE));
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PHONE_STATE_CHANGED);
        intentFilter.addAction(SMS_RECEIVED_ACTION);
        registerReceiver(telephonyBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(telephonyBroadcastReceiver);
    }

    // Zd.4. Napisz aplikację, która pozwoli wysłać SMSa o dowolnej treści na wskazany numer telefonu.
    @OnClick(R.id.send_button)
    public void send_button_onClick(Button b) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(
                    number_editText.getText().toString(),
                    null,
                    msg_editText.getText().toString(),
                    null,
                    null);
            Toast.makeText(this, R.string.sent_toastText, Toast.LENGTH_SHORT).show();

        } catch(Exception e) {
            Toast.makeText(this, R.string.failedToSend_toastText, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Zd.9.  Napisz aplikację, która pozwala z jej poziomu zadzwonić na zadany numer.
    @OnClick(R.id.call_button)
    public void call_button_onClick(Button b) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse(getString(R.string.tel, number_editText.getText().toString())));
        startActivity(intent);
    }
}
