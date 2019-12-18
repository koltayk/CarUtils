package com.android.kk.changemap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangeMapMainActivity extends AppCompatActivity {

    private static final Map<String, String> programs = new HashMap<>();  // Map program name -> program directory

    public static String getDir(String text) {
        return programs.get(text);
    }

    private static final Map<String, String> vendors = new HashMap<>();  // Map text -> code

    public static String getCode(String text) {
        return vendors.get(text);
    }

    public static String getText(String code) {
        for (String text: vendors.keySet()) {
            if (vendors.get(text).equals(code)) {
                return text;
            }
        }
        return null;
    }

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public static final String STORAGE = "/sdcard/";
    public static final String SYS_TXT = "sys.txt";
    private static final String FIND_IGO_SYSTXT = "find -L " + STORAGE + " -name " + SYS_TXT;
//    private static final Pattern pattern1 = Pattern.compile(".*", Pattern.DOTALL);
//    public static final String CONTENT_PATH = "/sdcard1/home/odroid/igo/igoContent";
//    public static final String CONTENT_PATH = "/sdcard1/media/0/iGOcontent/";
    public static final String CONTENT_PATH = "/storage/extSdCard/iGOcontent/";
    public static final String REGEX = "(.*content=" + CONTENT_PATH + "content_)(..)(.*)";
    private static final Pattern pattern = Pattern.compile(REGEX, Pattern.DOTALL);

    private Matcher matcher;
    private String contentPref;
    private String sysTxtOut = null;
    private String oldVendor = null;
    private String newVendor = null;
    private String fileName = null;

    // Define the object for Radio Group,
    // Submit and Clear buttons
    private RadioGroup selectGroup;
    private RadioGroup changeGroup;
    Button submit, abort;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

//        {
//            programs.put(getString(R.string.Avic), "iGO_Avic");
//            programs.put(getString(R.string.Isr), "iGO_Isr");
//            programs.put(getString(R.string.Pal), "iGO_Pal");
//        }

        {
            vendors.put(getString(R.string.radio_here), "HR");
            vendors.put(getString(R.string.radio_tomtom), "TT");
            vendors.put(getString(R.string.radio_nng), "HU");
        }
        super.onCreate(savedInstanceState);
        initSelect();
    }

    private void changeMap(String dir) {
        getVendor(dir);
        setContentView(R.layout.activity_change_map_main);
        changeGroup = findViewById(R.id.radioGroupChangeMap);
        submit = findViewById(R.id.submit);
        abort = findViewById(R.id.abort);
        TextView text = findViewById(R.id.progName);
        text.setText(dir);

        // Add the Listener to the Submit Button
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                // When submit button is clicked,
                // Ge the Radio Button which is set
                // If no Radio Button is set, -1 will be returned
                int selectedId = changeGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(ChangeMapMainActivity.this,"semmi sincs kijelölve", Toast.LENGTH_SHORT).show();
                }
                else {
                    RadioButton radioButton = (RadioButton)changeGroup.findViewById(selectedId);
                    final String text = radioButton.getText().toString();
                    newVendor = getCode(text);
                    if (!oldVendor.equals(newVendor)) {
                        sysTxtOut = matcher.group(1) + newVendor + matcher.group(3);
                        try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
                            out.print(sysTxtOut);
                            String msg = "új térképszolgáltató: " + newVendor;
                            Log.d("kklog", msg);
                            // Now display the value of selected item by the Toast message
                            Toast.makeText(ChangeMapMainActivity.this, text, Toast.LENGTH_SHORT).show();
                            android.os.Process.killProcess(android.os.Process.myPid()); // kilép
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            Log.d("kklog", e.getMessage());
                            Toast.makeText(ChangeMapMainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        // Add the Listener to the Abort Button
        abort.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                android.os.Process.killProcess(android.os.Process.myPid()); // kilép
            }
        });

        // Uncheck or reset the radio buttons initially
        initCheck();
    }

    private void initCheck() {
        int count = changeGroup.getChildCount();
        List<RadioButton> listOfRadioButtons = new ArrayList<RadioButton>();
        for (int i=0;i<count;i++) {
            View o = changeGroup.getChildAt(i);
            if (o instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) o;
                if (radioButton.getText().equals(getText(oldVendor))) {
                    radioButton.setChecked(true);
                }
            }
        }
    }

    private void checkPermission(String permission, int callBack) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, ask it.
            ActivityCompat.requestPermissions(this, new String[]{permission}, callBack);
        }
    }
//
//    private void initSelect() {
//        int count = selectGroup.getChildCount();
//        for (int i=0;i<count;i++) {
//            View o = selectGroup.getChildAt(i);
//            if (o instanceof RadioButton) {
//                RadioButton radioButton = (RadioButton) o;
//                radioButton.setChecked(false);
//            }
//        }
//    }

    private void initSelect() {
        try {
//            Process process = Runtime.getRuntime().exec(FIND_IGO_SYSTXT);
//            String inpStream = readFullyAsString(process.getInputStream(), Charset.defaultCharset().name());
//            String errStream = readFullyAsString(process.getErrorStream(), Charset.defaultCharset().name());
//            Log.d("kklog", inpStream);
//            Log.d("kklog", errStream);
//            String[] split = inpStream.split("\n");
            String[] split = {"/sdcard/iGO_Avic/sys.txt", "/sdcard/iGO_Pal/sys.txt"};
            List<String> fileNames = new ArrayList<>();
            for (String filePath: split) {
                if (filePath.startsWith(STORAGE)) {
                    fileNames.add(filePath);
                }
            }
            switch (fileNames.size()) {
                case 0:
                    break;
                case 1:
                    changeMap(fileNames.get(0));
                    break;
                default:
                    setContentView(R.layout.activity_select_program);
                    // Bind the components to their respective objects
                    // by assigning their IDs
                    // with the help of findViewById() method
                    selectGroup = findViewById(R.id.selectProgramGroup);
                    for (String filePath: fileNames) {
                        adProgButton(filePath);
                    }

                    // Add the Listener to the RadioGroup
                    selectGroup.setOnCheckedChangeListener(
                        new RadioGroup.OnCheckedChangeListener() {
                            @Override

                            // The flow will come here when
                            // any of the radio buttons in the radioGroup
                            // has been clicked

                            // Check which radio button has been clicked
                            public void onCheckedChanged(RadioGroup group, int checkedId)
                            {
                                // Get the selected Radio Button
                                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                                final String text = radioButton.getText().toString();
                                changeMap(text);
                            }
                        }
                    );
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adProgButton(String filePath) {
        String[] pathParts = filePath.split("/");
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(pathParts[2]);
        radioButton.setTextSize(2,36);
        RadioGroup.LayoutParams layoutParam = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT, 1f);
        radioButton.setLayoutParams(layoutParam);
        radioButton.setEnabled(true);
        selectGroup.addView(radioButton);
    }

    private void getVendor(String dir) {
        this.fileName = STORAGE + dir + "/" + SYS_TXT;
        String sysTxt = null;
        File file = new File(fileName);
        try (FileInputStream inputStream = new FileInputStream(file)){
            sysTxt = readFullyAsString(inputStream, Charset.defaultCharset().name());
            matcher = pattern.matcher(sysTxt);
            if (matcher.matches()) {
                oldVendor = matcher.group(2);
                Log.d("kklog", "old content: " + oldVendor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//
//    // The dialog fragment receives a reference to this Activity through the
//    // Fragment.onAttach() callback, which it uses to call the following methods
//    // defined by the NoticeDialogFragment.NoticeDialogListener interface
//
//    public void onDialogPositiveClick(DialogFragment dialog) {
//        try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
//            out.print(sysTxtOut);
//            String msg = "új térképszolgáltató: " + newVendor;
//            Log.d("kklog", msg);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        finish();
//    }
//
//
//    public void onDialogNegativeClick(DialogFragment dialog) {
//        // User touched the dialog's negative button
//        finish();
//    }
//
//    public void onRadioButtonClicked(View view) {
//        // Is the button now checked?
//        boolean checked = ((RadioButton) view).isChecked();
//
//        // Check which radio button was clicked
//        switch(view.getId()) {
//            case R.string.radio_here:
//                if (checked)
//                    newVendor = "HR";
//                break;
//            case R.string.radio_tomtom:
//                if (checked)
//                    newVendor = "TT";
//                break;
//            case R.string.radio_nng:
//                if (checked)
//                    newVendor = "TM";
//                break;
//        }
//        getVendor();
//    }

    public static String readFullyAsString(InputStream inputStream, String encoding) throws IOException {
        return readFully(inputStream).toString(encoding);
    }

    public static byte[] readFullyAsBytes(InputStream inputStream) throws IOException {
        return readFully(inputStream).toByteArray();
    }

    public static ByteArrayOutputStream readFully(InputStream inputStream)  throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
    }

}
