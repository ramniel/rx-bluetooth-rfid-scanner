package com.reactlibrary;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.rscja.deviceapi.RFIDWithUHFBLE;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.ConnectionStatus;
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback;
import com.rscja.deviceapi.interfaces.KeyEventCallback;
import com.rscja.deviceapi.interfaces.ScanBTCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RxBluetoothRfidScannerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private final BluetoothAdapter bluetoothAdapter;
    private RFIDWithUHFBLE uhf;
    private final ExecutorService executorService;

    final int FLAG_START = 0;
    final int FLAG_STOP = 1;
    final int FLAG_TAG = 2;
    final int FLAG_TAG_LIST = 3;
    final int FLAG_UPDATE_TIME = 4;
    final int FLAG_SET_SUCCESS = 5;
    final int FLAG_SET_FAIL = 6;
    final int FLAG_GET_MODE = 7;
    final int FLAG_SUCCESS = 8;
    final int FLAG_FAIL = 9;

    private String selectedMode;

    private String ptrString;
    private String lenString;

    private HashMap<String, String> map;

    private ArrayList<HashMap<String, String>> tagList = new ArrayList<>();
    private final List<String> tempTags = new ArrayList<>();

    public boolean isScanning = false;
    public String remoteBTName = "";
    public String remoteBTAdd = "";

    public static final String TAG_DATA = "tagData";
    public static final String TAG_EPC = "tagEpc";
    public static final String TAG_TID = "tagTid";
    public static final String TAG_LEN = "tagLen";
    public static final String TAG_COUNT = "tagCount";
    public static final String TAG_RSSI = "tagRssi";

    private boolean singleRead = false;
    private boolean loopFlag = false;
    private boolean isExit = false;
    boolean isRunning = false;


    public RxBluetoothRfidScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addLifecycleEventListener(this);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.uhf = RFIDWithUHFBLE.getInstance();
        this.executorService = Executors.newFixedThreadPool(3);
    }

    @Override
    public String getName() {
        return "RxBluetoothRfidScanner";
    }

    @Override
    public void onHostResume() {
        if(uhf.getConnectStatus() == ConnectionStatus.CONNECTED) {
            getMode();
        }
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        uhf.free();
        Utils.freeSound();
    }

    @ReactMethod
    private void initializeReader(Promise promise) {
        Log.d("UHF Reader", "Initializing Reader");
        if (uhf == null) {
            uhf = RFIDWithUHFBLE.getInstance();
        }

        uhf.init(reactContext);
        Utils.initSound(reactContext);

        uhf.setKeyEventCallback(new KeyEventCallback() {
            @Override
            public void onKeyDown(int keycode) {
                Log.d("TAG", "  keycode =" + keycode + "   ,isExit=" + isExit);
                if (!isExit && uhf.getConnectStatus() == ConnectionStatus.CONNECTED) {
                    if(singleRead) {
                        getTag();
                    } else if(loopFlag) {
                        stopInventory();
                    } else {
                        startThread();
                    }
                }
                WritableMap map = Arguments.createMap();
                map.putString("keycode", String.valueOf(keycode));
                sendEvent("BLE_EVENT", Utils.convertParams("TRIGGER_PULLED", map));
            }
        });

        checkLocationEnable();

        promise.resolve(true);
    }

    @ReactMethod
    public void deInitializeReader(Promise promise) {
        isExit = true;
        uhf.free();
        Utils.freeSound();

        promise.resolve(true);
    }

    @ReactMethod
    public void getCurrentState(Promise promise) {
        try {
            String state = Constants.BLUETOOTH_UNKNOWN;
            if (bluetoothAdapter != null) {
                switch (bluetoothAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        state = Constants.BLUETOOTH_ON;
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        state = Constants.BLUETOOTH_OFF;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }

            WritableMap map = Arguments.createMap();
            map.putString(Constants.STATUS, state);

            promise.resolve(Utils.convertParams(Constants.TYPE.BLT_STATE_CHANGE, map));

        } catch (Exception e) {
            Log.d("ERROR", e.getLocalizedMessage());
            promise.reject("NO_BLUETOOTH", e.getLocalizedMessage());
        }
    }

    @ReactMethod
    public void enableBluetooth(final Promise promise) {
        //enable bluetooth
        if (uhf.getConnectStatus() == ConnectionStatus.CONNECTING) {
            Log.d("RXBLE", "Bluetooth device is connecting");
        } else {

            if (bluetoothAdapter == null) {
                Log.d("RXBLE", "Device does not support bluetooth");
            }

            if (!bluetoothAdapter.isEnabled()) {
                Log.d("RXBLE", "Bluetooth device is disabled");
                this.reactContext.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1, null);
            }
        }

        promise.resolve(true);
    }

    @ReactMethod
    public void disableBluetooth(final Promise promise) {
        //enable bluetooth
        if (bluetoothAdapter != null) {
            bluetoothAdapter.disable();
        }
        promise.resolve(true);
    }

    @ReactMethod
    public void isBluetoothEnabled(final Promise promise) {                                                     //check if the bluetooth is enabled or not

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            promise.resolve(false);
        } else {
            promise.resolve(true);
        }
    }


    boolean scanningBLE;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void scanForDevices(final Promise promise) throws InterruptedException {
        int permissionChecked;
        permissionChecked = ActivityCompat.checkSelfPermission(reactContext, ACCESS_COARSE_LOCATION);

        if (permissionChecked == PackageManager.PERMISSION_DENIED) {

            requestPermissions(getCurrentActivity(),
                    new String[]{ACCESS_COARSE_LOCATION}, 1);
        }


        Handler handler = new Handler(reactContext.getMainLooper());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                scanningBLE = false;
                uhf.stopScanBTDevices();
            }
        }, 10000);

        Thread.sleep(10000);

        scanningBLE = true;
        uhf.startScanBTDevices(new ScanBTCallback() {
            @Override
            public void getDevices(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                WritableMap map = Arguments.createMap();
                map.putString("address", bluetoothDevice.getAddress());
                map.putString("name", bluetoothDevice.getName());
                sendEvent("BLE_EVENT", Utils.convertParams("DEVICE", map));
                //promise.resolve(Utils.convertParams("DEVICE", map));
            }
        });

        promise.resolve(true);
    }

    @ReactMethod
    public void connectToDevice(String address, Promise promise) {
        if (uhf == null) {
            uhf = RFIDWithUHFBLE.getInstance();
        }
        uhf.connect(address, new ConnectionStatusCallback<Object>() {
            @Override
            public void getStatus(final ConnectionStatus connectionStatus, final Object device1) {
                new Runnable() {
                    @Override
                    public void run() {
                        BluetoothDevice device = (BluetoothDevice) device1;
                        if (connectionStatus == ConnectionStatus.CONNECTED) {
                            WritableMap map = Arguments.createMap();
                            map.putString("address", device.getAddress());
                            map.putString("name", device.getName());
                            map.putString("status", "connected");

                            sendEvent("BLE_EVENT", Utils.convertParams("DEVICE", map));

                        } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                            WritableMap map = Arguments.createMap();
                            map.putString("address", device.getAddress());
                            map.putString("name", device.getName());
                            map.putString("status", "disconnected");

                            sendEvent("BLE_EVENT", Utils.convertParams("DEVICE", map));
                        }

                    }
                };
            }
        });
        promise.resolve(true);
    }

    @ReactMethod
    public void stopScanForDevices(Promise promise) {
        scanningBLE = false;
        uhf.stopScanBTDevices();
        promise.resolve(true);
    }

    @ReactMethod
    public void readSingleTag(final Promise promise) {
        try {
            UHFTAGInfo tag = uhf.inventorySingleTag();

            if (!tag.getEPC().isEmpty()) {
                WritableMap map = Arguments.createMap();
                map.putString("epc", tag.getEPC());
                map.putString("rssi", tag.getRssi());
                map.putString("tid", tag.getTid());
                map.putString("user", tag.getUser());

                sendEvent("BLE_EVENT", Utils.convertParams("TAG", map));

                promise.resolve(true);
            } else {
                promise.reject("ERROR", "READ FAILED");
            }
        } catch (Exception ex) {
            promise.reject("ERROR", ex);
        }
    }

    @ReactMethod
    public void setSingleRead(boolean singleRead) {
        this.singleRead = singleRead;
    }

    @ReactMethod
    public void setFilters(String dataStr, String ptrStr, String lenStr, Promise promise) {

        int filterBank = RFIDWithUHFBLE.Bank_EPC;
        if (selectedMode.equals("EPC")) {
            filterBank = RFIDWithUHFBLE.Bank_EPC;
        } else if (selectedMode.equals("EPC_TID")) {
            filterBank = RFIDWithUHFBLE.Bank_TID;
        } else if (selectedMode.equals("EPC_TID_USER")) {
            filterBank = RFIDWithUHFBLE.Bank_USER;
        }
        if (lenStr == null || lenStr.isEmpty()) {
            Toast.makeText(reactContext, "Data length can not be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ptrStr == null || ptrStr.isEmpty()) {
            Toast.makeText(reactContext, "The start address cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        int ptr = Utils.toInt(ptrStr, 0);
        int len = Utils.toInt(lenStr, 0);
        String data = dataStr.trim();
        if (len > 0) {
            String rex = "[\\da-fA-F]*";
            if (data.isEmpty() || !data.matches(rex)) {
                Toast.makeText(reactContext, "The filtered data must be hexadecimal data", Toast.LENGTH_SHORT).show();

                promise.reject("ERROR", "filtered data must be hexadecimal");

                return;
            }

            int l = data.replace(" ", "").length();
            if (len <= l * 4) {
                if (l % 2 != 0)
                    data += "0";
            } else {
                Toast.makeText(reactContext, "Failed to set filter data", Toast.LENGTH_SHORT).show();
                promise.reject("ERROR", "Failed to set filter data");
                return;
            }

            if (uhf.setFilter(filterBank, ptr, len, data)) {
                Toast.makeText(reactContext, "Filter data set", Toast.LENGTH_SHORT).show();

                promise.resolve("Filter data set");
            } else {
                Toast.makeText(reactContext, "Failed to set filter data", Toast.LENGTH_SHORT).show();
                promise.reject("ERROR", "Failed to set filter data");
            }
        } else {
            //禁用过滤
            String dataSt = "00";
            if (uhf.setFilter(RFIDWithUHFBLE.Bank_EPC, 0, 0, dataSt)
                    && uhf.setFilter(RFIDWithUHFBLE.Bank_TID, 0, 0, dataSt)
                    && uhf.setFilter(RFIDWithUHFBLE.Bank_USER, 0, 0, dataSt)) {
                Toast.makeText(reactContext, "Filters disabled", Toast.LENGTH_SHORT).show();

                promise.resolve("Filters disabled");
            } else {
                Toast.makeText(reactContext, "Failed to disable filters", Toast.LENGTH_SHORT).show();
                promise.reject("ERROR", "Failed to disable filters");
            }
        }
    }



    @ReactMethod
    public void setEPCMode(Promise promise) {
        ptrString = "32";
        executorService.execute(epcModeRunnable);
        promise.resolve(true);
    }

    @ReactMethod
    public void setTIDMode(Promise promise) {
        ptrString = "0";
        executorService.execute(epcTidModeRunnable);
        promise.resolve(true);
    }

    @ReactMethod
    public void setUserMode(Promise promise) {
        ptrString = "0";
        executorService.execute(epcTidUserModeRunnable);
        promise.resolve(true);
    }

    @ReactMethod
    public void startInventoryScan(Promise promise) {
        startThread();
        promise.resolve(true);
    }

    @ReactMethod
    public void stopInventoryScan(Promise promise){
        if (uhf.getConnectStatus() == ConnectionStatus.CONNECTED) {
            stopInventory();
            promise.resolve(true);
        } else {
            promise.reject("ERROR", "Scanner disconnected");
        }
    }

    public synchronized void startThread() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        new TagThread().start();
    }

    private void stopInventory() {
        loopFlag = false;
        ConnectionStatus connectionStatus = uhf.getConnectStatus();
        Message msg = handler.obtainMessage(FLAG_STOP);
        boolean result = uhf.stopInventory();
        if (result || connectionStatus == ConnectionStatus.DISCONNECTED) {
            msg.arg1 = FLAG_SUCCESS;
        } else {
            msg.arg1 = FLAG_FAIL;
        }
        isScanning = false;
        handler.sendMessage(msg);
    }

    long total = 0;

    @ReactMethod
    public void clearData(Promise promise) {
        total = 0;
        tagList.clear();
        tempTags.clear();
        promise.resolve("true");
    }

    private void getMode() {
        executorService.execute(getModeRunnable);
    }

    private void setMode(Mode mode) {
        switch (mode) {
            case EPC:
                if (uhf.setEPCMode()) {

                    handler.sendEmptyMessage(FLAG_SET_SUCCESS);
                } else {
                    handler.sendEmptyMessage(FLAG_SET_FAIL);
                }
                break;
            case EPC_TID:
                if (uhf.setEPCAndTIDMode()) {
                    handler.sendEmptyMessage(FLAG_SET_SUCCESS);
                } else {
                    handler.sendEmptyMessage(FLAG_SET_FAIL);
                }
                break;
            case EPC_TID_USER:
                int userPtr = 0;
                int userLen = 6;
                if (!TextUtils.isEmpty(ptrString)) {
                    userPtr = Integer.parseInt(ptrString);
                }
                if (!TextUtils.isEmpty(lenString)) {
                    userLen = Integer.parseInt(lenString);
                }
                if (uhf.setEPCAndTIDUserMode(userPtr, userLen)) {
                    handler.sendEmptyMessage(FLAG_SET_SUCCESS);
                } else {
                    handler.sendEmptyMessage(FLAG_SET_FAIL);
                }
                break;
        }
    }

    private final Runnable getModeRunnable = new Runnable() {

        @Override
        public void run() {
            if (uhf.getConnectStatus() == ConnectionStatus.CONNECTED) {
                byte[] data = uhf.getEPCAndTIDUserMode();
                Message msg = handler.obtainMessage(FLAG_GET_MODE, data);
                handler.sendMessage(msg);
            }
        }
    };


    private final Runnable epcModeRunnable = new Runnable() {
        @Override
        public void run() {
            setMode(Mode.EPC);
        }
    };

    private final Runnable epcTidModeRunnable = new Runnable() {
        @Override
        public void run() {
            setMode(Mode.EPC_TID);
        }
    };

    private final Runnable epcTidUserModeRunnable = new Runnable() {
        @Override
        public void run() {
            setMode(Mode.EPC_TID_USER);
        }
    };

    public enum  Mode {
        EPC, EPC_TID, EPC_TID_USER
    }

    @SuppressWarnings("HandlerLeak")
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FLAG_START:
                    if(msg.arg1 == FLAG_SUCCESS) {

                    } else {
                        Utils.playSound(2);
                        Toast.makeText(reactContext, "Failed to start", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case FLAG_STOP:
                    if(msg.arg1 == FLAG_SUCCESS) {

                    } else {
                        Utils.playSound(2);
                        Toast.makeText(reactContext, "Failed to stop", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case FLAG_TAG_LIST:
                    List<UHFTAGInfo> list = (List<UHFTAGInfo>) msg.obj;
                    addEPCToList(list);
                    break;
                case FLAG_TAG:
                    UHFTAGInfo info = (UHFTAGInfo) msg.obj;
                    addEPCToList(info);
                    Utils.playSound(1);
                    break;
                case FLAG_UPDATE_TIME:
                    break;
                case FLAG_SET_SUCCESS:
                    Toast.makeText(reactContext, "SUCCESS", Toast.LENGTH_SHORT).show();
                    break;
                case FLAG_SET_FAIL:
                    Toast.makeText(reactContext, "FAILED", Toast.LENGTH_SHORT).show();
                    break;
                case FLAG_GET_MODE:
                    byte[] data = (byte[]) msg.obj;
                    if (data != null) {
                        if (data[0] == 0) {
                            selectedMode = "EPC";
                        } else if (data[0] == 1) {
                            selectedMode = "EPC_TID";
                        } else if (data.length >= 3 && data[0] == 2) {
                            selectedMode = "EPC_TID_USER";
                            ptrString = String.valueOf(data[1]);
                            ptrString = String.valueOf(data[2]);
                        } else {
                            selectedMode = null;
                        }

                        Toast.makeText(reactContext, "Mode set", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(reactContext, "Failed to set mode", Toast.LENGTH_SHORT).show();
                        selectedMode = null;
                    }
                    break;
            }
        }
    };

    private void sendEvent(String eventName, @Nullable WritableMap eventData) {
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, eventData);
    }

    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 100;
    private static final int REQUEST_ACTION_LOCATION_SETTINGS = 3;
    private void checkLocationEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (reactContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(reactContext.getCurrentActivity(),
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION_REQUEST);
            }
        }
        if (!isLocationEnabled()) {
            Utils.alert(reactContext.getCurrentActivity(),
                    "Get location permission",
                    "Go to the settings interface to open the location permission?",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            reactContext.startActivityForResult(intent, REQUEST_ACTION_LOCATION_SETTINGS, null);
                        }
                    });
        }
    }

    private boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(reactContext.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(reactContext.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    class TagThread extends Thread {

        public void run() {
            Message msg = handler.obtainMessage(FLAG_START);
            if (uhf.startInventoryTag()) {
                loopFlag = true;
                isScanning = true;
                //mStrTime = System.currentTimeMillis();
                msg.arg1 = FLAG_SUCCESS;
            } else {
                msg.arg1 = FLAG_FAIL;
            }
            handler.sendMessage(msg);
            isRunning = false;//执行完成设置成false
            long startTime=System.currentTimeMillis();
            while (loopFlag) {
                List<UHFTAGInfo> list = getUHFInfo();
                if(list==null || list.size()==0){
                    SystemClock.sleep(1);
                }else{
                    Utils.playSound(1);
                    handler.sendMessage(handler.obtainMessage(FLAG_TAG_LIST, list));
                }
                if(System.currentTimeMillis()-startTime>100){
                    startTime=System.currentTimeMillis();
                    handler.sendEmptyMessage(FLAG_UPDATE_TIME);
                }

            }
            stopInventory();
        }
    }

    private synchronized List<UHFTAGInfo> getUHFInfo() {
        return uhf.readTagFromBufferList_EpcTidUser();
    }

    private void addEPCToList(UHFTAGInfo uhftagInfo) {
        if (!TextUtils.isEmpty(uhftagInfo.getEPC())) {
            int index = checkIsExist(uhftagInfo.getEPC());

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EPC:");
            stringBuilder.append(uhftagInfo.getEPC());
            if (!TextUtils.isEmpty(uhftagInfo.getTid())) {
                stringBuilder.append("\r\nTID:");
                stringBuilder.append(uhftagInfo.getTid());
            }
            if (!TextUtils.isEmpty(uhftagInfo.getUser())) {
                stringBuilder.append("\r\nUSER:");
                stringBuilder.append(uhftagInfo.getUser());
            }

            map = new HashMap<String, String>();
            map.put(TAG_EPC, uhftagInfo.getEPC());
            map.put(TAG_DATA, stringBuilder.toString());
            map.put(TAG_COUNT, String.valueOf(1));
            map.put(TAG_RSSI, uhftagInfo.getRssi() == null ? "" : uhftagInfo.getRssi());

            WritableMap remap = Arguments.createMap();
            remap.putString(TAG_EPC, uhftagInfo.getEPC());
            remap.putString(TAG_DATA, stringBuilder.toString());
            remap.putString(TAG_COUNT, String.valueOf(1));
            remap.putString(TAG_RSSI, uhftagInfo.getRssi() == null ? "" : uhftagInfo.getRssi());

            // mContext.getAppContext().uhfQueue.offer(epc + "\t 1");
            if (index == -1) {
                tagList.add(map);
                tempTags.add(uhftagInfo.getEPC());
            } else {
                int tagCount = Integer.parseInt(tagList.get(index).get(TAG_COUNT), 10) + 1;
                map.put(TAG_COUNT, String.valueOf(tagCount));
                tagList.set(index, map);
            }
            sendEvent("BLE_EVENT", Utils.convertParams("UHF_TAG", remap));
            ++total;
        }
    }
    private void addEPCToList(List<UHFTAGInfo> list) {
        for(int k=0;k<list.size();k++){
            UHFTAGInfo uhftagInfo=list.get(k);
            if (!TextUtils.isEmpty(uhftagInfo.getEPC())) {
                int index = checkIsExist(uhftagInfo.getEPC());

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EPC:");
                stringBuilder.append(uhftagInfo.getEPC());
                if (!TextUtils.isEmpty(uhftagInfo.getTid())) {
                    stringBuilder.append("\r\nTID:");
                    stringBuilder.append(uhftagInfo.getTid());
                }
                if (!TextUtils.isEmpty(uhftagInfo.getUser())) {
                    stringBuilder.append("\r\nUSER:");
                    stringBuilder.append(uhftagInfo.getUser());
                }

                map = new HashMap<String, String>();
                map.put(TAG_EPC, uhftagInfo.getEPC());
                map.put(TAG_DATA, stringBuilder.toString());
                map.put(TAG_COUNT, String.valueOf(1));
                map.put(TAG_RSSI, uhftagInfo.getRssi() == null ? "" : uhftagInfo.getRssi());

                WritableMap remap = Arguments.createMap();
                remap.putString(TAG_EPC, uhftagInfo.getEPC());
                remap.putString(TAG_DATA, stringBuilder.toString());
                remap.putString(TAG_COUNT, String.valueOf(1));
                remap.putString(TAG_RSSI, uhftagInfo.getRssi() == null ? "" : uhftagInfo.getRssi());

                // mContext.getAppContext().uhfQueue.offer(epc + "\t 1");
                if (index == -1) {
                    tagList.add(map);
                    tempTags.add(uhftagInfo.getEPC());
                    sendEvent("BLE_EVENT", Utils.convertParams("UHF_TAG", remap));
                } else {
                    int tagCount = Integer.parseInt(tagList.get(index).get(TAG_COUNT), 10) + 1;
                    map.put(TAG_COUNT, String.valueOf(tagCount));
                    tagList.set(index, map);
                }
                ++total;
            }
        }
    }

    private void getTag() {
        UHFTAGInfo tag = uhf.inventorySingleTag();
        handler.sendMessage(handler.obtainMessage(FLAG_TAG, tag));
    }

    public int checkIsExist(String epc) {
        if (TextUtils.isEmpty(epc)) {
            return -1;
        }
        return binarySearch(tempTags, epc);
    }

    static int binarySearch(List<String> array, String src) {
        int left = 0;
        int right = array.size() - 1;
        // 这里必须是 <=
        while (left <= right) {
            if (compareString(array.get(left), src)) {
                return left;
            } else if (left != right) {
                if (compareString(array.get(right), src))
                    return right;
            }
            left++;
            right--;
        }
        return -1;
    }

    static boolean compareString(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        } else if (str1.hashCode() != str2.hashCode()) {
            return false;
        } else {
            char[] value1 = str1.toCharArray();
            char[] value2 = str2.toCharArray();
            int size = value1.length;
            for (int k = 0; k < size; k++) {
                if (value1[k] != value2[k]) {
                    return false;
                }
            }
            return true;
        }
    }
}
