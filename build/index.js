import { NativeEventEmitter, NativeModules } from 'react-native';
var RxBluetoothRfidScanner = NativeModules.RxBluetoothRfidScanner;
var eventEmitter = new NativeEventEmitter(RxBluetoothRfidScanner);
var initializeReader = function () { return RxBluetoothRfidScanner.initializeReader(); };
var deInitializeReader = function () { return RxBluetoothRfidScanner.deInitializeReader(); };
var readSingleTag = function () { return RxBluetoothRfidScanner.readSingleTag(); };
var getCurrentState = function () { return RxBluetoothRfidScanner.getCurrentState(); };
var scanForDevices = function () { return RxBluetoothRfidScanner.scanForDevices(); };
var stopScanForDevices = function () { return RxBluetoothRfidScanner.stopScanForDevices(); };
var startReadingTags = function (callback) { return RxBluetoothRfidScanner.startReadingTags(callback); };
var stopReadingTags = function (callback) { return RxBluetoothRfidScanner.stopReadingTags(callback); };
var bluetoothListener = function (listener) { return eventEmitter.addListener('BLE_EVENT', listener); };
var powerListener = function (listener) { return eventEmitter.addListener('UHF_POWER', listener); };
var tagListener = function (listener) { return eventEmitter.addListener('UHF_TAG', listener); };
var clearTags = function () { return RxBluetoothRfidScanner.clearAllTags(); };
export default {
    powerListener: powerListener,
    tagListener: tagListener,
    bluetoothListener: bluetoothListener,
    initializeReader: initializeReader,
    readSingleTag: readSingleTag,
    getCurrentState: getCurrentState,
    scanForDevices: scanForDevices,
    stopScanForDevices: stopScanForDevices,
    startReadingTags: startReadingTags,
    stopReadingTags: stopReadingTags,
    deInitializeReader: deInitializeReader,
    clearTags: clearTags,
};
//# sourceMappingURL=index.js.map