package cordova.plugin.custom.ble;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;

import com.seamoon.blellib.LockManager;
import com.seamoon.blellib.bluetooth.ClientManager;
import com.seamoon.blellib.bluetooth.IConnectCallBack;
import com.seamoon.blellib.bluetooth.ISearchCallBack;
import com.seamoon.blellib.bluetooth.IWriteCallBack;
import com.seamoon.blellib.bluetooth.OadLockerProtocol;
import com.seamoon.blellib.bluetooth.IBeaconListContract;
import com.seamoon.blellib.bluetooth.ICBrecvData;
import com.seamoon.blellib.bluetooth.ICBrecvProgress;
import com.seamoon.blellib.bluetooth.LockerProtocol;

import com.seamoon.blellib.datafactory.LockerAdvertising;
import com.seamoon.blellib.datafactory.LockerModel;
import com.seamoon.blellib.datafactory.Methods;
import com.seamoon.blellib.datafactory.PowerOffRecord;
import com.seamoon.blellib.datafactory.ResultMonitor;
import com.seamoon.blellib.sqlite.DBHelper;
import com.seamoon.blellib.bluetooth.BluetoothKitManager;

import com.seamoon.codelib.MainApi;
import com.seamoon.codelib.bluetoothKeyUtils.BaseUtils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import android.os.Bundle;
import android.os.Looper;
import android.os.CountDownTimer;
import android.widget.Toast;
import android.util.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import org.apache.cordova.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class CustomBLE extends CordovaPlugin implements Serializable{

    private static LockerModel lockerModel;
    private static LockManager lockManager;
    private String lockSn;
    // private String snInfo = "6RLxgWwJKJkCKEFa6brXrB9lkaAHvrPJi0vLokBgd2aCu/0RCXaTDkui4ASPJGbc/wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//Ndazkjr98mbDEwZzQoxrEhzyNislT+jE81eEsD4t2PYqoE3ll1DWmfaJ/gcd98cArwp++6XTAcupECUJbt9NgQ4qZlVxTNm7OgQ8TBSIt71KxQfNH0iMUHJ0KRMwBVXlysgpW7Tzt+sl3GoX3zF6n/BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR/+i/cVq1OcLiYGSr/pfPSVQ=9f36b80de3a1c8d3472f351c3202d7a6";
    private String snInfo;
    private String key;
    private String newKey;
    private static final String downloadUrl = "https://airbnk.51qd.org/firmware/airbnk/M300_B3V1-1-3.bin";
    private Serializable records;
    private final static String TAG = "LockLib";
    private OadLockerProtocol oadLockerProtocol;
    boolean isFounded = false;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private SQLiteDatabase db;
    private boolean mConnected;
    private LockerAdvertising adv;
    private BluetoothKitManager kitManager;

    public static CustomBLE instance = null;
    public static CordovaWebView cordovaWebView;
    public static CordovaInterface cordovaInterface;
    public static Activity mainActivity;
    private boolean isSend;
    private boolean hasUpgraded;  //升级成功
    private String filePath;
    private LockerModel lockBlueEnt;
    private static int REQUEST_ENABLE_BT = 1111;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        instance = this;
        cordovaWebView = webView;
        cordovaInterface = cordova;
    }

    // @Override
    // public void onDestroy() {
    //     instance = null;
    // }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    
        if (action.equals("search")){
            snInfo = args.getJSONObject(0).getString("snInfo");
            key = args.getJSONObject(0).getString("key");
            this.search(snInfo, callbackContext);
            return true;
        } else if (action.equals("onClick")){
            // String clickOn = args.getJSONObject(0).getString("clickOn");
            this.onClick(args, callbackContext);
            return true;
        }
        return false;
    }

    private void search(String snInfo, CallbackContext callbackContext){
        lockManager = null;
        Context context=this.cordova.getActivity().getApplicationContext();
        Context activity=this.cordova.getActivity();
        DBHelper contactDBHelper = new DBHelper(context);
        db = contactDBHelper.getWritableDatabase();
        if (snInfo.length() > 0) {
            lockSn = MainApi.getSnFromSnInfo(snInfo);

            if(lockSn == null){
                callbackContext.error(context +"snInfo error!");
                return;
            }
            // callbackContext.success("loM"+lockManager+"lockSN:" + lockSn);
            Cursor cursor = db.rawQuery("select lockModel,lockSn from sdk_lockmodel where lockSn = ?", new String[]{lockSn});
            if(cursor.moveToNext()){
                byte data[] = cursor.getBlob(cursor.getColumnIndex("lockModel"));
                ByteArrayInputStream arrayInputStream = null;
                ObjectInputStream inputStream = null;
                try {
                    arrayInputStream = new ByteArrayInputStream(data);
                    inputStream = new ObjectInputStream(arrayInputStream);
                    lockerModel = (LockerModel) inputStream.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error("err"+e);
                }finally {
                    if(inputStream != null){
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(arrayInputStream != null){
                        try {
                            arrayInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mainActivity = CustomBLE.instance.cordovaInterface.getActivity();
                lockManager = new LockManager(mainActivity, 60, true, lockSn,connectCallBack);
                lockManager.setLockerModel(lockerModel);
                lockManager.setSQLiteDatabase(db);
                lockManager.initBluetooth();
                lockManager.connectDevice(connectCallBack);
                cursor.close();
                return;
            }
            cursor.close();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    mainActivity = cordovaInterface.getActivity();
                    lockManager = new LockManager(mainActivity, 60, true, lockSn,connectCallBack);
                    lockManager.setSQLiteDatabase(db);
                    callbackContext.success("loM"+lockManager);
                    lockManager.startScan(new ISearchCallBack() {
                        @Override
                        public void updateDevicesList(List<LockerModel> lockerModels) {
                            if (lockerModels.size() > 0) {
                                lockerModel = lockerModels.get(0);
                                callbackContext.success("loM"+lockerModel);
                                if (!isFounded) {
                                    isFounded = true;
                                    lockManager.connectDevice(connectCallBack);
                                }
                            } else{
                                callbackContext.error("locMs"+lockerModels.toString());
                            }
                        }
                        @Override
                        public void onSearchStopped() {
                            // Log.i(TAG, "onSearchStopped");
                            if (!isFounded){

                            }
                        }
                    });
                }
            }).start();
        } else {
            callbackContext.error("Sninfo cannot be empty!");
        }
    }

    private IConnectCallBack connectCallBack = new IConnectCallBack() {
        @Override
        public void onSuccess(int code) {
            // mConnected = true;
            // view_Connect.setEnabled(false);
            // view_Disconnect.setEnabled(true);
            // lockManager.stopTimer();
            getAdvertisingInfo();
            // startTimer();
            // callbackContext.success("connected"+code);
        }
        @Override
        public void onFailed(int errorCode) {
            // mConnected = false;
            // callbackContext.success("faild" + errorCode);
        }
    };

    private void sendCode(String sendCode) {
        if (lockManager != null) {
            // stopTimer();
            lockManager.sendDirective(sendCode, writeCallBack);
        }
    }

    private  boolean missDialog;
    private CountDownTimer countDownTimer;
    public  void startTimer1(long time) {
        countDownTimer = new CountDownTimer(time, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {

            }
            @Override
            public void onFinish() {
                if(missDialog == true){
                    // LoadingStaticDialog.loadDialogDismiss();
                    // XToastUtil.showToast(mainActivity, "Request timeout, please try again!");
                    mainActivity = cordovaInterface.getActivity();
                    Toast.makeText(mainActivity, "req timeout, please try again", Toast.LENGTH_LONG).show();
                    stopTimer1();
                }
            }
        }.start();
    }

    public void stopTimer1() {
        Log.i(TAG, "--------  stopTimer1  --------");
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }


    private  void getAdvertisingInfo() {
        // Log.i(TAG, "Send GetAdvCode ----------------");
        lockManager.sendDirective(Methods.GetAdvCode, writeCallBack);
    }





    public void onClick(JSONArray arg, CallbackContext callbackContext) throws JSONException{
        String clickOn = arg.getJSONObject(0).getString("clickOn");
        // if(!mConnected && v.getId() != R.id.disconnect && v.getId() != R.id.connect && v.getId() != R.id.upgrade){
        //     XToastUtil.showToast(this,"Not connected!");
        //     return;
        // }
        // key = "";
        mainActivity = CustomBLE.instance.cordovaInterface.getActivity();
        // Activity mactivity = cordovaInterface.getActivity();
        switch (clickOn) {
            case "connect" :{
                // refreshBtnImg(2);
                search(snInfo, callbackContext);
                // callbackContext.success(snInfo+ "connect called");
                callbackContext.success("connect called");
                break;
            }
            case "unlock": {
                // Log.i(TAG,"KEY:"+key);
                String openStr = MainApi.lockOperation(adv.getData(), snInfo, 1,key);
                if("500".equals(openStr)){
                    callbackContext.error("Key error!");
                    break;
                }
                // LoadingStaticDialog.showLoadingDialog(mainActivity, "Operating");
                // startTimer1(20000);
                // missDialog = true;
                sendCode(openStr);
                callbackContext.success(openStr+ "unlock called");
                break;
            }
            case "lock": {
                String closeStr = MainApi.lockOperation(adv.getData(), snInfo, 2,key);
                if("500".equals(closeStr)){
                    callbackContext.error(mainActivity+"Key error!");
                    break;
                }
                // LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
                // startTimer1(20000);
                // missDialog = true;
                sendCode(closeStr);
                callbackContext.success(closeStr+ "lock called");
                break;
            }
            case "setlocaltime": {
                String localTime = arg.getJSONObject(0).getString("localTime");
                if(getCheckTimeToSet(localTime)){
                    Intent intent = new Intent();
                    intent.putExtra("localTime", localTime);
                    // mainActivity.setResult(666,intent);
                    onActivityResult(666,intent);
                    // SynchronizeActivity.this.finish();
                    callbackContext.success("set time called"+localTime);
                } else {
                    callbackContext.error("set time error");
                }
                // Intent intent =new Intent(mainActivity+ SynchronizeActivity.class);
                // startActivityForResult(intent,666);
                
                break;
            }
            case "record": {
                String recordsStr = MainApi.getLockRecords(adv.getData(), snInfo,key);
                if("500".equals(recordsStr)){
                    // XToastUtil.showToast(mainActivity,"Key error!");
                    callbackContext.error("key error");
                    break;
                }
                Log.i(TAG, "recordsString -> " + recordsStr);
                // LoadingStaticDialog.showLoadingDialog(mainActivity, "Operating");
                // startTimer1(20000);
                // missDialog = true;
                sendCode(recordsStr);
                countDownTimer = new CountDownTimer(10000, 1000L) {
                    @Override
                    public void onTick(long millisUntilFinished) {
        
                    }
                    @Override
                    public void onFinish() {
                        
                            mainActivity = cordovaInterface.getActivity();
                            Toast.makeText(mainActivity, "req timeout, please try again", Toast.LENGTH_LONG).show();
                            callbackContext.success(records.toString());
                            stopTimer1();
                    }
                }.start();
                // callbackContext.success(records.toString());
                break;
            }
            case "config": {
                // Intent intent =new Intent(mainActivity, ConfigActivity.class);
                // intent.putExtra("lockType",MainApi.getLockTypeFromSnInfo(snInfo));
                // startActivityForResult(intent,555);
                JSONObject jo = new JSONObject();
                    jo.put("action", "config");
                    jo.put("locktype", MainApi.getLockTypeFromSnInfo(snInfo));
                callbackContext.success(jo);
                break;
            }
            case "setconfig": {
                Intent intent = new Intent();
                intent.putExtra("direction", arg.getJSONObject(0).getString("direction"));
                intent.putExtra("enableAutoClose", arg.getJSONObject(0).getString("enableAutoClose"));
                intent.putExtra("automaticClosingTime", arg.getJSONObject(0).getString("automaticClosingTime"));
                intent.putExtra("enableMagnetic", arg.getJSONObject(0).getString("enableMagnetic"));
                intent.putExtra("magneticTime", arg.getJSONObject(0).getString("magneticTime"));
                intent.putExtra("auxiliaryrotation",arg.getJSONObject(0).getString("auxiliaryrotation"));
                onActivityResult(555,intent);
                callbackContext.success("setConfig");
                break;
            }
            case "upgrade": {
                // stopTimer();
                if (lockManager != null){
                    lockManager.destory();
                }
                // Intent intent =new Intent(mainActivity, UpgradeActivity.class);
                // String snInfoNow = snInfo.substring(0, snInfo.length() - 32);
                // Map<String, Object> snInfoMap = BaseUtils.dispose(snInfoNow);
                // intent.putExtra("lockSn",lockSn);
                // intent.putExtra("secretKey",snInfoMap.get("secretKey").toString());
                // intent.putExtra("snInfo",snInfo);
                // intent.putExtra("key",key);
                // startActivityForResult(intent,222);
                callbackContext.success("Not Available");
                break;
            }
            case "setKey":{
                String keyToSet = arg.getJSONObject(0).getString("keyToSet");

                if(getAndcheck(keyToSet)){
                    Intent intent = new Intent();
                    intent.putExtra("key", keyToSet);
                    onActivityResult(333,intent);
                    // mainActivity.setResult(333,intent);
                    // SetKeyActivity.this.finish();
                    callbackContext.success("setkey called"+keyToSet);
                } else {
                    callbackContext.error("keyToSet error"+keyToSet);
                }
                // Intent intent =new Intent(mainActivity, SetKeyActivity.class);
                // startActivityForResult(intent,333);
                break;
            }
            case "gettime":{
                String gettimestr = MainApi.getLockTime(adv.getData(), snInfo,key);
                if("500".equals(gettimestr)){
                    // XToastUtil.showToast(MainActivity.this,"Key error!");
                    callbackContext.error("Key Error+ gettime called");
                    break;
                }
                Log.i(TAG, "gettimestr -> " + gettimestr);
                // LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
                // startTimer1(10000);
                // missDialog = true;
                sendCode(gettimestr);
                callbackContext.success(gettimestr);
                break;
            }
            case "disconnect":{
                // stopTimer();
                if (lockManager != null){
                    lockManager.destory();
                }
                mConnected = false;
                isFounded = false;
                // mainActivity.finish();
                break;
            }
        }
    }



    private boolean getAndcheck(String ed_key){
        mainActivity = CustomBLE.instance.cordovaInterface.getActivity();
        if(ed_key != null){
            // key = ed_key.getText().toString();
            if("".equals(ed_key)){
                Toast.makeText(mainActivity, "key cannot be empty!", Toast.LENGTH_LONG).show();
                return false;
            }else{
                if(ed_key.length()>32){
                    String bindKeyHex = ed_key.substring(0,ed_key.length()-32);
                    String sign1 = BaseUtils.MD5(bindKeyHex, "");
                    if(!sign1.equals(ed_key.substring(ed_key.length()-32))){
                        Toast.makeText(mainActivity, "key error!", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }else{
                    Toast.makeText(mainActivity, "key error!", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean getCheckTimeToSet(String localTime){
        mainActivity = CustomBLE.instance.cordovaInterface.getActivity();
        if(localTime != null){
            // localTime = ed_localTime.getText().toString();
            if("".equals(localTime)){
                Toast.makeText(mainActivity, "localTime cannot be empty!", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }


    private final IWriteCallBack writeCallBack = new IWriteCallBack() {
        @Override
        public void onSuccess(byte[] recvData) {
            // mainActivity = CustomBLE.instance.cordovaInterface.getActivity();
            if (ResultMonitor.isAdvertising(recvData)) {
                if (lockerModel != null) {
                    adv = ResultMonitor.transferDataToAdv(lockSn, recvData);
                    int states = adv.getStates();
                    int magnentStates = adv.getMagnetStates();
                    if(adv.isMagnetEnable() &&
                            adv.getBordModel() < 4  && adv.getSoftVersion().compareTo("1.2.0") >= 0){
                        // if (states == 2) {//卡住
                        //     refreshBtnImg(6);
                        // } else if (states == 1 && magnentStates == 0) {
                        //     refreshBtnImg(9);
                        // } else if (states == 0 && magnentStates == 0) {
                        //     refreshBtnImg(10);
                        // } else if (states == 1 && magnentStates == 1) {
                        //     refreshBtnImg(11);
                        // } else if (states == 0 && magnentStates == 1) {
                        //     refreshBtnImg(12);
                        // }
                    }else{
                        // if (states == 2) {
                        //     refreshBtnImg(6);
                        // } else if (states == 1) {
                        //     refreshBtnImg(3);
                        // } else if (states == 0) {
                        //     refreshBtnImg(1);
                        // }
                    }
                }
            }else{
                int type = com.seamoon.blellib.datafactory.ResultMonitor.checkDataType(recvData);
                if(type == 3){
                    // LoadingStaticDialog.loadDialogDismiss();
                    // stopTimer1();
                    // missDialog = false;
                    List<PowerOffRecord> list = com.seamoon.blellib.datafactory.ResultMonitor.transferDataToRecords(recvData);
                    // Intent intent = new Intent();
                    // intent.putExtra("records",(Serializable) list);
                    records = ((Serializable) list);
                    Toast.makeText(mainActivity, "Records:"+list, Toast.LENGTH_LONG).show();
                    // intent.putExtra("snInfo",snInfo);
                    // onActivityResult(888,intent);
                    // startActivityForResult(intent,888);
                    // startTimer();
                }else if(type == 12){
                    // LoadingStaticDialog.loadDialogDismiss();
                    // stopTimer1();
                    // missDialog = false;
                    // callbackContext.success(mainActivity+"Success! t-12");
                    // Intent intent = new Intent();
                    // intent.putExtra("newKey", newKey);
                    // setResult(123,intent);
                    // finish();
                }else if(type == 13){
                    // LoadingStaticDialog.loadDialogDismiss();
                    // stopTimer1();
                    // missDialog = false;
                    // long time = com.seamoon.blellib.datafactory.ResultMonitor.getTimeFromData(recvData)*60*1000;
                    // DialogCommon dialogCommon = new DialogCommon(MainActivity.this, new DialogUtils.OnResult() {
                    //     @Override
                    //     public void onResult(String which) {
                    //         if (which != null) {
                    //             if (which.equals("22222")) {
                    //             }
                    //         }
                    //     }
                    // });
                    // dialogCommon.setContent(UtilTools.utcToLocal(time));
                    // dialogCommon.setRightBtnTitle("OK");
                    // dialogCommon.show();
                }else{
                    // LoadingStaticDialog.loadDialogDismiss();
                    // stopTimer1();
                    // missDialog = false;
                    // callbackContext.success("Success!");
                    // startTimer();
                }
            }
            if(lockerModel != null){
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(arrayOutputStream);
                    objectOutputStream.writeObject(lockerModel);
                    objectOutputStream.flush();
                    byte data1[] = arrayOutputStream.toByteArray();
                    objectOutputStream.close();
                    arrayOutputStream.close();
                    db.execSQL("update sdk_lockmodel set lockModel=? where lockSn = ?",
                            new Object[]{data1, lockSn});
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        @Override
        public void onFailed(int errorCode) {
            // startTimer();
            // callbackContext.error("err"+errorCode);
        }
    };


    // @Override
    protected void onActivityResult(int requestCode, Intent data) {
        mainActivity = cordovaInterface.getActivity();
        // mainActivity.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case 555:
                if(data != null){
                    int direction = data.getIntExtra("direction",0);
                    int enableAutoClose = data.getIntExtra("enableAutoClose",0);
                    int automaticClosingTime = data.getIntExtra("automaticClosingTime",0)/10;
                    int enableMagnetic = data.getIntExtra("enableMagnetic",0);
                    int magneticTime = Integer.parseInt(data.getStringExtra("magneticTime"));
                    int auxiliaryrotation = data.getIntExtra("auxiliaryrotation",0);
                    String configStr = MainApi.configLock(adv.getData(),snInfo,
                            direction, enableAutoClose, automaticClosingTime, enableMagnetic, magneticTime,auxiliaryrotation,
                            System.currentTimeMillis()/1000,key);
                    if("500".equals(configStr)){
                        // XToastUtil.showToast(MainActivity.this,"Key error!");
                        Toast.makeText(mainActivity, "key error", Toast.LENGTH_LONG).show();
                        break;
                    }
                    Log.i(TAG, "configString -> " + configStr);
                    if(configStr == null){
                        Toast.makeText(mainActivity, "ERROR!", Toast.LENGTH_LONG).show();
                    }
                    // LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
                    Toast.makeText(mainActivity, "Operating", Toast.LENGTH_LONG).show();
                    // startTimer1(20000);
                    // missDialog = true;
                    sendCode(configStr);
                }
                break;
            case 666:
                if(data != null){
                    String localTime = data.getStringExtra("localTime");
                    String syncTimeStr = MainApi.setLocalTime(adv.getData(), snInfo, Long.parseLong(localTime),key);
                    if("500".equals(syncTimeStr)){
                        // XToastUtil.showToast(MainActivity.this,"Key error!");
                        Toast.makeText(mainActivity, "Key error!", Toast.LENGTH_LONG).show();
                        break;
                    }
                    Log.i(TAG, "syncTimeString -> " + syncTimeStr);
                    if(syncTimeStr == null){
                        Toast.makeText(mainActivity, "ERROR!", Toast.LENGTH_LONG).show();
                    }
                    // LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
                    Toast.makeText(mainActivity, "Operating", Toast.LENGTH_LONG).show();
                    // startTimer1(20000);
                    // missDialog = true;
                    sendCode(syncTimeStr);
                }
                break;
            case 777:
                //清除掉数据
                db.execSQL("delete from sdk_lockmodel  where lockSn = ?", new Object[]{lockSn});
                break;
            case 888:
                if(data != null){
                    String mark = data.getStringExtra("mark");
                    if(mark != null){
                        String deleteStr = MainApi.deleteLockRecords(adv.getData(),snInfo,key);
                        if("500".equals(deleteStr)){
                            // XToastUtil.showToast(MainActivity.this,"Key error!");
                            Toast.makeText(mainActivity, "KEY ERROR!", Toast.LENGTH_LONG).show();
                            break;
                        }
                        Log.i(TAG, "deleteStr -> " + deleteStr);
                        Toast.makeText(mainActivity, "deleteStr -> " + deleteStr, Toast.LENGTH_LONG).show();
                        // LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
                        // startTimer1(20000);
                        // missDialog = true;
                        // sendCode(deleteStr);
                    }
                }
                break;
            case 222:
                // refreshBtnImg(13);
                // view_Disconnect.setEnabled(false);
                // view_Connect.setEnabled(true);
                lockManager.destory();
                break;
            case 333:
                if(data != null){
                    newKey = data.getStringExtra("key");
                    String resetKey = MainApi.resetkey(adv.getData(), snInfo,key,newKey);
                    if("500".equals(resetKey)){
                        // XToastUtil.showToast(MainActivity.this,"Key error!");
                        Toast.makeText(mainActivity, "KEY ERROR!", Toast.LENGTH_LONG).show();
                        break;
                    }
                    Log.i(TAG, "resetKey -> " + resetKey);
                    if(resetKey == null){
                        Toast.makeText(mainActivity, "ERROR!", Toast.LENGTH_LONG).show();
                    }
                    // LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
                    Toast.makeText(mainActivity, "Operating", Toast.LENGTH_LONG).show();
                    startTimer1(30000);
                    missDialog = true;
                    sendCode(resetKey);
                }
                break;
             default:
                 break;
        }
    }

    // @Override
    // protected void onDestroy() {
    //     Log.i(TAG, "----------onDestroy ---------");
    //     super.onDestroy();
    //     disconnectBle();
    //     stopTimer1();
    // }

    private void disconnectBle() {
        
        // ClientManager.getClient(this).stopSearch();
        if(lockerModel != null){
            String lockMacAddress = lockerModel.getMacAddress();
            // if( lockMacAddress != null) {
            //     ClientManager.getClient(this).unregisterConnectStatusListener(lockMacAddress, mConnectStatusListener);
            //     ClientManager.getClient(this).disconnect(lockMacAddress);
            //     ClientManager.getClient(this).clearRequest(lockMacAddress, 0);
            // }
        }
        // stopTimer();
    }
    private final BleConnectStatusListener mConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            Log.v(TAG, String.format("NNRoomDetailActivity onConnectStatusChanged %d in %s",
                    status, Thread.currentThread().getName()));
        }
    };



    // private void initView() {
    //     lockSn = getIntent().getStringExtra("lockSn");
    //     secretKey = getIntent().getStringExtra("secretKey");
    //     snInfo =  getIntent().getStringExtra("snInfo");
    //     key =  getIntent().getStringExtra("key");
    //     if (lockSn != null) {
    //         startTimer(30000);
    //         missDialog = true;
    //         checkProcess();
    //     }
    // }

    // @Override
    // protected void onDestroy() {
    //     super.onDestroy();
    //     stopDeviceScan();
    //     if (lockBlueEnt != null) {
    //         ClientManager.getClient(this).disconnect(lockBlueEnt.getMacAddress());
    //         ClientManager.getClient(this).clearRequest(lockBlueEnt.getMacAddress(), 0);
    //     }
    //     ClientManager.getClient(this).stopSearch();
    // }

    private void checkProcess() {
        mainActivity = cordovaInterface.getActivity();
        Log.i(TAG, "--------  checkProcess  --------");
        // tvUpgradeMsg.setText("Search and connect to the device");
        Toast.makeText(mainActivity, "Search and connect to the device", Toast.LENGTH_LONG).show();
        Context context=this.cordova.getActivity().getApplicationContext();
        ClientManager.getClient(context).registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                Log.i(TAG, String.format("onBluetoothStateChanged %b", openOrClosed));
                if (openOrClosed) {  //open
                    loadSecretKey();
                }
            }
        });
        BluetoothManager bluetoothManager = (BluetoothManager) mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivityForResult(intent, REQUEST_ENABLE_BT);
            return;
        }
        loadSecretKey();
    }


    private void loadSecretKey() {
        Log.i(TAG, "--------  loadSecretKey  --------");
        //tvUpgradeMsg.setText("loading key...");
        Toast.makeText(mainActivity, "loading key...", Toast.LENGTH_LONG).show();
        lockSearchInit();
    }

    // private void downloadFile(final int type) {
    //     if (downloadUrl != null) {
    //         // tvUpgradeMsg.setText("Downloading files...");
    //         Toast.makeText(mainActivity, "Downloading files...", Toast.LENGTH_LONG).show();
    //         // MyNovate myNovate = ApiClient.buildNovateNoCache(this);
    //         String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
    //         // myNovate.downloadMin("LockUpgrade", downloadUrl, fileName, new DownLoadCallBack() {
    //             @Override
    //             public void onStart(String s) {
    //                 super.onStart(s);
    //                 Log.v(TAG, "DownLoadMin onStart");
    //             }

    //             @Override
    //             public void onError(Throwable e) {
    //                 Log.v(TAG, "onError:" + e.getMessage());
    //                 tvUpgradeMsg.setText("Download firmware failed");
    //             }

    //             @Override
    //             public void onSucess(String key, String path, String name, long fileSize) {
    //                 Log.v(TAG, "DownLoad onSuccess");
    //                 Log.i(TAG, "path -> " + path + "fileName -> " + name);
    //                 filePath = path + name;
    //                 if(type == 0){
    //                     tvUpgradeMsg.setText("Send an upgrade command to the device");
    //                     sendUpgradeCode();
    //                 }
    //             }

    //             @Override
    //             public void onProgress(String key, int progress, long fileSizeDownloaded, long totalSize) {
    //                 super.onProgress(key, progress, fileSizeDownloaded, totalSize);
    //                 Log.v(TAG, "DownLoad onProgress progress: " + progress + "%");
    //             }

    //             @Override
    //             public void onCancel() {
    //                 super.onCancel();
    //                 Log.v(TAG, "DownLoadMin onCancel");
    //             }

    //         });

    //     }
    // }

    private void lockSearchInit() {
        Log.i(TAG, "--------  lockSearchInit  --------");
        Context context=this.cordova.getActivity().getApplicationContext();
        ClientManager.getClient(context).registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                Log.i(TAG, String.format("Lock Upgrading onBluetoothStateChanged %b", openOrClosed));
                Toast.makeText(mainActivity, "lock upgrading onBluetoothSt...", Toast.LENGTH_LONG).show();
            }
        });
        if (kitManager == null) {
            kitManager = new BluetoothKitManager(mainActivity,lockSn);
            kitManager.setBeaconListContract(beaconListContract);
        }
    }

    // LockerAdvertising adv ;
    IBeaconListContract beaconListContract = new IBeaconListContract() {
        @Override
        public void updataBeaconListView(LockerModel lockModel) {
            adv = new LockerAdvertising(lockModel.getBytes());
            LockerModel md = lockModel;
            Log.i(TAG, " --------------- Beacon Refresh -------------- \n serialNumber -> " + adv.getSerialNumber());
            Log.e(TAG, String.format("md.getAdv().isImageA() -> %b", adv.isImageA()));
            if (adv.isImageA()) {
                if (!hasUpgraded) {
                    // tvUpgradeMsg.setText("Do not remove the phone during the firmware upgrade");
                    Toast.makeText(mainActivity, "Do not remove the phone during the firmware upgrade", Toast.LENGTH_LONG).show();
                    try {
                        upgradeVersion(md);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (!isSend) {
                    // tvUpgradeMsg.setText("Downloading firmware upgrade");
                    Toast.makeText(mainActivity, "Downloading firmware upgrade", Toast.LENGTH_LONG).show();
                    lockBlueEnt = md;
                    // downloadFile(0);
                    isSend = true;
                }
                if (hasUpgraded) {
                    hasUpgraded = false;
                    // tvUpgradeMsg.setText("Upgrade success!");
                    Toast.makeText(mainActivity, "Upgrade Success", Toast.LENGTH_LONG).show();
                    destory();
                    DBHelper contactDBHelper = new DBHelper(mainActivity.getBaseContext());
                    SQLiteDatabase db = contactDBHelper.getWritableDatabase();
                    db.execSQL("delete from sdk_lockmodel  where lockSn = ?", new Object[]{lockSn});
                    mainActivity.setResult(555,new Intent());
                    // UpgradingActivity.this.finish();
                }
            }
            return;
        }
    };

    private void stopDeviceScan() {
        Context context=this.cordova.getActivity().getApplicationContext();
        ClientManager.getClient(context).stopSearch();
    }

    public void destory() {
        Context context=this.cordova.getActivity().getApplicationContext();
        stopDeviceScan();
        if (lockBlueEnt != null) {
            ClientManager.getClient(context).disconnect(lockBlueEnt.getMacAddress());
            ClientManager.getClient(context).clearRequest(lockBlueEnt.getMacAddress(), 0);
        }
    }

    // private void sendUpgradeCode() {
    //     Log.i(TAG, " -------------- sendUpgradeCode --------------- ");
    //     String sendCode = MainApi.upgrade(snInfo,(int) adv.getLockEvents(),key);
    //     if("500".equals(sendCode)){
    //         XToastUtil.showToast(UpgradingActivity.this,"Key error!");
    //         setResult(555,new Intent());
    //         UpgradingActivity.this.finish();
    //         return;
    //     }
    //     Log.i(TAG, " sendUpgradeCode V3 sendCode -> " + sendCode);
    //     if (lockerProtocol == null) {
    //         lockerProtocol = new LockerProtocol(UpgradingActivity.this, lockBlueEnt.getMacAddress());
    //         lockerProtocol.registerRecvCB(new ICBrecvData() {
    //             @Override
    //             public void recv(byte[] recvData, boolean isSuccess) {
    //                 if (isSuccess) {
    //                     int strResult = ResultMonitor.resultCode(recvData);
    //                     Log.i(TAG, "Respond -> " + strResult + "\n" + OTAFactory.byteToString(recvData) + "\nlockerProtocol");
    //                 }
    //             }
    //         });
    //     }
    //     connectDevice(OTAFactory.hexStr2Byte(sendCode));
    // }

    // private void connectDevice(final byte[] sendBytes) {
    //     Log.i(TAG, " ------- connectDevice ---------");
    //     BleConnectOptions options = new BleConnectOptions.Builder()
    //             .setConnectRetry(1)
    //             .setConnectTimeout(30000)
    //             .setServiceDiscoverRetry(1)
    //             .setServiceDiscoverTimeout(10000)
    //             .build();
    //     ClientManager.getClient(this).connect(lockBlueEnt.getMacAddress(), options, new BleConnectResponse() {
    //         @Override
    //         public void onResponse(int code, BleGattProfile profile) {
    //             Log.i(TAG, " --------- connectDevice ------------ \nonResponse code = " + code);
    //             if (code == LockerProtocol.REQUEST_SUCCESS) {
    //                 lockerProtocol.otaSend(sendBytes);
    //             } else {
    //             }
    //         }
    //     });
    // }

    private void upgradeVersion(LockerModel lockerMd) throws IOException {
        Log.i(TAG, " -------------- upgradeVersion --------------- ");
        lockBlueEnt = lockerMd;
        Log.i(TAG, "filePath -> " + filePath);
        if(filePath == null){
            // downloadFile(1);
            return;
        }
        destory();
        File file = new File(filePath);
        if (file.exists()) {
            Log.i(TAG, "file.exists() -> YES");
            FileInputStream fis = new FileInputStream(file);
            int length = fis.available();
            final byte[] buffer = new byte[length];
            fis.read(buffer);
            if (oadLockerProtocol == null) {
                oadLockerProtocol = new OadLockerProtocol(mainActivity, lockBlueEnt.getMacAddress());
            }
            upgradeDevice(buffer);
        } else {
            Log.i(TAG, "file.exists() -> NO");
        }
    }

    int upgradeVersionTag = 0;

    private void upgradeDevice(final byte[] bytes) {
        // stopTimer();
        // missDialog = false;
        Log.i(TAG, " ------- upgradeDevice ---------");
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(1)
                .setConnectTimeout(30000)
                .setServiceDiscoverRetry(1)
                .setServiceDiscoverTimeout(10000)
                .build();
        Context context=this.cordova.getActivity().getApplicationContext();
        ClientManager.getClient(context).connect(lockBlueEnt.getMacAddress(), options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                if (code == LockerProtocol.REQUEST_SUCCESS) {
                    if (upgradeVersionTag != 1) {               //避免同时多次连接
                        oadLockerProtocol.oadUpgrade(bytes, new ICBrecvProgress() {
                            @Override
                            public void upgradeProgress(float progress) {
                                Log.i(TAG, "upgradeDevice progress -> " + progress);
                                if (progress < 1) {
                                    String progressStr = String.format("%s %.1f%%", "Do not remove the phone during the firmware upgrade", progress * 100);
                                    // tvUpgradeMsg.setText(progressStr);
                                    Toast.makeText(mainActivity, "progress"+progressStr, Toast.LENGTH_LONG).show();
                                } else {
                                    if (kitManager != null) {
                                        kitManager.searchDevice();  //恢复扫描设备
                                    }
                                    hasUpgraded = true;
                                }
                            }
                        });
                        upgradeVersionTag++;
                    }
                } else {
                    upgradeVersionTag = 0;
                }
            }
        });
    }


}
