package com.surya432.Zprinter;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.content.Context;
import android.os.Looper;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** ZprinterPlugin */
public class ZprinterPlugin  implements FlutterPlugin, MethodCallHandler  {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private static final boolean DEBUG = true;
  private Context context;

  public ZprinterPlugin(Registrar registrar) {
    this.context = registrar.activeContext();
  }
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "Zprinter");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (DEBUG) {
      System.out.print("onMethodCall: " + call.method + " ");
    }
    switch (call.method){
      case "discoverBluetoothDevices":
        discoverBluetoothDevices(result);
        break;
      case "getDeviceProperties":
        if (DEBUG) {
          System.out.println("with arguments: {mac:" + call.argument("mac") + "}");
        }
        getDeviceProperties((String) call.argument("mac"), result);
        break;
      case "getBatteryLevel":
        if (DEBUG) {
          System.out.println("with arguments: {mac:" + call.argument("mac") + "}");
        }
        getBatteryLevel((String) call.argument("mac"), result);
        break;
      case "isOnline":
        if (DEBUG) {
          System.out.println("with arguments: {mac:" + call.argument("mac") + "}");
        }
        isOnline((String) call.argument("mac"), result);
        break;
      case "sendZplOverBluetooth":
        if (DEBUG) {
          System.out.println("with arguments: {mac:" + call.argument("mac") + ", data: " + call.argument("data") + "}");
        }
        sendZplOverBluetooth((String) call.argument("mac"), (String) call.argument("data"), result);
        break;
      case "sendCpclOverBluetooth":
        if (DEBUG) {
          System.out.println("with arguments: {mac:" + call.argument("mac") + ", data: " + call.argument("data") + "}");
        }
        sendCpclOverBluetooth((String) call.argument("mac"), (String) call.argument("data"), result);
        break;
      default:
        result.notImplemented();
    }
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }
  private class DiscoveryRunner extends Thread implements DiscoveryHandler {

    Result result;
    String endMac;
    boolean resultSent = false;

    private HashMap<String, String> devices = new HashMap<>();

    public void setResult(Result result) {
      this.result = result;
    }

    public void setEndMac(String mac) {
      this.endMac = mac;
    }

    public void run() {

      //  Looper.prepare();
      try {
        if (DEBUG) {
          System.out.println("BluetoothDiscoverer.findPrinters");
        }
        BluetoothDiscoverer.findPrinters(ZprinterPlugin.this.context, this);
      } catch (ConnectionException e) {
        e.printStackTrace();
      } finally {
        //    Looper.myLooper().quit();
      }

    }

    @Override
    public void foundPrinter(DiscoveredPrinter discoveredPrinter) {
      if (DEBUG) {
        System.out.println("ZebraBlDiscoverer: Found Printer:" + discoveredPrinter.address + " || " + discoveredPrinter.toString());
        for (Map.Entry<String, String> me : discoveredPrinter.getDiscoveryDataMap().entrySet()) {
          System.out.println(me.getKey() + " => " + me.getValue());
        }
      }

      if (endMac != null) {
        if (DEBUG) {
          System.out.println("Exit when found is true");
        }
        if (discoveredPrinter.address.equalsIgnoreCase(endMac)) {
          if (DEBUG) {
            System.out.println("Searched mac found. Finishing discovery");
          }
          devices.clear();
          devices.put(discoveredPrinter.address, discoveredPrinter.getDiscoveryDataMap().get("FRIENDLY_NAME"));
          discoveryFinished();
        }
      }
      devices.put(discoveredPrinter.address, discoveredPrinter.getDiscoveryDataMap().get("FRIENDLY_NAME"));
    }

    @Override
    public void discoveryFinished() {
      if (resultSent) {
        if (DEBUG) {
          System.out.println("ZebraBlDiscoverer: discoveryFinished {resultSent}");
          return;
        }
      }
      if (DEBUG) {
        System.out.println("ZebraBlDiscoverer: discoveryFinished");
      }
      resultSent = true;
      result.success(devices);
      //  BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }

    @Override
    public void discoveryError(String s) {
      if (DEBUG) {
        System.out.println("ZebraBlDiscoverer: discoveryError:" + s);
      }
      if (resultSent) {
        if (DEBUG) {
          System.out.println("ZebraBlDiscoverer: discoveryError {resultSent}");
          return;
        }
      }
      resultSent = true;
      result.error(s, null, null);
      //  BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }
  }
  private void discoverBluetoothDevices(Result result) {
    DiscoveryRunner runner = new DiscoveryRunner();
    runner.setResult(result);
    runner.run();
  }
  private void isOnline(String mac, Result result) {
    DiscoveryRunner runner = new DiscoveryRunner();
    runner.setResult(result);
    runner.setEndMac(mac);
    runner.run();
  }


  private void getBatteryLevel(String mac, Result result) {
    try {
      Connection connection = new BluetoothConnection(mac);
      connection.open();

      ZebraPrinterLinkOs printer = ZebraPrinterFactory.getLinkOsPrinter(connection);

      result.success(printer.getSettingValue("power.percent_full"));

    } catch (Exception e) {
      result.error(e.getMessage(), null, null);
    }
  }

  private void getDeviceProperties(String mac, Result result) {

    try {
      HashMap<String, HashMap<String, String>> props = new HashMap<>();

      Connection connection = new BluetoothConnection(mac);
      connection.open();

      ZebraPrinterLinkOs printer = ZebraPrinterFactory.getLinkOsPrinter(connection);

      if (DEBUG) {
        System.out.println("getDeviceProperties: ");
      }
      Set<String> availableSettings = printer.getAvailableSettings();
      Map<String, String> allSettingValues = printer.getAllSettingValues();

      for (String setting : availableSettings) {
        HashMap<String, String> m = new HashMap<>();

        String s = allSettingValues.get(setting);

        m.put("value", s);
        m.put("set", printer.getSettingRange(setting));

        props.put(setting, m);

        if (DEBUG) {
          System.out.println("getDeviceProperties: " + setting + ": [" + printer.getSettingRange(setting) + "] => Value: " + s);
        }
      }
    } catch (Exception e) {
      result.error(e.getMessage(), null, null);
    }

  }
  private void sendCpclOverBluetooth(final String mac, final String data, final Result result) {

    new Thread(new Runnable() {
      public void run() {
        try {
          // Instantiate connection for given Bluetooth&reg; MAC Address.
          Connection connection = new BluetoothConnection(mac);
          // Initialize
          Looper.prepare();

          // Open the connection - physical connection is established here.
          connection.open();

          // Send the data to printer as a byte array.
          connection.write(data.getBytes());



          // Make sure the data got to the printer before closing the connection
          Thread.sleep(500);

          // Close the connection to release resources.
          connection.close();

          result.success("wrote " + data.getBytes().length + "bytes");

          Looper.myLooper().quit();
        } catch (Exception e) {
          result.error(e.getMessage(), null, null);
          // Handle communications error here.
          e.printStackTrace();
        }
      }
    }).start();
  }
  private void sendZplOverBluetooth(final String mac, final String data, final Result result) {

    new Thread(new Runnable() {
      public void run() {
        try {
          // Instantiate connection for given Bluetooth&reg; MAC Address.
          Connection connection = new BluetoothConnection(mac);
          // Initialize
          Looper.prepare();

          // Open the connection - physical connection is established here.
          connection.open();

          // Send the data to printer as a byte array.
          connection.write(data.getBytes());

          // Make sure the data got to the printer before closing the connection
          Thread.sleep(500);

          // Close the connection to release resources.
          connection.close();

          result.success("wrote " + data.getBytes().length + "bytes");

          Looper.myLooper().quit();
        } catch (Exception e) {
          result.error(e.getMessage(), null, null);
          // Handle communications error here.
          e.printStackTrace();
        }
      }
    }).start();
  }
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
