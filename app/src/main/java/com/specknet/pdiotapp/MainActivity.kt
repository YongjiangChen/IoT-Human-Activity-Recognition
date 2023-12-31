package com.specknet.pdiotapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.*
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService
import com.specknet.pdiotapp.bluetooth.ConnectingActivity
import com.specknet.pdiotapp.live.ClassifyActivity
import com.specknet.pdiotapp.onboarding.OnBoardingActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

// https://www.digitalocean.com/community/tutorials/android-navigation-drawer-example-tutorial
class MainActivity : ActivitySuperclass() {

    override var currentContentView = R.layout.host

    // home buttons:
    lateinit var loginButton: SignInButton
    lateinit var connectButton: Button

    // connect fragment:
    val REQUEST_CODE_SCAN_RESPECK = 0
    private lateinit var scanRespeckButton: Button
    private lateinit var connectSensorsButton: Button
    private lateinit var restartConnectionButton: Button
    private lateinit var thingyID: EditText
    private lateinit var respeckID: EditText
    lateinit var sharedPreferences: SharedPreferences

    var nfcAdapter: NfcAdapter? = null
    val MIME_TEXT_PLAIN = "application/vnd.bluetooth.le.oob"
    private val TAG = "NFCReader"

    // gso:
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private var userSignedIn: Boolean = false

    // permissions:
    lateinit var permissionAlertDialog: AlertDialog.Builder
    val permissionsForRequest = arrayListOf<String>()

    var locationPermissionGranted = false
    var cameraPermissionGranted = false
    var bluetoothPermissionGranted = false
    var readStoragePermissionGranted = false
    var writeStoragePermissionGranted = false

    // broadcast receiver:
    val filter = IntentFilter()

    var isUserFirstTime = false

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // check whether the onboarding screen should be shown
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.PREF_USER_FIRST_TIME)) {
            isUserFirstTime = false
        }
        else {
            isUserFirstTime = true
            sharedPreferences.edit().putBoolean(Constants.PREF_USER_FIRST_TIME, false).apply()
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
        }

        permissionAlertDialog = AlertDialog.Builder(this)
        setupClickListeners()
        setupBluetoothService()

        // register a broadcast receiver for respeck status
        filter.addAction(Constants.ACTION_RESPECK_CONNECTED)
        filter.addAction(Constants.ACTION_RESPECK_DISCONNECTED)

        if (Utils.userSignedIn)
        {
            updateUIPostSignIn()
        }
        if (Utils.currentView == 0)
        {
            setupPermissions()
        }
    }

    private fun startSpeckService() {
        val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
        Log.i("service","isServiceRunning = " + isServiceRunning)

        if (!isServiceRunning) {
            Log.i("service", "Starting BLT service")
            val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
            this.startService(simpleIntent)
        }
        else {
            Log.i("service", "Service already running, restart")
            this.stopService(Intent(this, BluetoothSpeckService::class.java))
            Toast.makeText(this, "restarting service with new sensors", Toast.LENGTH_SHORT).show()
            this.startService(Intent(this, BluetoothSpeckService::class.java))

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        Log.w("SIGNING", "IN")
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)
            val personName: String? = account.displayName
            UserAccount.getAccount().setGoogleAccount(account)
            updateUIPostSignIn()
        } catch (e: ApiException) {
            Log.w("Hello", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun updateUIPostSignIn()
    {
        Utils.userSignedIn = true
        var loginContainer: LinearLayout = findViewById(R.id.login_container)
        loginContainer.visibility = View.GONE

        // get username if exists:
        var nameString: String? = ""
        var account = UserAccount.getAccount().googleAccount
        if (account != null)
        {
            nameString = UserAccount.getAccount().googleAccount.givenName
            var userDP: String? = account.photoUrl.toString()
            if (userDP != null) {
                Glide.with(this).load(userDP).into(findViewById(R.id.user_dp))
                var dp: ImageView = findViewById(R.id.user_dp)
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                {
                    //dp.setColorFilter(R.color.dark_background, PorterDuff.Mode.DST)
                } else {
                    //dp.setColorFilter(R.color.light_main_background, PorterDuff.Mode.DST)
                }
            }
        } else {

        }

        // set display name
        val hello: TextView = findViewById(R.id.welcome)
        var text = "Welcome"
        if (!isUserFirstTime)
        {
            text = "$text back"
        }
        hello.text = "$text\n$nameString"
    }

    /*
    override fun onResume()
    {
        super.onResume()
        var nameString: String? = "unknown"
        var account = UserAccount.getAccount().googleAccount
        if (account != null)
        {
            nameString = UserAccount.getAccount().googleAccount.displayName
            if (nameString != null) {
                Log.w("1", nameString)
            }
            var userDP: String? = account.photoUrl.toString()
            if (userDP != null) {
                Log.w("1", userDP)
            }
            if (userDP != "null") Glide.with(this).load(userDP).into(findViewById(R.id.user_dp))
        }

        // set display name
        val hello: TextView = findViewById(R.id.hello_user)
        hello.text = "Hello $nameString"
    }
    */
    fun setupClickListeners() {
        loginButton = findViewById(R.id.sign_in_button)
        loginButton.setOnClickListener {
            val signInIntent: Intent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1);
        }

        connectButton = findViewById(R.id.connect_sensors_button)
        connectButton.setOnClickListener {
            selectItem(1)
        }
    }

    fun setupPermissions() {
        // request permissions

        // location permission
        Log.i("Permissions", "Location permission = " + locationPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsForRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsForRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        else {
            locationPermissionGranted = true
        }

        // camera permission
        Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
            permissionsForRequest.add(Manifest.permission.CAMERA)
        }
        else {
            cameraPermissionGranted = true
        }

        // bluetooth permission
        Log.i("Permissions", "bluetooth permission = " + cameraPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
            permissionsForRequest.add(Manifest.permission.BLUETOOTH)
        }
        else {
            cameraPermissionGranted = true
        }

        // read storage permission
        Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else {
            readStoragePermissionGranted = true
        }

        // write storage permission
        Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        else {
            writeStoragePermissionGranted = true
        }

        if (permissionsForRequest.size >= 1) {
            ActivityCompat.requestPermissions(this,
                permissionsForRequest.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS)
        }

    }

    fun setupBluetoothService() {
        val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
        Log.i("debug","isServiceRunning = " + isServiceRunning)

        // check sharedPreferences for an existing Respeck id
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID, starting service and attempting to reconnect")

            // launch service to reconnect
            // start the bluetooth service if it's not already running
            if(!isServiceRunning) {
                Log.i("service", "Starting BLT service")
                val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
                this.startService(simpleIntent)
            }
        }
        else {
            Log.i("sharedpref", "No Respeck seen before, must pair first")
            // TODO then start the service from the connection activity
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if(grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    when(permissionsForRequest[i]) {
                        Manifest.permission.ACCESS_COARSE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.CAMERA -> cameraPermissionGranted = true
                        Manifest.permission.READ_EXTERNAL_STORAGE -> readStoragePermissionGranted = true
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> writeStoragePermissionGranted = true
                    }

                }
            }
        }

        // count how many permissions need granting
        var numberOfPermissionsUngranted = 0
        if (!locationPermissionGranted) numberOfPermissionsUngranted++
        if (!cameraPermissionGranted) numberOfPermissionsUngranted++
        if (!readStoragePermissionGranted) numberOfPermissionsUngranted++
        if (!writeStoragePermissionGranted) numberOfPermissionsUngranted++

        // show a general message if we need multiple permissions
        if (numberOfPermissionsUngranted > 1) {
            val generalSnackbar = Snackbar
                .make(coordinatorLayout, "Several permissions are needed for correct app functioning", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS") {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                .show()
        }
        else if(numberOfPermissionsUngranted == 1) {
            var snackbar: Snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_LONG)
            if (!locationPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Location permission needed for Bluetooth to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!cameraPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Camera permission needed for QR code scanning to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!readStoragePermissionGranted || !writeStoragePermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Storage permission needed to record sensor.",
                        Snackbar.LENGTH_LONG
                    )
            }

            snackbar.setAction("SETTINGS") {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(settingsIntent)
            }
                .show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.show_tutorial) {
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}