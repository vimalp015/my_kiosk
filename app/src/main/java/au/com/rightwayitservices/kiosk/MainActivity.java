package au.com.rightwayitservices.kiosk;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import au.com.rightwayitservices.kiosk.R;

public class MainActivity extends Activity {

    private int permissionCheck;
    private PackageManager mPackageManager;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);

        mPackageManager = this.getPackageManager();

        Button lockTaskButton = findViewById(R.id.start_lock_button);
        lockTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //does not work :(
//                try {
//                    Runtime.getRuntime().exec("au.com.rightwayitservices.kiosk/.DeviceAdminReceiver");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                if (mDevicePolicyManager.isDeviceOwnerApp(
                        getApplicationContext().getPackageName())) {
                    Intent lockIntent = new Intent(getApplicationContext(),
                            LockedActivity.class);

                    mPackageManager.setComponentEnabledSetting(
                            new ComponentName(getApplicationContext(),
                                    LockedActivity.class),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    startActivity(lockIntent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.not_lock_whitelisted, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        Button resetDeviceOwnerButton = findViewById(R.id.remove_device_admin_button);
        resetDeviceOwnerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDevicePolicyManager.clearDeviceOwnerApp("au.com.rightwayitservices.kiosk");

                //Use below code via adb to set device owner
                //adb shell dpm set-device-owner au.com.rightwayitservices.kiosk/.DeviceAdminReceiver
            }
        });

        // Check to see if permission to access external storage is granted,
        // and request if not
        permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        // Check to see if started by LockActivity and disable LockActivity if so
        Intent intent = getIntent();

        if (intent.getIntExtra(LockedActivity.LOCK_ACTIVITY_KEY, 0) ==
                LockedActivity.FROM_LOCK_ACTIVITY) {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getPackageName());
            mPackageManager.setComponentEnabledSetting(
                    new ComponentName(getApplicationContext(), LockedActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, results array is empty
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionCheck = grantResults[0];
                } else {

                }
                return;
            }
        }
    }
}
