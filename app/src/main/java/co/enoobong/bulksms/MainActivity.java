package co.enoobong.bulksms;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.Manifest.permission.READ_CONTACTS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int FILE_SELECT_CODE = 1;
    private static final String TAG = "MainActivity";
    private TextInputEditText phoneNumber, message;
    private FloatingActionButton addFromContacts, addFromCSV;
    private static final int REQUEST_READ_CONTACTS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addFromContacts = (FloatingActionButton) findViewById(R.id.addFromContacts);
        addFromCSV = (FloatingActionButton) findViewById(R.id.addFromCSV);

        phoneNumber = (TextInputEditText) findViewById(R.id.phoneNumbers);
        message = (TextInputEditText) findViewById(R.id.message);

        addFromContacts.setOnClickListener(this);
        addFromCSV.setOnClickListener(this);

//       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
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

        phoneNumber.setText(builder);
    }

    private void getPhoneNumbersFromCSV(String uri){
        File file = new File(uri);
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
                inputStream.close();
            } catch (IOException e){
                Log.e(TAG, e.getMessage());
            }
        }
        phoneNumber.setText(builder);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK){
                    String uri = data.getDataString();
                    getPhoneNumbersFromCSV(uri);
            }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void launchFilePicker(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/.csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try{
            startActivityForResult(Intent.createChooser(intent, "Select a .csv file"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex){
            Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getPhoneNumbersFromContacts();
            } else {
                Toast.makeText(this, "Until you grant this permission you can't send messages to your contacts", Toast.LENGTH_LONG).show();
            }
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
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.addFromContacts:
                if(!mayRequestContacts()){
                    return;
                }
                getPhoneNumbersFromContacts();
                break;
            case R.id.addFromCSV:
                launchFilePicker();
                break;
            default:
                break;
        }
    }
}
