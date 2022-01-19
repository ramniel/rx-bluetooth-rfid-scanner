import { NativeEventEmitter, NativeModules } from 'react-native';

const { RxBluetoothRfidScanner } = NativeModules;

const eventEmitter = new NativeEventEmitter(RxBluetoothRfidScanner);

/**
 * TYPES
 */

type initializeReader = () => Promise<any>;
type deInitializeReader = () => Promise<any>;
type getCurrentState = () => Promise<any>;
type enableBluetooth = () => Promise<any>;
type disableBluetooth = () => Promise<any>;
type isBluetoothEnabled = () => Promise<any>;
type scanForDevices = () => Promise<any>;
type stopScanForDevices = () => Promise<any>;
type connectToDevice = (address: string) => Promise<any>;
type readSingleTag = () => Promise<any>;
type setSingleRead = (singleRead: boolean) => void;
type setFilters = (data: string, ptr: string, len: string) => Promise<any>;
type setEPCMode = (mode: string) => Promise<any>;
type setTIDMode = (mode: string) => Promise<any>;
type setUserMode = (mode: string) => Promise<any>;
type startInventoryScan = () => Promise<any>;
type stopInventoryScan = () => Promise<any>;
type AddListener = (cb: (args: any[]) => void) => void;
type clearData = () => Promise<any>;

/**
 *
 * METHODS
 */

const initializeReader: initializeReader = () => RxBluetoothRfidScanner.initializeReader();

const deInitializeReader: deInitializeReader = () => RxBluetoothRfidScanner.deInitializeReader();

const getCurrentState: getCurrentState = () => RxBluetoothRfidScanner.getCurrentState();

const enableBluetooth: enableBluetooth = () => RxBluetoothRfidScanner.enableBluetooth();

const disableBluetooth: disableBluetooth = () => RxBluetoothRfidScanner.disableBluetooth();

const isBluetoothEnabled: isBluetoothEnabled = () => RxBluetoothRfidScanner.isBluetoothEnabled();

const scanForDevices: scanForDevices = () => RxBluetoothRfidScanner.scanForDevices();

const stopScanForDevices: stopScanForDevices = () => RxBluetoothRfidScanner.stopScanForDevices();

const connectToDevice: connectToDevice = (address) => RxBluetoothRfidScanner.connectToDevice(address);

const readSingleTag: readSingleTag = () => RxBluetoothRfidScanner.readSingleTag();

const startInventoryScan: startInventoryScan = () => RxBluetoothRfidScanner.startInventoryScan();

const stopInventoryScan: stopInventoryScan = () => RxBluetoothRfidScanner.stopInventoryScan();

const setEPCMode: setEPCMode = (mode) => RxBluetoothRfidScanner.setEPCMode(mode);

const setTIDMode: setTIDMode = (mode) => RxBluetoothRfidScanner.setTIDMode(mode);

const setUserMode: setUserMode = (mode) => RxBluetoothRfidScanner.setUserMode(mode);

const setSingleRead: setSingleRead = (singleRead) => RxBluetoothRfidScanner.setSingleRead(singleRead);

const bluetoothListener: AddListener = (listener) => eventEmitter.addListener('BLE_EVENT', listener);

const setFilters: setFilters = (data: string, ptr: string, len: string) =>
  RxBluetoothRfidScanner.setFilters(data, ptr, len);

const clearData: clearData = () => RxBluetoothRfidScanner.clearData();

export default {
  bluetoothListener,
  initializeReader,
  deInitializeReader,
  getCurrentState,
  enableBluetooth,
  disableBluetooth,
  isBluetoothEnabled,
  scanForDevices,
  connectToDevice,
  stopScanForDevices,
  readSingleTag,
  setSingleRead,
  setFilters,
  setEPCMode,
  setTIDMode,
  setUserMode,
  startInventoryScan,
  stopInventoryScan,
  clearData,
};
