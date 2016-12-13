package co.enoobong.bulksms;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.SEND_SMS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int FILE_SELECT_CODE = 1;
    private static final String TAG = "MainActivity";
    private static final String SMS_SENT = "sent";
    private static final String SMS_DELIVERED = "delivered";
    private static final int REQUEST_READ_CONTACTS = 0;
    private static final int REQUEST_READ_STORAGE = 1;
    private static final int REQUEST_SEND_SMS = 2;
    private TextInputEditText phoneNumberET, messageET;
    private FloatingActionButton addFromContacts, addFromCSV;
    private Button sendMessage;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = null;
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    message = "Message Sent";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    message = "Error. Message not sent.";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    message = "Error: No service.";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    message = "Error: Null PDU.";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    message = "Error: Radio off.";
                    break;
            }

            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addFromContacts = (FloatingActionButton) findViewById(R.id.addFromContacts);
        addFromCSV = (FloatingActionButton) findViewById(R.id.addFromCSV);

        phoneNumberET = (TextInputEditText) findViewById(R.id.phoneNumbers);
        messageET = (TextInputEditText) findViewById(R.id.message);

        sendMessage = (Button) findViewById(R.id.sendButton);

        addFromContacts.setOnClickListener(this);
        addFromCSV.setOnClickListener(this);
        sendMessage.setOnClickListener(this);
        registerReceiver(receiver, new IntentFilter(SMS_SENT));

//       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addFromContacts:
                if (!mayRequestContacts()) {
                    return;
                }
                getPhoneNumbersFromContacts();
                break;
            case R.id.addFromCSV:
                if (!mayRequestExternalStorage()) {
                    return;
                }
                launchFilePicker();
                break;
            case R.id.sendButton:
                if (!maySendSMS()) {
                    return;
                }
                String[] phoneNumbers = phoneNumberET.getText().toString().split(" ");
                String message = messageET.getText().toString().trim();
                sendMessage(phoneNumbers, message);
                break;
            default:
                break;
        }
    }

    private void getPhoneNumbersFromContacts(){
        Cursor managedCursor = getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone._ID,  ContactsContract.CommonDataKinds.Phone.NUMBER},
                        null, null, null);
        StringBuilder builder = new StringBuilder();
        try{
            while (managedCursor.moveToNext()) {
                builder.append(managedCursor.getString(managedCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))).append(" ");
            }
        } finally {
            managedCursor.close();
        }

        phoneNumberET.setText(builder);
    }

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String filePath = uri.getPath();
                    String[] projection = {MediaStore.Files.FileColumns.DATA};
                    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                    cursor.moveToFirst();
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                    Log.d(TAG, filePath + " " + path);
                    cursor.close();
                    getPhoneNumbersFromCSV(filePath);
                }
                break;
            default:
                break;
        }
    }

    private void getPhoneNumbersFromCSV(String uri){
        File file = new File(uri);
        Log.d(TAG, file.toString());
        StringBuilder builder = new StringBuilder();
        InputStream inputStream = null;
        try{
            inputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(" ");
            }

        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        } finally {
            try{
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e){
                Log.e(TAG, e.getMessage());
            }
        }
        phoneNumberET.setText(builder);
    }

    private void sendMessage(String[] phoneNumbers, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);
        for (String phoneNumber : phoneNumbers) {
            smsManager.sendTextMessage(phoneNumber, null, message, piSend, piDelivered);
        }

    }

    public String getPath(Uri uri) {

        String path;
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null) {
            path = uri.getPath();
            Log.d(TAG, path);
        } else {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            Log.d(TAG, path);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPhoneNumbersFromContacts();
                } else {
                    Toast.makeText(this, "Until you grant this permission you can't send messages to your contacts", Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_READ_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchFilePicker();
                } else {
                    Toast.makeText(this, "Until you grant this permission you can't send messages to your contacts", Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_SEND_SMS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    String[] phoneNumbers = phoneNumberET.getText().toString().split(" ");
                    String message = messageET.getText().toString().trim();
                    sendMessage(phoneNumbers, message);

                } else {
                    Toast.makeText(this, "Until you grant this permission you can't send messages to your contacts", Toast.LENGTH_LONG).show();
                }
                break;
        }

    }

    private boolean mayRequestContacts(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)){
            Snackbar.make(addFromContacts, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View view) {
                            requestPermissions(new String[] {READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }

        return false;
    }

    private boolean mayRequestExternalStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
            Snackbar.make(addFromCSV, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View view) {
                            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
        }

        return false;

    }

    private boolean maySendSMS() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(SEND_SMS)) {
            Snackbar.make(sendMessage, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View view) {
                            requestPermissions(new String[]{SEND_SMS}, REQUEST_SEND_SMS);
                        }
                    });
        } else {
            requestPermissions(new String[]{SEND_SMS}, REQUEST_SEND_SMS);
        }

        return false;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
