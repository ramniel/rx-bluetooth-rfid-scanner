package com.reactlibrary;

public class Constants {
    public static final String STATUS= "status";
    public static final String BLUETOOTH_UNKNOWN = "unknown";
    public static final String BLUETOOTH_ON = "on";
    public static final String BLUETOOTH_OFF = "off";
    public static final String BLUETOOTH_TURNING_ON = "turning_on";// Android only
    public static final String BLUETOOTH_TURNING_OFF = "turning_off";// Android only

    public static final String DEVICE_INFO= "device_info";
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS= "device_address";

    public static final String BLUETOOTH_EVENT = "bluetooth_event";
    public static final String BLUETOOTH_EVENT_TYPE= "type";
    public static final String DATA= "data";

    public static final int REQUEST_ACCESS_COARSE_LOCATION = 0; // Android need location permission.
    public static final int DISCOVERABLE_DURATION = 300; // Timeout of scanning
    public static final int DISCOVERY_REQUEST = 6;// Android only

    public static class TYPE{
        public static final String BLT_STATE_CHANGE = "BLT_STATE_CHANGE";
        public static final String BLT_DISCOVERY_DEVICE = "BLT_DISCOVERY_DEVICE";
        public static final String BLT_DISCOVERY_STARTED = "BLT_DISCOVERY_STARTED"; // called:
        public static final String BLT_DISCOVERY_DEVICE_FINISH = "BLT_DISCOVERY_DEVICE_FINISH"; // called: Time out/ DOTR not found any more device.
        public static final String SCAN_EVENT = "SCAN_EVENT";

    }

    public static class SCANNER_TYPE{
        public static final String ON_READ_TAG_DATA = "onReadTagData";
    }
}