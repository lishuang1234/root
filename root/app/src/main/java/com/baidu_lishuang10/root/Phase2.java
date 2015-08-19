package com.baidu_lishuang10.root;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import androidterm.Exec;

/**
 * Created by baidu_lishuang10 on 15/8/4.
 */
public class Phase2 extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextView mInforTx;
    private int MODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2);
        SharedPreferences setting = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        MODE = setting.getInt(MainActivity.MODE, 0);
        new Thread() {
            @Override
            public void run() {
                if (MODE == MainActivity.MODE_ROOT)
                    root();
                else if (MODE == MainActivity.MODE_UNROOT)
                    unroot();
            }
        }.start();
        initToolbar();
        initView();
    }

    private void root() {
        showInfor("开始root。");
        final int[] processId = new int[1];
        final FileDescriptor fd = Exec.createSubprocess("/system/bin/sh", "-", null, processId);
        final FileOutputStream out = new FileOutputStream(fd);
        final FileInputStream inp = new FileInputStream(fd);
        //临时存储文件
        try {
            SaveIncludedZippedFileIntoFilesFolder(R.raw.busybox, "busybox", getApplicationContext());
            SaveIncludedZippedFileIntoFilesFolder(R.raw.su, "su", getApplicationContext());
            SaveIncludedZippedFileIntoFilesFolder(R.raw.superuser, "superuser.apk", getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread() {
            public void run() {
                byte[] mBuffer = new byte[4096];
                int read = 0;
                while (read >= 0) {
                    try {
                        read = inp.read(mBuffer);
                        String str = new String(mBuffer, 0, read);


                    } catch (Exception ex) {

                    }
                }
            }
        }.start();

        try {
            String command = "id\n";///??
            out.write(command.getBytes());
            out.flush();
            command = "chmod 777 " + getFilesDir() + "/busybox\n";
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/busybox mount -o remount,rw /system\n";//重新挂载为可读写
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/busybox cp " + getFilesDir() + "/su /system/bin/\n";//复制su
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/busybox cp " + getFilesDir() + "/SuperUser.apk /system/app\n";//复制apk
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/busybox cp " + getFilesDir() + "/busybox /system/bin/\n";//复制busybox
            out.write(command.getBytes());
            out.flush();
            command = "chown root.root /system/bin/busybox\nchmod 755 /system/bin/busybox\n";//更改busybox拥有属性
            out.write(command.getBytes());
            out.flush();
            command = "chown root.root /system/bin/su\n";//更改su属性
            out.write(command.getBytes());
            out.flush();
            command = getFilesDir() + "/busybox chmod 6755 /system/bin/su\n";
            out.write(command.getBytes());
            out.flush();
            command = "chown root.root /system/app/SuperUser.apk\nchmod 755 /system/app/SuperUser.apk\n";
            out.write(command.getBytes());
            out.flush();
            //删除临时root文件
            command = "rm " + getFilesDir() + "/busybox\n";
            out.write(command.getBytes());
            out.flush();
            command = "rm " + getFilesDir() + "/su\n";
            out.write(command.getBytes());
            out.flush();
            command = "rm " + getFilesDir() + "/SuperUser.apk\n";
            out.write(command.getBytes());
            out.flush();
            command = "rm " + getFilesDir() + "/rageagainstthecage\n";
            out.write(command.getBytes());
            out.flush();
            command = "echo \"reboot now!\"\n";//重启
            showInfor("手机重启。");
            out.write(command.getBytes());
            out.flush();
            Thread.sleep(3000);
            command = "sync\nsync\n";
            out.write(command.getBytes());
            out.flush();
            command = "reboot\n";
            out.write(command.getBytes());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void SaveIncludedZippedFileIntoFilesFolder(int id, String name, Context context) throws IOException {
        InputStream inp = context.getResources().openRawResource(id);
        FileOutputStream fos = context.openFileOutput(name, MODE_WORLD_READABLE);
        GZIPInputStream gis = new GZIPInputStream(inp);
        byte[] temp = new byte[1024];
        int length;
        while ((length = gis.read(temp)) >= 0) {
            fos.write(temp, 0, length);
        }
        gis.close();
        fos.getChannel().force(true);
        fos.flush();
        fos.close();

    }

    private void unroot() {


    }

    private void initView() {
        mInforTx = (TextView) findViewById(R.id.activity_p2_tx);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.activity_p2_tb);
        setSupportActionBar(mToolbar);
        setTitle("Rooting");
    }

    public void showInfor(final String infor) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInforTx.setText(infor);
            }
        });
    }
}
