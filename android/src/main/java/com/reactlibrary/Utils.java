package com.reactlibrary;

import static android.content.Context.AUDIO_SERVICE;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;

public class Utils {

    private static HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();
    private static SoundPool soundPool;
    private static float volumnRatio;
    private static AudioManager am;

    public static boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }
    public static boolean isDeniedPermission(Context context, String permission) {
        return shouldAskPermission() && checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGrantedAllPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (isDeniedPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isGrantedAllPermissions(Context context,int[] grantResults) {
        int countGranted = 0;
        for (int granted: grantResults) {
            if (granted == PackageManager.PERMISSION_GRANTED) {
                countGranted ++;
            }
        }
        return countGranted == grantResults.length;
    }


    public static boolean checkPermissions(ReactApplicationContext context, String[] permissions, int requestCode) {

        if (isGrantedAllPermissions(context, permissions)) {
            return true;
        } else {
            requestPermissions( context.getCurrentActivity() , permissions, requestCode);
            return false;
        }
    }

    public static void openAppSetting(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void showAskPermissionSetting(final Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Permission Request")
                .setMessage("Allow app to use certain requested permissions")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openAppSetting(context);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.create();
        alertDialogBuilder.show();
    }

    public static WritableMap convertParams(String type, WritableMap data){
        data.putString(Constants.BLUETOOTH_EVENT_TYPE, type);
        return data;
    }

    public static boolean isMatchWithPrefix(String[] listPrefix, String deviceName){
        if(listPrefix.length ==0)
            return true;

        boolean isMatch = false;
        for(int i = 0; i < listPrefix.length;i++){
            if(deviceName.startsWith(listPrefix[i])){
                isMatch = true;
                break;
            }
        }
        return isMatch;
    }

    public static void initSound(Context context) {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
        soundMap.put(1, soundPool.load(context, R.raw.barcodebeep, 1));
        soundMap.put(2, soundPool.load(context, R.raw.serror, 1));
        am = (AudioManager) context.getSystemService(AUDIO_SERVICE);// 实例化AudioManager对象
    }

    public static void freeSound() {
        if (soundPool != null)
            soundPool.release();
        soundPool = null;
    }

    public static void playSound(int id) {

        float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 返回当前AudioManager对象的最大音量值
        float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);// 返回当前AudioManager对象的音量值
        volumnRatio = audioCurrentVolumn / audioMaxVolumn;
        try {
            soundPool.play(soundMap.get(id), volumnRatio, // 左声道音量
                    volumnRatio, // 右声道音量
                    1, // 优先级，0为最低
                    0, // 循环次数，0无不循环，-1无永远循环
                    1 // 回放速度 ，该值在0.5-2.0之间，1为正常速度
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void alert(Activity act, String titleInt, String message, DialogInterface.OnClickListener positiveListener) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            builder.setTitle(titleInt);
            builder.setMessage(message);

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            if (positiveListener != null) {
                builder.setPositiveButton("OKAY", positiveListener);
            }
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void alert(Activity act, String title, View view, int iconInt, DialogInterface.OnClickListener positiveListener) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            builder.setTitle(title);
            builder.setView(view);
            builder.setIcon(iconInt);

            builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            if (positiveListener != null) {
                builder.setPositiveButton("OKAY", positiveListener);
            }
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static int toInt(String str, int defValue) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defValue;
    }
}