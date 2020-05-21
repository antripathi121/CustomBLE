package cordova.plugin.custom.ble;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.seamoon.blellib.sqlite.DBHelper;
import com.seamoon.blellib.datafactory.LockerModel;
import com.seamoon.codelib.MainApi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.seamoon.blellib.LockManager;

import android.os.Bundle;
import android.database.Cursor;


/**
 * This class echoes a string called from JavaScript.
 */
public class CustomBLE extends CordovaPlugin {

    private static LockerModel lockerModel;
    private static LockManager lockManager;
    private SQLiteDatabase db;
    private String lockSn;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    
        if (action.equals("search")){
            String snInfo = args.getString(0);
            this.search(snInfo, callbackContext);
            return true;
        }
        return false;
    }

    private void search(String snInfo, CallbackContext callbackContext){
        lockManager = null;
        DBHelper contactDBHelper = new DBHelper(this);
        db = contactDBHelper.getWritableDatabase();
        if (snInfo.length() > 0) {
            lockSn = MainApi.getSnFromSnInfo(snInfo);
            Log.i("lockSn","lockSn:"+lockSn);
            callbackContext.success("lockSN:" + lockSn);
            if(lockSn == null){
                callbackContext.error(this, "snInfo error!");
                // return;
            }
            // Cursor cursor = db.rawQuery("select lockModel,lockSn from sdk_lockmodel where lockSn = ?", new String[]{lockSn});
            // if(cursor.moveToNext()){
            //     byte data[] = cursor.getBlob(cursor.getColumnIndex("lockModel"));
            //     ByteArrayInputStream arrayInputStream = null;
            //     ObjectInputStream inputStream = null;
            //     try {
            //         arrayInputStream = new ByteArrayInputStream(data);
            //         inputStream = new ObjectInputStream(arrayInputStream);
            //         lockerModel = (LockerModel) inputStream.readObject();
            //     } catch (Exception e) {
            //         e.printStackTrace();
            //         callbackContext.error("err"+e);
            //     }finally {
            //         if(inputStream != null){
            //             try {
            //                 inputStream.close();
            //             } catch (IOException e) {
            //                 e.printStackTrace();
            //             }
            //         }
            //         if(arrayInputStream != null){
            //             try {
            //                 arrayInputStream.close();
            //             } catch (IOException e) {
            //                 e.printStackTrace();
            //             }
            //         }
            //     }
            //     lockManager = new LockManager(CustomBLE.this, 60, true, lockSn,connectCallBack);
            //     lockManager.setLockerModel(lockerModel);
            //     lockManager.setSQLiteDatabase(db);
            //     lockManager.initBluetooth();
            //     lockManager.connectDevice(connectCallBack);
            //     cursor.close();
            //     return;
            // }
            // cursor.close();
            // new Thread(new Runnable() {
            //     public void run() {
            //         Looper.prepare();
            //         lockManager = new LockManager(MainActivity.this, 60, true, lockSn,connectCallBack);
            //         lockManager.setSQLiteDatabase(db);
            //         lockManager.startScan(new ISearchCallBack() {
            //             @Override
            //             public void updateDevicesList(List<LockerModel> lockerModels) {
            //                 if (lockerModels.size() > 0) {
            //                     lockerModel = lockerModels.get(0);
            //                     if (!isFounded) {
            //                         isFounded = true;
            //                         lockManager.connectDevice(connectCallBack);
            //                     }
            //                 }
            //             }
            //             @Override
            //             public void onSearchStopped() {
            //                 Log.i(TAG, "onSearchStopped");
            //                 if (!isFounded){

            //                 }
            //             }
            //         });
            //     }
            // }).start();
        } else {
            callbackContext.error("Sninfo cannot be empty!");
        }
    }



}
