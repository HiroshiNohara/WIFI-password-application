package com.app.wifipassword;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import com.app.wifipassword.zxing.android.CaptureActivity;
import com.app.wifipassword.zxing.encode.CodeCreator;

import org.litepal.LitePal;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WifiActivity extends BaseActivity implements WifiAdapter.onItemClickListener {

    private SharedPreferences pref;
    private List<Wifi> wifiList = new ArrayList<>();
    private List<Wifi> searchList = new ArrayList<>();
    private RecyclerView recyclerView;
    private WrapContentGridLayoutManager layoutManager;
    private WifiAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private SearchView searchView;
    private boolean isExpand = false;
    private AnimatorSet settingsAnimator;
    private AnimatorSet scanningAnimator;
    private AnimatorSet selfWifiAnimator;
    private int REQUEST_CODE_LOCATION = 0;
    private int REQUEST_CODE_SCAN = 1;
    private boolean isHideWifi = true;
    private boolean wifiFlag = false;
    private Point point = new Point();
    private Spinner spinner;
    private List<String> wifiTypeList;
    private ArrayAdapter<String> spinnerAdapter;
    private String currentWifiType;
    private Map<Integer, Boolean> map = new LinkedHashMap<>();
    private boolean isInSearch = false;
    private TextView noFindHint;
    private Receiver wifiReceiver;
    private boolean isRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_wifi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.wifi_recyclerView);
        isHideWifi = pref.getBoolean("is_hide_wifi", true);

        initWifiList();
        getSupportActionBar().setTitle(wifiList.size() == 0 ? this.getString(R.string.list_title) : this.getString(R.string.list_title) + "(" + wifiList.size() + this.getString(R.string.list_number));
        layoutManager = new WrapContentGridLayoutManager(this, 1);
        recyclerView.setLayoutManager(new WrapContentGridLayoutManager(WifiActivity.this, 1));
        adapter = new WifiAdapter(wifiList);
        recyclerView.setAdapter(adapter);
        adapter.setListener(this);
        recyclerView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (isRefreshing) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
        );

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doInBackground();
            }
        });

        settingsAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.floating_button);
        scanningAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.floating_button);
        selfWifiAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.floating_button);
        final FloatingActionButton button = (FloatingActionButton) findViewById(R.id.wifi_floating_button);
        final RelativeLayout floatingLayout = (RelativeLayout) findViewById(R.id.floating_layout);
        final LinearLayout settingsLayout = (LinearLayout) findViewById(R.id.settings_layout);
        final LinearLayout scanningLayout = (LinearLayout) findViewById(R.id.scanning_layout);
        final LinearLayout selfWifiLayout = (LinearLayout) findViewById(R.id.self_wifi_layout);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotate(v);
                isExpand = !isExpand;
                floatingLayout.setVisibility(isExpand ? View.VISIBLE : View.GONE);
                if (isExpand) {
                    settingsAnimator.setTarget(settingsLayout);
                    settingsAnimator.start();
                    scanningAnimator.setTarget(scanningLayout);
                    scanningAnimator.setStartDelay(150);
                    scanningAnimator.start();
                    selfWifiAnimator.setTarget(selfWifiLayout);
                    selfWifiAnimator.setStartDelay(200);
                    selfWifiAnimator.start();
                }
            }
        });
        FloatingActionButton settingsButton = (FloatingActionButton) findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotate(button);
                isExpand = !isExpand;
                floatingLayout.setVisibility(View.GONE);
                Intent intent = new Intent(WifiActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        FloatingActionButton scanningButton = (FloatingActionButton) findViewById(R.id.scanning_button);
        scanningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotate(button);
                isExpand = !isExpand;
                floatingLayout.setVisibility(View.GONE);
                startQrCode();
            }
        });
        FloatingActionButton selfWifiButton = (FloatingActionButton) findViewById(R.id.self_wifi_button);
        selfWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotate(button);
                isExpand = !isExpand;
                floatingLayout.setVisibility(View.GONE);
                QRInputPop();
            }
        });
        Intent shortcutIntent = getIntent();
        if (shortcutIntent.getStringExtra("QRMessage") != null) {
            AnalyzeQRCode(shortcutIntent);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiReceiver = new Receiver();
        registerReceiver(wifiReceiver, filter);
    }

    private void initWifiList() {
        wifiList.clear();
        getWifiInfo();
        readXml();
        if (wifiList.size() != 0) {
            List<Wifi> litePalWifi = LitePal.findAll(Wifi.class);
            List<String> wifiListName = new ArrayList<>();
            for (int i = 0; i < wifiList.size(); i++) {
                wifiListName.add(wifiList.get(i).getWifiName());
            }
            for (Wifi wifi : litePalWifi) {
                if (!wifiListName.contains(wifi.getWifiName())) {
                    LitePal.deleteAll(Wifi.class, "wifiName = ?", wifi.getWifiName());
                }
            }
            litePalWifi = LitePal.findAll(Wifi.class);
            List<String> litePalWifiName = new ArrayList<>();
            for (int i = 0; i < litePalWifi.size(); i++) {
                litePalWifiName.add(litePalWifi.get(i).getWifiName());
            }
            for (int i = 0; i < wifiList.size(); i++) {
                if (!litePalWifiName.contains(wifiList.get(i).getWifiName())) {
                    Wifi wifi = new Wifi();
                    wifi.setWifiName(wifiList.get(i).getWifiName());
                    String password = wifiList.get(i).getWifiPassword();
                    if (password != null && !password.equals("") && !password.equals("null")) {
                        wifi.setWifiPassword("presence");
                    }
                    wifi.setHighLight(false);
                    wifi.setRemark(null);
                    wifi.save();
                }
            }
            litePalWifi = isHideWifi ? LitePal.where("wifipassword = ?", "presence").find(Wifi.class) : LitePal.findAll(Wifi.class);
            if (isHideWifi) {
                List<Integer> hideList = new ArrayList<>();
                for (int i = 0; i < wifiList.size(); i++) {
                    String password = wifiList.get(i).getWifiPassword();
                    if (password == null || password.equals("") || password.equals("null")) {
                        hideList.add(i);
                    }
                }
                for (int i = 0; i < hideList.size(); i++) {
                    wifiList.remove(hideList.get(i).intValue());
                }
            }
            int index = 0;
            map.clear();
            for (Wifi wifi : wifiList) {
                if (LitePal.where("wifiname = ?", wifi.getWifiName()).find(Wifi.class).get(0).getHighLight()) {
                    map.put(index, true);
                }
                ++index;
            }
        }
    }

    private String getWifiInfo() {
        StringBuilder wifiConf = new StringBuilder();
        Process process = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;
        try {
            process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            dataInputStream = new DataInputStream(process.getInputStream());
            dataOutputStream.writeBytes("cat /data/misc/wifi/WifiConfigStore.xml\n");
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            InputStreamReader inputStreamReader = new InputStreamReader(dataInputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                wifiConf.append(line);
            }
            bufferedReader.close();
            inputStreamReader.close();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                assert process != null;
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return wifiConf.toString();
    }

    private void readXml() {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                requestLocationPermission();
            }
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo currentWifi = wifiManager.getConnectionInfo();
            String currentWifiName = currentWifi.getSSID();
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(getWifiInfo()));
            int eventType = xmlPullParser.getEventType();
            String name = "";
            String password = "";
            String type = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG: {
                        if ("string".equals(nodeName)) {
                            if (xmlPullParser.getAttributeValue(null, "name").equals("SSID")) {
                                name = xmlPullParser.nextText();
                            } else if (xmlPullParser.getAttributeValue(null, "name").equals("PreSharedKey")) {
                                password = xmlPullParser.nextText();
                            } else if (xmlPullParser.getAttributeValue(null, "name").equals("ConfigKey")) {
                                type = xmlPullParser.nextText();
                            }
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        if ("WifiConfiguration".equals(nodeName)) {
                            Wifi wifi = new Wifi();
                            List<Wifi> findWifi;
                            if (name.equals(currentWifiName)) {
                                wifi.setWifiName(name);
                                wifi.setWifiPassword(password);
                                wifi.setWifiType(type);
                                findWifi = LitePal.where("wifiname = ?", name).find(Wifi.class);
                                wifi.setRemark(findWifi.size() == 0 ? null : findWifi.get(0).getRemark());
                                wifiList.add(0, wifi);
                            } else {
                                wifi.setWifiName(name);
                                wifi.setWifiPassword(password);
                                wifi.setWifiType(type);
                                findWifi = LitePal.where("wifiname = ?", name).find(Wifi.class);
                                wifi.setRemark(findWifi.size() == 0 ? null : findWifi.get(0).getRemark());
                                wifiList.add(wifi);
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rotate(View view) {
        float toDegree = isExpand ? -90f : 45f;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", 0.0f, toDegree).setDuration(400);
        animator.start();
    }

    private void startQrCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_SCAN);
            return;
        }
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra("launch_method", "activity");
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    private void AnalyzeQRCode(Intent data) {
        if (data != null) {
            String content = data.getStringExtra("QRMessage");
            if (!content.contains("T:") || !content.contains("S:") || !content.contains("P:")) {

            } else {
                String name = "";
                int nameIndex = content.indexOf("S:", 5) + 2;
                for (int i = nameIndex; i < content.length(); i++) {
                    String current = content.substring(i, i + 1);
                    if (current.equals(";")) {
                        break;
                    }
                    name += current;
                }
                String password = "";
                int passwordIndex = content.indexOf("P:", 5) + 2;
                for (int i = passwordIndex; i < content.length(); i++) {
                    String current = content.substring(i, i + 1);
                    if (current.equals(";")) {
                        break;
                    }
                    password += current;
                }
                final String connectName = name;
                final String connectPassword = password;
                final AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(getString(R.string.scanning_title))
                        .setMessage(getString(R.string.scanning_name) + name + "\n" + getString(R.string.scanning_password) + password + "\n")
                        .setPositiveButton(getString(R.string.dialog_connect), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                wifiFlag = !wifiFlag;
                                connectWifi(connectName, connectPassword);
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                alertDialog.show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            AnalyzeQRCode(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doInBackground();
                } else {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(getString(R.string.denied_location_permission))
                            .setPositiveButton(getString(R.string.dialog_known), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(WifiActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                                }
                            })
                            .create().show();
                }
                break;
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startQrCode();
                } else {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(getString(R.string.denied_camera_permission))
                            .setPositiveButton(getString(R.string.dialog_known), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(WifiActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_SCAN);
                                }
                            })
                            .create().show();
                }
                break;
        }
    }

    private void connectWifi(String SSID, String preSharedKey) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        NetworkInfo wifiState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiManager.isWifiEnabled() || wifiState.isConnected()) {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", SSID);
            wifiConfig.preSharedKey = String.format("\"%s\"", preSharedKey);
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doInBackground();
                }
            }, 3000);
        } else {
            Toast.makeText(this, R.string.open_wifi, Toast.LENGTH_LONG).show();
        }
    }

    private void QRInputPop() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.wifi_input, null);
        spinner = (Spinner) promptsView.findViewById(R.id.spinner);
        wifiTypeList = new ArrayList<String>();
        wifiTypeList.add("WPA");
        wifiTypeList.add("WEP");
        spinnerAdapter = new ArrayAdapter<String>(this, R.layout.wifi_type_spinner, wifiTypeList);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentWifiType = wifiTypeList.get(position).equals("WPA") ? "WPA" : "WEP";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        final EditText selfWifiName = (EditText) promptsView.findViewById(R.id.self_wifi_name);
        final EditText selfWifiPassword = (EditText) promptsView.findViewById(R.id.self_wifi_password);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        })
                .setNegativeButton(getString(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(selfWifiName.getText().toString().trim())) {
                    selfWifiName.setError(getString(R.string.error_field_required));
                    View focusView = selfWifiName;
                    focusView.requestFocus();
                } else if (TextUtils.isEmpty(selfWifiPassword.getText().toString().trim())) {
                    selfWifiPassword.setError(getString(R.string.error_field_required));
                    View focusView = selfWifiPassword;
                    focusView.requestFocus();
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    QRCodePop(selfWifiName.getText().toString().trim(), selfWifiPassword.getText().toString().trim());
                    alertDialog.cancel();
                }
            }
        });
    }

    private void QRCodePop(String wifiName, String wifiPassword) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.self_wifi_qr_code, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        String contentText = "WIFI:T:" + currentWifiType + ";P:" + wifiPassword + ";S:" + wifiName + ";";
        final ImageView qrCode = (ImageView) promptsView.findViewById(R.id.self_wifi_qr_code);
        Bitmap bitmap = CodeCreator.createQRCode(contentText, 360, 360, null);
        if (bitmap != null) {
            qrCode.setImageBitmap(bitmap);
        }
        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton(getString(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void doInBackground() {
        isRefreshing = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                initWifiList();
                adapter = new WifiAdapter(isInSearch ? searchList : wifiList);
                adapter.setListener(WifiActivity.this);
                Message msg = new Message();
                msg.what = 1;
                doHandler.sendMessage(msg);
            }
        }).start();
    }

    private Handler doHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    recyclerView.setLayoutManager(new WrapContentGridLayoutManager(WifiActivity.this, 1));
                    recyclerView.setAdapter(adapter);
                    getSupportActionBar().setTitle(wifiList.size() == 0 ? WifiActivity.this.getString(R.string.list_title) :
                            WifiActivity.this.getString(R.string.list_title) + "(" + wifiList.size() + WifiActivity.this.getString(R.string.list_number));
                    swipeRefresh.setRefreshing(false);
                    isRefreshing = false;
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.toolbar_search);
        searchView = (SearchView) item.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setMaxWidth(1100);
        item.expandActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchList.clear();
                for (Wifi wifi : wifiList) {
                    String wifiRemark = LitePal.where("wifiname = ?", wifi.getWifiName()).find(Wifi.class).get(0).getRemark();
                    if (wifi.getWifiName().toLowerCase().contains(query.toLowerCase())
                            || (wifiRemark != null && (wifiRemark.toLowerCase().contains(query.toLowerCase())))) {
                        searchList.add(wifi);
                    }
                }
                map.clear();
                int index = 0;
                for (Wifi searchWifi : searchList) {
                    if (LitePal.where("wifiname = ?", searchWifi.getWifiName()).find(Wifi.class).get(0).getHighLight()) {
                        map.put(index, true);
                    }
                    ++index;
                }
                recyclerView = (RecyclerView) findViewById(R.id.wifi_recyclerView);
                adapter = new WifiAdapter(searchList);
                recyclerView.setAdapter(adapter);
                adapter.setListener(WifiActivity.this);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                noFindHint = (TextView) findViewById(R.id.noFindHint);
                isInSearch = true;
                noFindHint.setVisibility(View.GONE);
                if (newText == null || newText.length() == 0) {
                    isInSearch = false;
                    map.clear();
                    doInBackground();
                } else {
                    searchList.clear();
                    for (Wifi wifi : wifiList) {
                        String wifiRemark = wifi.getRemark();
                        if (wifi.getWifiName().toLowerCase().contains(newText.toLowerCase())
                                || (wifiRemark != null && (wifiRemark.toLowerCase().contains(newText.toLowerCase())))) {
                            searchList.add(wifi);
                        }
                    }
                    map.clear();
                    int index = 0;
                    for (Wifi searchWifi : searchList) {
                        if (LitePal.where("wifiname = ?", searchWifi.getWifiName()).find(Wifi.class).get(0).getHighLight()) {
                            map.put(index, true);
                        }
                        ++index;
                    }
                    recyclerView = (RecyclerView) findViewById(R.id.wifi_recyclerView);
                    adapter = new WifiAdapter(searchList);
                    recyclerView.setAdapter(adapter);
                    adapter.setListener(WifiActivity.this);
                    if (searchList.isEmpty()) {
                        noFindHint.setVisibility(View.VISIBLE);
                    }
                }
                return false;
            }
        });
        return true;
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
            return;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            point.x = (int) ev.getRawX();
            point.y = (int) ev.getRawY();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public Point getPoint() {
        return point;
    }

    @Override
    public Map<Integer, Boolean> getMap() {
        return map;
    }

    public class WrapContentGridLayoutManager extends GridLayoutManager {

        public WrapContentGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        public WrapContentGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        public WrapContentGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
            }
        }

        @Override
        public boolean supportsPredictiveItemAnimations() {
            return false;
        }

    }

    class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            switch (wifiState) {
                case WifiManager.WIFI_STATE_ENABLED:
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doInBackground();
                        }
                    }, 3000);
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                case WifiManager.WIFI_STATE_DISABLED:
                    isRefreshing = true;
                    initWifiList();
                    adapter = new WifiAdapter(isInSearch ? searchList : wifiList);
                    adapter.setListener(WifiActivity.this);
                    recyclerView.setLayoutManager(new WrapContentGridLayoutManager(WifiActivity.this, 1));
                    recyclerView.setAdapter(adapter);
                    getSupportActionBar().setTitle(wifiList.size() == 0 ? WifiActivity.this.getString(R.string.list_title) :
                            WifiActivity.this.getString(R.string.list_title) + "(" + wifiList.size() + WifiActivity.this.getString(R.string.list_number));
                    isRefreshing = false;
                    break;
                default:
                    break;
            }
        }
    }

}
