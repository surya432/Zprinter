
import 'dart:async';
import 'dart:io' show Platform;

import 'package:flutter/services.dart';
class ZebraBluetoothDevice {
  final String mac;
  String friendlyName = "Unknown";

  ZebraBluetoothDevice(this.mac, this.friendlyName);

  Future<Map<String, Map<String, String>>> properties() => Zprinter._getDeviceProperties(mac);

  Future<String> batteryLevel() async {
    return await Zprinter._getBatteryLevel(mac);
  }

  Future<void> sendZplOverBluetooth(String data) => Zprinter._sendZplOverBluetooth(mac, data);
  Future<void> sendCpclOverBluetooth(String data) => Zprinter._sendCpclOverBluetooth(mac, data);

  Future<bool> isOnline() => Zprinter._isOnline(mac);
}
class Zprinter {
  static const MethodChannel _channel =
      const MethodChannel('Zprinter');
  static Future<List<ZebraBluetoothDevice>> discoverBluetoothDevices() async {
    dynamic d = await _channel.invokeMethod("discoverBluetoothDevices");

    List<ZebraBluetoothDevice> devices = List();

    d.forEach((k, v) {
      devices.add(ZebraBluetoothDevice(k, v));
    });

    return devices;
  }
  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<Map<String, Map<String, String>>> _getDeviceProperties(String mac) async {
    dynamic d = await _channel.invokeMethod("getDeviceProperties", {"mac": mac});
    Map<String, Map<String, String>> map = Map();

    d.forEach((k, v) {
      map[k] = Map<String, String>.from(v);
    });
    return map;
  }

  static Future<String> _getBatteryLevel(String mac) async {
    dynamic d = await _channel.invokeMethod("getBatteryLevel", {"mac": mac});
    return d.toString().replaceAll("% Full", "");
  }

  static Future<void> _sendZplOverBluetooth(String mac, String data) async {
    await _channel.invokeMethod("sendZplOverBluetooth", {"mac": mac, "data": data});
  }
  static Future<void> _sendCpclOverBluetooth(String mac, String data) async {
    await _channel.invokeMethod("sendCpclOverBluetooth", {"mac": mac, "data": data});
  }

  static Future<bool> _isOnline(String mac) async {
    dynamic d;
    if (Platform.isAndroid) {
      d = await _channel.invokeMethod("isOnline", {"mac": mac});
    } else {
      d = await _channel.invokeMethod("discoverBluetoothDevices"); // paired before
    }
    print(d);

    return d.length > 0;
  }
}
