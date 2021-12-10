"use strict";
exports.__esModule = true;
var react_native_1 = require("react-native");
var RxBluetoothRfidScanner = react_native_1.NativeModules.RxBluetoothRfidScanner;
var eventEmitter = new react_native_1.NativeEventEmitter(RxBluetoothRfidScanner);
/**
 *
 * METHODS
 */
var initializeReader = function () { return RxBluetoothRfidScanner.initializeReader(); };
var deInitializeReader = function () { return RxBluetoothRfidScanner.deInitializeReader(); };
var getCurrentState = function () { return RxBluetoothRfidScanner.getCurrentState(); };
var enableBluetooth = function () { return RxBluetoothRfidScanner.enableBluetooth(); };
var disableBluetooth = function () { return RxBluetoothRfidScanner.disableBluetooth(); };
var isBluetoothEnabled = function () { return RxBluetoothRfidScanner.isBluetoothEnabled(); };
var scanForDevices = function () { return RxBluetoothRfidScanner.scanForDevices(); };
var stopScanForDevices = function () { return RxBluetoothRfidScanner.stopScanForDevices(); };
var connectToDevice = function (address) { return RxBluetoothRfidScanner.connectToDevice(address); };
var readSingleTag = function () { return RxBluetoothRfidScanner.readSingleTag(); };
var startInventoryScan = function () { return RxBluetoothRfidScanner.startInventoryScan(); };
var stopInventoryScan = function () { return RxBluetoothRfidScanner.stopInventoryScan(); };
var setEPCMode = function (mode) { return RxBluetoothRfidScanner.setEPCMode(mode); };
var setTIDMode = function (mode) { return RxBluetoothRfidScanner.setTIDMode(mode); };
var setUserMode = function (mode) { return RxBluetoothRfidScanner.setUserMode(mode); };
var bluetoothListener = function (listener) { return eventEmitter.addListener('BLE_EVENT', listener); };
var setFilters = function (data, ptr, len) {
    return RxBluetoothRfidScanner.setFilters(data, ptr, len);
};
var clearData = function () { return RxBluetoothRfidScanner.clearData(); };
exports["default"] = {
    bluetoothListener: bluetoothListener,
    initializeReader: initializeReader,
    deInitializeReader: deInitializeReader,
    getCurrentState: getCurrentState,
    enableBluetooth: enableBluetooth,
    disableBluetooth: disableBluetooth,
    isBluetoothEnabled: isBluetoothEnabled,
    scanForDevices: scanForDevices,
    connectToDevice: connectToDevice,
    stopScanForDevices: stopScanForDevices,
    readSingleTag: readSingleTag,
    setFilters: setFilters,
    setEPCMode: setEPCMode,
    setTIDMode: setTIDMode,
    setUserMode: setUserMode,
    startInventoryScan: startInventoryScan,
    stopInventoryScan: stopInventoryScan,
    clearData: clearData
};
