package com.mackentoch.beaconsandroid;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class BeaconsAndroidModule extends ReactContextBaseJavaModule implements BeaconConsumer {
	private static final String LOG_TAG = "BeaconsAndroidModule";
	private static final String NOTIFICATION_CHANNEL_ID = "BeaconsAndroidModule";
	private static final int RUNNING_AVG_RSSI_FILTER = 0;
	private static final int ARMA_RSSI_FILTER = 1;
	private BeaconManager mBeaconManager;
	private Context mApplicationContext;
	private ReactApplicationContext mReactContext;
	private static boolean channelCreated = false;
	private String debugApi = null;
	private String requestToken = null;
	private String beaconRequestApi = null;
  private Region MyRegion = null;

  Set<String> mBeaconsProcessed = new HashSet<>();

	public BeaconsAndroidModule(ReactApplicationContext reactContext) {
		super(reactContext);
		Log.d(LOG_TAG, "BeaconsAndroidModule - started");
		this.mReactContext = reactContext;
		this.mApplicationContext = reactContext.getApplicationContext();
		this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);

		mBeaconManager.setDebug(false);

		// Fix beacon empty when screen off
		ScanFilter.Builder builder = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			builder = new ScanFilter.Builder();
			builder.setManufacturerData(0x004c, new byte[] {});
			ScanFilter filter = builder.build();
		}
	}

	@Override
	public String getName() {
		return LOG_TAG;
	}

	@Override
	public Map < String,
	Object > getConstants() {
		final Map < String,
		Object > constants = new HashMap < >();
		constants.put("SUPPORTED", BeaconTransmitter.SUPPORTED);
		constants.put("NOT_SUPPORTED_MIN_SDK", BeaconTransmitter.NOT_SUPPORTED_MIN_SDK);
		constants.put("NOT_SUPPORTED_BLE", BeaconTransmitter.NOT_SUPPORTED_BLE);
		constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS);
		constants.put("NOT_SUPPORTED_CANNOT_GET_ADVERTISER", BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER);
		constants.put("RUNNING_AVG_RSSI_FILTER", RUNNING_AVG_RSSI_FILTER);
		constants.put("ARMA_RSSI_FILTER", ARMA_RSSI_FILTER);
		return constants;
	}

	@ReactMethod
	public void setHardwareEqualityEnforced(Boolean e) {
		Beacon.setHardwareEqualityEnforced(e.booleanValue());
	}

	public void bindManager() {
		if (!mBeaconManager.isBound(this)) {
			Log.d(LOG_TAG, "bindManager: ");
			mBeaconManager.bind(this);
		}
	}

	public void unbindManager() {
		if (mBeaconManager.isBound(this)) {
			Log.d(LOG_TAG, "unbindManager: ");
			mBeaconManager.unbind(this);
		}
	}

	@ReactMethod
	public void addParser(String parser, Callback resolve, Callback reject) {
		try {
			Log.d(LOG_TAG, "addParser: " + parser);
			mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(parser));
			resolve.invoke();
		} catch(Exception e) {
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void removeParser(String parser, Callback resolve, Callback reject) {
		try {
			Log.d(LOG_TAG, "removeParser: " + parser);
			unbindManager();
			mBeaconManager.getBeaconParsers().remove(new BeaconParser().setBeaconLayout(parser));
			bindManager();
			resolve.invoke();
		} catch(Exception e) {
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void addParsersListToDetection(ReadableArray parsers, Callback resolve, Callback reject) {
		try {
			unbindManager();
			for (int i = 0; i < parsers.size(); i++) {
				String parser = parsers.getString(i);
				Log.d(LOG_TAG, "addParsersListToDetection - add parser: " + parser);
				mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(parser));
			}
			bindManager();
			resolve.invoke(parsers);
		} catch(Exception e) {
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void removeParsersListToDetection(ReadableArray parsers, Callback resolve, Callback reject) {
		try {
			unbindManager();
			for (int i = 0; i < parsers.size(); i++) {
				String parser = parsers.getString(i);
				Log.d(LOG_TAG, "removeParsersListToDetection - remove parser: " + parser);
				mBeaconManager.getBeaconParsers().remove(new BeaconParser().setBeaconLayout(parser));
			}
			bindManager();
			resolve.invoke(parsers);
		} catch(Exception e) {
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void setBackgroundScanPeriod(int period) {
		mBeaconManager.setBackgroundScanPeriod((long) period);
	}

	@ReactMethod
	public void setBackgroundBetweenScanPeriod(int period) {
		mBeaconManager.setBackgroundBetweenScanPeriod((long) period);
	}

	@ReactMethod
	public void setForegroundScanPeriod(int period) {
		mBeaconManager.setForegroundScanPeriod((long) period);
	}

	@ReactMethod
	public void setForegroundBetweenScanPeriod(int period) {
		mBeaconManager.setForegroundBetweenScanPeriod((long) period);
	}

	@ReactMethod
	public void setRssiFilter(int filterType, double avgModifier) {
		String logMsg = "Could not set the rssi filter.";
		if (filterType == RUNNING_AVG_RSSI_FILTER) {
			logMsg = "Setting filter RUNNING_AVG";
			BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
			if (avgModifier > 0) {
				RunningAverageRssiFilter.setSampleExpirationMilliseconds((long) avgModifier);
				logMsg += " with custom avg modifier";
			}
		} else if (filterType == ARMA_RSSI_FILTER) {
			logMsg = "Setting filter ARMA";
			BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
			if (avgModifier > 0) {
				ArmaRssiFilter.setDEFAULT_ARMA_SPEED(avgModifier);
				logMsg += " with custom avg modifier";
			}
		}
		Log.d(LOG_TAG, logMsg);
	}

	@ReactMethod
	public void checkTransmissionSupported(Callback callback) {
		int result = BeaconTransmitter.checkTransmissionSupported(mReactContext);
		callback.invoke(result);
	}

	@ReactMethod
	public void getMonitoredRegions(Callback callback) {
		WritableArray array = new WritableNativeArray();
		for (Region region: mBeaconManager.getMonitoredRegions()) {
			WritableMap map = new WritableNativeMap();
			map.putString("identifier", region.getUniqueId());
			map.putString("uuid", region.getId1() != null ? region.getId1().toString() : "");
			map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
			map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
			array.pushMap(map);
		}
		callback.invoke(array);
	}

	@ReactMethod
	public void getRangedRegions(Callback callback) {
		WritableArray array = new WritableNativeArray();
		for (Region region: mBeaconManager.getRangedRegions()) {
			WritableMap map = new WritableNativeMap();
			map.putString("region", region.getUniqueId());
			map.putString("uuid", region.getId1() != null ? region.getId1().toString() : "");
			array.pushMap(map);
		}
		callback.invoke(array);
	}

	/***********************************************************************************************
     * BeaconConsumer
     **********************************************************************************************/
	@Override
	public void onBeaconServiceConnect() {
		Log.d(LOG_TAG, "beaconServiceConnected");
    mBeaconManager.removeAllRangeNotifiers();
		// mBeaconManager.addMonitorNotifier(mMonitorNotifier);
		mBeaconManager.addRangeNotifier(mRangeNotifier);
    sendEvent(mReactContext, "beaconServiceConnected", null);
	}

	@Override
	public Context getApplicationContext() {
		return mApplicationContext;
	}

	@Override
	public void unbindService(ServiceConnection serviceConnection) {
    Log.e(LOG_TAG, "Unbinding service: " + serviceConnection);
		mApplicationContext.unbindService(serviceConnection);
	}

	@Override
	public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
    Log.e(LOG_TAG, "Binding service: " + serviceConnection);
		return mApplicationContext.bindService(intent, serviceConnection, i);
	}

	/***********************************************************************************************
     * Monitoring
     **********************************************************************************************/
	@ReactMethod
	public void startMonitoring(String regionId, String beaconUuid, int minor, int major, Callback resolve, Callback reject) {
		Log.d(LOG_TAG, "startMonitoring, monitoringRegionId: " + regionId + ", monitoringBeaconUuid: " + beaconUuid + ", minor: " + minor + ", major: " + major);

		try {
			Region region = createRegion(
			regionId, beaconUuid, String.valueOf(minor).equals("-1") ? "": String.valueOf(minor), String.valueOf(major).equals("-1") ? "": String.valueOf(major));

			mBeaconManager.startMonitoringBeaconsInRegion(region);

			this.MyRegion = region;

			resolve.invoke();
		} catch(Exception e) {
			Log.e(LOG_TAG, "startMonitoring, error: ", e);
			reject.invoke(e.getMessage());
		}
	}

	private MonitorNotifier mMonitorNotifier = new MonitorNotifier() {@Override
		public void didEnterRegion(Region region) {

      // Uncomment below if no repeteing events for the same beacon is wanted
      // Only process this beacon if we have not done so before
      // if (region.getId1() != null && !mBeaconsProcessed.contains(region.getId1().toString())) {
        Log.i(LOG_TAG, "regionDidEnter");

        sendEvent(mReactContext, "regionDidEnter", createMonitoringResponse(region));
        // Mark this beacon as having already been processed.
        mBeaconsProcessed.add(region.getId1().toString());
      // } else {
      //   Log.i(LOG_TAG, "regionDidEnter, but Beacon already detected once");
      // }

			try {
				mBeaconManager.startRangingBeaconsInRegion(MyRegion);
			} catch(RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void didExitRegion(Region region) {
			Log.i(LOG_TAG, "didExitRegion");
			sendEvent(mReactContext, "regionDidExit", createMonitoringResponse(region));

			try {
				mBeaconManager.stopRangingBeaconsInRegion(MyRegion);
			} catch(RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void didDetermineStateForRegion(int i, Region region) {
		}
	};

	private WritableMap createMonitoringResponse(Region region) {
		WritableMap map = new WritableNativeMap();
		map.putString("identifier", region.getUniqueId());
		map.putString("uuid", region.getId1() != null ? region.getId1().toString() : "");
		map.putInt("major", region.getId2() != null ? region.getId2().toInt() : 0);
		map.putInt("minor", region.getId3() != null ? region.getId3().toInt() : 0);
		return map;
	}

	@ReactMethod
	public void stopMonitoring(String regionId, String beaconUuid, int minor, int major, Callback resolve, Callback reject) {

		Region region = createRegion(
		regionId, beaconUuid, String.valueOf(minor).equals("-1") ? "": String.valueOf(minor), String.valueOf(major).equals("-1") ? "": String.valueOf(major)
		// minor,
		// major
		);

		try {
			mBeaconManager.stopMonitoringBeaconsInRegion(region);
			this.MyRegion = null;
			resolve.invoke();
		} catch(Exception e) {
			Log.e(LOG_TAG, "stopMonitoring, error: ", e);
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void stopBeaconService(Boolean toggled, Callback resolve, Callback reject) {
    Log.e(LOG_TAG, "Stopping Beacon Service");
		try {
      // if (toggled.booleanValue()) {
      //   // if (Build.VERSION.SDK_INT > 26) {
			// 	// 	NotificationManager notificationManager = (NotificationManager) mApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
			// 	// 	notificationManager.deleteNotificationChannel("beacons");
      //   //   Log.e(LOG_TAG, "Delete notif channel");
      //   //   unbindManager();
			// 	// }
      // } else {
        unbindManager();
        mBeaconManager.disableForegroundServiceScanning();
      // }

			resolve.invoke();
		} catch(Exception e) {
			Log.e(LOG_TAG, "stopBeaconService, error: ", e);
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void startBeaconService(Boolean toggled, Callback resolve, Callback reject) {
		Log.e(LOG_TAG, "Starting Beacon Service");
		this.mApplicationContext = mReactContext.getApplicationContext();
    this.mBeaconManager = BeaconManager.getInstanceForApplication(mApplicationContext);

    try {
      if (toggled.booleanValue()) {
        unbindManager();
      }
      if (!mBeaconManager.isAnyConsumerBound()) {
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); // IBeacon

				// Notification.Builder builder = new Notification.Builder(mApplicationContext);
				// builder.setSmallIcon(mApplicationContext.getResources().getIdentifier("ic_notification", "mipmap", mApplicationContext.getPackageName()));
				// builder.setContentTitle("Scanning for Beacons");
				// Class intentClass = getMainActivityClass();
				// Intent intent = new Intent(mApplicationContext, intentClass);
				// PendingIntent pendingIntent = PendingIntent.getActivity(mApplicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				// builder.setContentIntent(pendingIntent);

				// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				// 	NotificationChannel channel = new NotificationChannel("beacons", "Beacons", NotificationManager.IMPORTANCE_DEFAULT);
				// 	channel.setDescription("Beacons ON notification");
				// 	NotificationManager notificationManager = (NotificationManager) mApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
				// 	notificationManager.createNotificationChannel(channel);
        //   builder.setChannelId(channel.getId());
        // }
        // Log.e(LOG_TAG, "Notification Channel started");

        // mBeaconManager.enableForegroundServiceScanning(builder.build(), 12345);
        mBeaconManager.disableForegroundServiceScanning();
        mBeaconManager.setEnableScheduledScanJobs(false);
        mBeaconManager.setBackgroundScanPeriod(10000l);
        mBeaconManager.setBackgroundBetweenScanPeriod(60000l);
        mBeaconManager.setForegroundScanPeriod(10000l);
        mBeaconManager.setForegroundBetweenScanPeriod(60000l);
        mBeaconManager.updateScanPeriods();


        Log.e(LOG_TAG, "Service started");
				bindManager();
      }
			resolve.invoke();
		} catch(Exception e) {
			Log.e(LOG_TAG, "startBeaconService, error: ", e);
			reject.invoke(e.getMessage());
    }
	}

	/***********************************************************************************************
     * Ranging
     **********************************************************************************************/
	@ReactMethod
	public void startRanging(String regionId, String beaconUuid, Callback resolve, Callback reject) {
    Log.d(LOG_TAG, "startRanging, rangingRegionId: " + regionId + ", rangingBeaconUuid: " + beaconUuid);
    Region region = createRegion(regionId, beaconUuid);

		try {
			mBeaconManager.startRangingBeaconsInRegion(region);
			resolve.invoke();
		} catch(Exception e) {
			Log.e(LOG_TAG, "startRanging, error: ", e);
			reject.invoke(e.getMessage());
		}
	}

	private RangeNotifier mRangeNotifier = new RangeNotifier() {@Override
		public void didRangeBeaconsInRegion(Collection < Beacon > beacons, Region region) {
			Log.d(LOG_TAG, "Ranging beacons: " + beacons.toString());
			Log.d(LOG_TAG, "Ranging region: " + region.toString());
      sendEvent(mReactContext, "regionDidEnter", createRangingResponse(beacons, region));

			final JSONArray beaconArray = new JSONArray();
			for (final Beacon beacon: beacons) {
				beaconArray.put(new JSONObject() {
					{
						try {
							put("uuid", beacon.getId1() != null ? beacon.getId1().toString() : "");
							// put("major", beacon.getId2() != null ? beacon.getId2().toInt() : 0);
							// put("minor", beacon.getId3() != null ? beacon.getId3().toInt() : 0);
							// put("rssi", beacon.getRssi());
							// if (beacon.getDistance() == Double.POSITIVE_INFINITY || Double.isNaN(beacon.getDistance()) || beacon.getDistance() == Double.NaN || beacon.getDistance() == Double.NEGATIVE_INFINITY) {
							// 	put("distance", 999.0);
							// 	put("proximity", "far");
							// } else {
							// 	put("distance", beacon.getDistance());
							// 	put("proximity", getProximity(beacon.getDistance()));
							// }
						} catch(JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}

			JSONArray sortedBeaconArray = null;
			try {
				sortedBeaconArray = this.sort(beaconArray, "distance", true);
			} catch(JSONException e) {
				e.printStackTrace();
			}

			if (sortedBeaconArray != null) {
        Log.e(LOG_TAG, "SortedBeaconArray: " + sortedBeaconArray.toString());
			} else {
				Log.d(LOG_TAG, "SortedBeaconArray: []");
			}

			final JSONArray finalSortedBeaconArray = sortedBeaconArray;

			JSONObject nearestBeacon = null;

			if (finalSortedBeaconArray != null) {
				try {
					nearestBeacon = (JSONObject) finalSortedBeaconArray.get(0);
				} catch(JSONException e) {
					e.printStackTrace();
				}
			}

      final JSONObject finalNearestBeacon = nearestBeacon;
		}

		private JSONArray sort(JSONArray jsonArr, String sortBy, boolean sortOrder) throws JSONException {
			JSONArray sortedJsonArray = new JSONArray();

			List < JSONObject > jsonValues = new ArrayList();
			for (int i = 0; i < jsonArr.length(); i++) {
				jsonValues.add(jsonArr.getJSONObject(i));
			}
			final String KEY_NAME = sortBy;
			final Boolean SORT_ORDER = sortOrder;
			Collections.sort(jsonValues, new Comparator < JSONObject > () {

				@Override
				public int compare(JSONObject a, JSONObject b) {
					Double valA = new Double( - 1);
					Double valB = new Double( - 1);

					try {
						valA = (Double) a.get(KEY_NAME);
						valB = (Double) b.get(KEY_NAME);
					}
					catch(JSONException e) {
						//exception
					}
					if (SORT_ORDER) {
						return valA.compareTo(valB);
					} else {
						return - valA.compareTo(valB);
					}
				}
			});

			for (int i = 0; i < jsonArr.length(); i++) {
				sortedJsonArray.put(jsonValues.get(i));
			}

			return sortedJsonArray;
		}
	};

	private WritableMap createRangingResponse(Collection < Beacon > beacons, Region region) {
		WritableMap map = new WritableNativeMap();
		WritableArray a = new WritableNativeArray();
		for (Beacon beacon: beacons) {
			WritableMap b = new WritableNativeMap();
			b.putString("uuid", beacon.getId1() != null ? beacon.getId1().toString() : "");
			b.putString("identifier", beacon.getId1() != null ? beacon.getId1().toString() : "");
			if (beacon.getIdentifiers().size() > 2) {
				b.putInt("major", beacon.getId2() != null ? beacon.getId2().toInt() : 0);
				b.putInt("minor", beacon.getId3() != null ? beacon.getId3().toInt() : 0);
			}
			b.putInt("rssi", beacon.getRssi());
			if (beacon.getDistance() == Double.POSITIVE_INFINITY || Double.isNaN(beacon.getDistance()) || beacon.getDistance() == Double.NaN || beacon.getDistance() == Double.NEGATIVE_INFINITY) {
				b.putDouble("distance", 999.0);
				b.putString("proximity", "far");
			} else {
				b.putDouble("distance", beacon.getDistance());
				b.putString("proximity", getProximity(beacon.getDistance()));
      }
			map = b;
		}

    return map;
	}

	private String getProximity(double distance) {
		if (distance == -1.0) {
			return "unknown";
		} else if (distance < 1) {
			return "immediate";
		} else if (distance < 3) {
			return "near";
		} else {
			return "far";
		}
	}

	@ReactMethod
	public void stopRanging(String regionId, String beaconUuid, Callback resolve, Callback reject) {
		if (!mBeaconManager.isBound(this)) {
			return;
		}

    Region region = createRegion(regionId, beaconUuid);

		try {
      mBeaconManager.stopRangingBeaconsInRegion(region);
      this.MyRegion = null;
			resolve.invoke();
		} catch(Exception e) {
			Log.e(LOG_TAG, "stopRanging, error: ", e);
			reject.invoke(e.getMessage());
		}
	}

	@ReactMethod
	public void setDebugApi(String debugApi) {
		Log.e(LOG_TAG, "setDebugApi " + debugApi);
		this.debugApi = debugApi;
	}

	@ReactMethod
	public void setRequestToken(String token) {
		Log.e(LOG_TAG, "setRequestToken " + token);
		this.requestToken = token;
	}

	@ReactMethod
	public void setBeaconRequestApi(String requestApi) {
		Log.e(LOG_TAG, "setBeaconRequestApi " + requestApi);
		this.beaconRequestApi = requestApi;
	}

	@ReactMethod
	public void setUserId(String userId) {
		Log.e(LOG_TAG, "setUserId " + userId);
	}

	@ReactMethod
	public void setNotificationDelay(Number notificationDelay) {
		Log.e(LOG_TAG, "setNotificationDelay " + notificationDelay);
	}

	/***********************************************************************************************
     * Utils
     **********************************************************************************************/
	private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    Log.e(LOG_TAG, "SHOULD SEND EVENT");
		if (reactContext.hasActiveCatalystInstance()) {
      Log.e(LOG_TAG, "EVENT SENT");
			reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
		}
	}

	private Region createRegion(String regionId, String beaconUuid) {
		Identifier id1 = (beaconUuid == null) ? null: Identifier.parse(beaconUuid);
		return new Region(regionId, id1, null, null);
	}

	private Region createRegion(String regionId, String beaconUuid, String minor, String major) {
		Identifier id1 = (beaconUuid == null) ? null: Identifier.parse(beaconUuid);
		return new Region(
		regionId, id1, major.length() > 0 ? Identifier.parse(major) : null, minor.length() > 0 ? Identifier.parse(minor) : null);
	}

	private Class getMainActivityClass() {
		String packageName = mApplicationContext.getPackageName();
		Intent launchIntent = mApplicationContext.getPackageManager().getLaunchIntentForPackage(packageName);
		String className = launchIntent.getComponent().getClassName();
		try {
			return Class.forName(className);
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void checkOrCreateChannel(NotificationManager manager) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
		if (channelCreated) return;
		if (manager == null) return;

		@SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Smart_Space_Pro_Channel", android.app.NotificationManager.IMPORTANCE_HIGH);
		channel.setDescription("Smart_Space_Pro_Channel_Description");
		channel.enableLights(true);
		channel.enableVibration(true);

		manager.createNotificationChannel(channel);
		channelCreated = true;
	}

	private Boolean isActivityRunning(Class activityClass) {
		ActivityManager activityManager = (ActivityManager) mApplicationContext.getSystemService(Context.ACTIVITY_SERVICE);
		List < ActivityManager.RunningTaskInfo > tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

		for (ActivityManager.RunningTaskInfo task: tasks) {
			if (activityClass.getCanonicalName().equalsIgnoreCase(task.baseActivity.getClassName())) return true;
		}

		return false;
	}

	private void sendDebug(JSONObject data) {
		final String debugApi = this.debugApi;
		new BeaconDebugRequest().execute(data, new JSONObject() {
			{
				try {
					put("debugApi", debugApi);
				} catch(JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void sendBeacon(JSONObject data) {
		final String beaconRequestApi = this.beaconRequestApi;
		final String requestToken = this.requestToken;
		new BeaconRequest().execute(data, new JSONObject() {
			{
				try {
					put("beaconRequestApi", beaconRequestApi);
					put("requestToken", requestToken);
				} catch(JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
