package cordova.plugin.custom.ble;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.seamoon.blellib.LockManager;
import com.seamoon.blellib.bluetooth.ClientManager;
import com.seamoon.blellib.bluetooth.IConnectCallBack;
import com.seamoon.blellib.bluetooth.ISearchCallBack;
import com.seamoon.blellib.bluetooth.IWriteCallBack;
import com.seamoon.blellib.bluetooth.OadLockerProtocol;
import com.seamoon.blellib.datafactory.LockerAdvertising;
import com.seamoon.blellib.datafactory.LockerModel;
import com.seamoon.blellib.datafactory.Methods;
import com.seamoon.blellib.datafactory.PowerOffRecord;
import com.seamoon.blellib.datafactory.ResultMonitor;
import com.seamoon.blellib.sqlite.DBHelper;

import com.seamoon.codelib.MainApi;
import com.seamoon.codelib.bluetoothKeyUtils.BaseUtils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import android.os.Bundle;
import android.os.Looper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import org.apache.cordova.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class CustomBLE extends CordovaPlugin {

    private static LockerModel lockerModel;
    private static LockManager lockManager;
    private String lockSn;
    // private String snInfo = "6RLxgWwJKJkCKEFa6brXrB9lkaAHvrPJi0vLokBgd2aCu/0RCXaTDkui4ASPJGbc/wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//Ndazkjr98mbDEwZzQoxrEhzyNislT+jE81eEsD4t2PYqoE3ll1DWmfaJ/gcd98cArwp++6XTAcupECUJbt9NgQ4qZlVxTNm7OgQ8TBSIt71KxQfNH0iMUHJ0KRMwBVXlysgpW7Tzt+sl3GoX3zF6n/BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR//8FWVldClKlHSU9aKvRUf//BVlZXQpSpR0lPWir0VH//wVZWV0KUqUdJT1oq9FR/+i/cVq1OcLiYGSr/pfPSVQ=9f36b80de3a1c8d3472f351c3202d7a6";
    private String snInfo;
    private String key;
    private String newKey;
    private final static String TAG = "LockLib";
    private OadLockerProtocol oadLockerProtocol;
    boolean isFounded = false;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private SQLiteDatabase db;
    private boolean mConnected;
    private LockerAdvertising adv;

    public static CustomBLE instance = null;
    public static CordovaWebView cordovaWebView;
    public static CordovaInterface cordovaInterface;
    public static Activity mainActivity;

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
            String clickOn = args.getJSONObject(0).getString("clickOn");
            this.onClick(clickOn, callbackContext);
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

    private  void getAdvertisingInfo() {
        // Log.i(TAG, "Send GetAdvCode ----------------");
        lockManager.sendDirective(Methods.GetAdvCode, writeCallBack);
    }





    public void onClick(String clickOn, CallbackContext callbackContext) {
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
                // search(snInfo, callbackContext);
                // callbackContext.success(snInfo+ "connect called");
                callbackContext.success("connect called"+ mainActivity);
                break;
            }
            case "unlock": {
                // Log.i(TAG,"KEY:"+key);
                String openStr = MainApi.lockOperation(adv.getData(), snInfo, 1,key);
                if("500".equals(openStr)){
                    callbackContext.error(mainActivity+"Key error!");
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
            // case "setlocaltime": {
            //     Intent intent =new Intent(mainActivity+ SynchronizeActivity.class);
            //     startActivityForResult(intent,666);
            //     break;
            // }
            // case R.id.record: {
            //     String recordsStr = MainApi.getLockRecords(adv.getData(), snInfo,key);
            //     if("500".equals(recordsStr)){
            //         XToastUtil.showToast(MainActivity.this,"Key error!");
            //         break;
            //     }
            //     Log.i(TAG, "recordsString -> " + recordsStr);
            //     LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
            //     startTimer1(20000);
            //     missDialog = true;
            //     sendCode(recordsStr);
            //     break;
            // }
            // case R.id.config: {
            //     Intent intent =new Intent(MainActivity.this, ConfigActivity.class);
            //     intent.putExtra("lockType",MainApi.getLockTypeFromSnInfo(snInfo));
            //     startActivityForResult(intent,555);
            //     break;
            // }
            // case R.id.upgrade: {
            //     stopTimer();
            //     if (lockManager != null){
            //         lockManager.destory();
            //     }
            //     Intent intent =new Intent(MainActivity.this, UpgradeActivity.class);
            //     String snInfoNow = snInfo.substring(0, snInfo.length() - 32);
            //     Map<String, Object> snInfoMap = BaseUtils.dispose(snInfoNow);
            //     intent.putExtra("lockSn",lockSn);
            //     intent.putExtra("secretKey",snInfoMap.get("secretKey").toString());
            //     intent.putExtra("snInfo",snInfo);
            //     intent.putExtra("key",key);
            //     startActivityForResult(intent,222);
            //     break;
            // }
            // case R.id.setKey:{
            //     Intent intent =new Intent(MainActivity.this, SetKeyActivity.class);
            //     startActivityForResult(intent,333);
            //     break;
            // }
            // case R.id.gettime:{
            //     String gettimestr = MainApi.getLockTime(adv.getData(), snInfo,key);
            //     if("500".equals(gettimestr)){
            //         XToastUtil.showToast(MainActivity.this,"Key error!");
            //         break;
            //     }
            //     Log.i(TAG, "gettimestr -> " + gettimestr);
            //     LoadingStaticDialog.showLoadingDialog(MainActivity.this, "Operating");
            //     startTimer1(10000);
            //     missDialog = true;
            //     sendCode(gettimestr);
            //     break;
            // }
            case "disconnect":{
                // stopTimer();
                if (lockManager != null){
                    lockManager.destory();
                }
                mConnected = false;
                isFounded = false;
                mainActivity.finish();
                break;
            }
        }
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
                    // Intent intent = new Intent(mainActivity, RecordActivity.class);
                    // intent.putExtra("records",(Serializable) list);
                    // intent.putExtra("snInfo",snInfo);
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



}
