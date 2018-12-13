package com.runvision.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * APP异常日志服务
 */
public class MyLogcatService extends Service {
    private static final String TAG = "MyLogcatService";
    Thread thread;
    boolean readlog = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        thread = new Thread(() -> {
            log2();//个人觉得这个方法更实用
        });
    }

    @Override
    public void onStart(Intent intent, int startId) {
        thread.start();
        Log.d(TAG, "onStart");
        super.onStart(intent, startId);
    }

    /**
     * 方法1
     */
    private void log2() {
        Log.d(TAG, "log2 start");
        String[] cmds = { "logcat", "-c" };
        String shellCmd = "logcat -v time -s *:W "; // adb logcat -v time *:W
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        try {
            runtime.exec(cmds).waitFor();
            process = runtime.exec(shellCmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains(String.valueOf(android.os.Process.myPid()))) {
                    // line = new String(line.getBytes("iso-8859-1"), "utf-8");
                    writeTofile(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "log2 finished");
    }

    /**
     * 方法2
     */
    private void log() {
        Log.d(TAG, "log start");
        String[] cmds = { "logcat", "-c" };
        String shellCmd = "logcat -v time -s *:W ";// //adb logcat -v time *:W
        Process process = null;
        InputStream is = null;
        DataInputStream dis = null;
        String line = "";
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(cmds);
            process = runtime.exec(shellCmd);
            is = process.getInputStream();
            dis = new DataInputStream(is);
            // String filter = GetPid();
            String filter = android.os.Process.myPid() + "";
            while ((line = dis.readLine()) != null) { //这里如果输入流没断，会一直循环下去。
                line = new String(line.getBytes("iso-8859-1"), "utf-8");
                if (line.contains(filter)) {
                    int pos = line.indexOf(":");
                    Log.d(TAG, line + "");
                    writeTofile(line);
                }
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "log finished");
    }

    private void writeTofile(String line) {
        String content = line + "\r\n";
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/logcat/RunLog.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }
}
