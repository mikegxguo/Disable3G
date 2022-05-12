package com.mitac.at;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.SystemProperties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

//import java.io.File;
//import java.io.IOException;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.FileReader;
//import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


public class xxxService extends Service {
    private static final String TAG = "3GService";
    public static final String EXTRA_EVENT = "event";
    public static final String EVENT_BOOT_COMPLETED = "BOOT_COMPLETED";
    private static boolean mHandled = false;
//    public static final String EXTRA_EVENT_APN = "apn";
//    public static final String EXTRA_EVENT_MODEM = "modem";
    private ModemTool mTool;


//    BroadcastReceiver mMitacAPIReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            //Bundle bundle = intent.getExtras();
//            switch (action) {
//                case Intent.ACTION_BOOT_COMPLETED:
//                    Log.d(TAG,"ACTION_BOOT_COMPLETED");
//                    String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
//                    String project = SystemProperties.get("ro.product.name");
//                    if(sc600_sku.contains("NA") && project.contains("chiron_pro")) {
//                        mTool = new ModemTool();
//                        Disable3G();
//                    }
//                    break;
//            }
//        }
//    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

//        IntentFilter mitacAPIFilter = new IntentFilter();
//        mitacAPIFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
//        registerReceiver(mMitacAPIReceiver, mitacAPIFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        String apn = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_APN);
//        if(apn != null) {
//            Log.d(TAG, "onStartCommand : apn = " + apn);
//        }
//        String modem = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_MODEM);
//        if(modem != null) {
//            Log.d(TAG, "onStartCommand :  modem = " + modem);
//        }
//        if(apn!=null && APNUtil.ValidateAPN(apn)){
//            APNUtil.customizeAPN(3GService.this);
//        } else if(modem!=null && modem.equals("get_ver")) {
//            String adsp_ver = getAdspVer();
//            Log.d(TAG, "ADSP version: " + adsp_ver);
//            String baseband = SystemProperties.get("persist.radio.version.baseband");
//            String[] temp = baseband.split(",");
//            SystemProperties.set("persist.sys.fw.version", temp[0]+","+adsp_ver);
//        }
        String event = intent == null ? "" : intent.getStringExtra(EXTRA_EVENT);
        Log.d(TAG, "onStartCommand : event = " + event);
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        String project = SystemProperties.get("ro.product.name");
        if(sc600_sku.contains("NA") && project.contains("chiron_pro")) {
            mTool = new ModemTool();
            Disable3G();
        }
        if (EVENT_BOOT_COMPLETED.equals(event)) {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

//    private String getAdspVer(){
//        File select_image = new File("/sys/devices/soc0/select_image");
//        final String filename = "/sys/devices/soc0/image_crm_version";
//        BufferedWriter bw = null;
//        FileReader reader = null;
//        String adsp_ver = "";
//        try {
//            bw = new BufferedWriter(new FileWriter(select_image));
//            bw.write("12");
//            bw.flush();
//            bw.close();
//
//            reader = new FileReader(filename);
//            char[] buf = new char[32];//N664-R00A00-000000_20210520
//            int n = reader.read(buf);
//            if (n > 1) {
//                adsp_ver = String.valueOf(buf,0,n-1);
//            }
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return adsp_ver;
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(mMitacAPIReceiver);
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private String sendGetAT(String atCommand, String prefix) {
        String content = null;
        BufferedReader br = null;
        try {
            //ATInterface atInterface = getATInterface();
            //String result = atInterface.sendAT(atCommand);
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, atCommand);
            //Log.d(TAG, "sendGetAT : atCommand=" + atCommand + ", prefix=" + prefix + ", result=" + result);
            if(result != null && result.contains("OK")) {
                br = new BufferedReader(new StringReader(result));
                String line;
                while((line = br.readLine()) != null) {
                    if(line.contains(prefix)) {
                        content = line.substring(prefix.length());
                        //content = content.replace("\"", "");
                        break;
                    }
                }
            } else if(result != null && result.contains("ERROR")) {
                content = "ERROR";
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return content;
    }

    private boolean sendAT(String cmd) {
        boolean res = false;
        try {
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            //Log.d(TAG, "sendAT : cmd = " + cmd + ", result = " + result);
            if (result != null && result.contains("OK")) {
                res = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return res;
    }

    //SC600Y, NA, disable 3G due to its royalty
    //Disable 3G: AT+QNVW=4548,0,"0000000000000000"
    //Restore 3G: AT+QNVW=4548,0,"0000800600000000"

    public boolean Disable3G() {
        String prefix = "+QNVR: \"";
        String val = null;
        String NV_4548_R = "AT+QNVR=4548,0";
        String NV_4548_W = "AT+QNVW=4548,0,\"0000000000000000\"";
        String RESET_MODEM = "AT+QCFG=\"reset\"";

        Log.d(TAG, "Checking whether 3G is disabled");
        //check 4548
        val = sendGetAT(NV_4548_R, prefix);
        if(val.contains("0000000000000000")) {
            Log.d(TAG, "NV item 4548 is changed");
            mHandled = true;
        } else {
            Log.d(TAG, "Changing NV item 4548");
            mHandled = false;
            sendAT(NV_4548_W);
        }
        //reset modem
        if(!mHandled) {
            sendAT(RESET_MODEM);
            Log.d(TAG, "Reset modem ......");
        }
        Log.d(TAG, "the process is finished");
        return true;
    }

}
