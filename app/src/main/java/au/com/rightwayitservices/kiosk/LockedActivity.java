package au.com.rightwayitservices.kiosk;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import au.com.rightwayitservices.kiosk.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LockedActivity extends Activity {

    private Button previousButton;
    private Button nextButton;
    private TextView questionText;
    private RadioButton yesRadioButton;
    private RadioButton noRadioButton;
    private ComponentName adminComponentName;
    private DevicePolicyManager devicePolicyManager;

    public static final String LOCK_ACTIVITY_KEY = "lock_activity";
    public static final int FROM_LOCK_ACTIVITY = 1;


    private List<String> questions = new ArrayList<>(
            Arrays.asList(
                    "Are you satisfied with the services?",
                    "Did a representative attend you within 5 minutes of your entering the store?",
                    "Would you like to recommend our store to friends and family?",
                    "Are you a resident of Sydney?",
                    "Would you visit our store again in next one week?"
            ));
    private Boolean[] response = new Boolean[5];
    private int currentQuestion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked);

        devicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        yesRadioButton = findViewById(R.id.yes_radio);
        noRadioButton = findViewById(R.id.no_radio);

        // Setup stop lock task button
        Button stopLockButton = findViewById(R.id.stop_lock_button);
        stopLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAlert();
            }
        });

        questionText = findViewById(R.id.question_text);
        questionText.setText(questions.get(currentQuestion));

        // Setup PREVIOUS button
        previousButton = findViewById(R.id.previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processResponse();
                if (currentQuestion > 0) {
                    currentQuestion = currentQuestion - 1;
                    questionText.setText(questions.get(currentQuestion));
                }
                previousButton.setEnabled(currentQuestion > 0);
                nextButton.setEnabled(true);
                displayResponse();
            }
        });


        // Setup NEXT button
        nextButton = findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processResponse();
                if (currentQuestion < questions.size() - 1) {
                    currentQuestion = currentQuestion + 1;
                    questionText.setText(questions.get(currentQuestion));
                }
                nextButton.setEnabled(currentQuestion < questions.size() - 1);
                previousButton.setEnabled(true);
                displayResponse();
            }
        });

        // Set Default My KIOSK policy
        adminComponentName = DeviceAdminReceiver.getComponentName(this);
        devicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            setDefaultMyKioskPolicies(true);
        } else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_device_owner, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // start lock task mode if its not already active
        if (devicePolicyManager.isLockTaskPermitted(this.getPackageName())) {
            ActivityManager am = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE);
            if (am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void setDefaultMyKioskPolicies(boolean active) {
        // set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        // TODO: need to study on the below feature before enabling it
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, false);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

        // disable keyguard and status bar
        devicePolicyManager.setKeyguardDisabled(adminComponentName, active);
        devicePolicyManager.setStatusBarDisabled(adminComponentName, active);

        // enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);

        // set system update policy
        if (active) {
            devicePolicyManager.setSystemUpdatePolicy(adminComponentName,
                    SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            devicePolicyManager.setSystemUpdatePolicy(adminComponentName,
                    null);
        }

        // set this Activity as a lock task package
        devicePolicyManager.setLockTaskPackages(adminComponentName,
                active ? new String[]{getPackageName()} : new String[]{});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        if (active) {
            // set KIOSK activity as home intent receiver so that it is started
            // on reboot
            devicePolicyManager.addPersistentPreferredActivity(
                    adminComponentName, intentFilter, new ComponentName(
                            getPackageName(), LockedActivity.class.getName()));
        } else {
            devicePolicyManager.clearPackagePersistentPreferredActivities(
                    adminComponentName, getPackageName());
        }
    }

    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            devicePolicyManager.addUserRestriction(adminComponentName,
                    restriction);
        } else {
            devicePolicyManager.clearUserRestriction(adminComponentName,
                    restriction);
        }
    }

    private void enableStayOnWhilePluggedIn(boolean enabled) {
        if (enabled) {
            devicePolicyManager.setGlobalSetting(
                    adminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                            | BatteryManager.BATTERY_PLUGGED_USB
                            | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            devicePolicyManager.setGlobalSetting(
                    adminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    "0"
            );
        }
    }

    private void showDialogAlert() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter password");

        // Set up the input
        final EditText input = new EditText(this);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("unlock", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().equals("unlock")) {
                    ActivityManager am = (ActivityManager) getSystemService(
                            Context.ACTIVITY_SERVICE);

                    if (am.getLockTaskModeState() ==
                            ActivityManager.LOCK_TASK_MODE_LOCKED) {
                        stopLockTask();
                    }

                    setDefaultMyKioskPolicies(false);

                    Intent intent = new Intent(
                            getApplicationContext(), MainActivity.class);

                    intent.putExtra(LOCK_ACTIVITY_KEY, FROM_LOCK_ACTIVITY);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Wrong password!!", Toast.LENGTH_SHORT).show();
                }
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void processResponse() {
        if (yesRadioButton.isChecked()) {
            response[currentQuestion] = true;
        } else if (noRadioButton.isChecked()) {
            response[currentQuestion] = false;
        } else {
            response[currentQuestion] = null;
        }
    }

    private void displayResponse() {
        RadioGroup radioGroup = findViewById(R.id.radio_group);
        if (response[currentQuestion] != null && response[currentQuestion]) {
            radioGroup.check(R.id.yes_radio);
        } else if (response[currentQuestion] != null && !response[currentQuestion]) {
            radioGroup.check(R.id.no_radio);
        } else {
            radioGroup.clearCheck();
        }
    }
}
