package com.baidu_lishuang10.root;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baidu_lishuang10.util.VirtualTerminal;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar mToolbar;
    private Button mRootBtn;
    private Button mUnRootBtn;
    private TextView mDetailTv;
    public static final String PREFS_NAME = "root";
    public static final String MODE = "mode";
    public static final int MODE_ROOT = 0;
    public static final int MODE_UNROOT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();
        initView();
        new Thread() {
            @Override
            public void run() {
                checkRoot();
            }
        }.start();
    }

    public void showLog(String infor) {
        Log.e("Main", infor);
    }

    /**
     * 判断设备是否已root
     */
    private void checkRoot() {
        try {
            VirtualTerminal virtualTerminal = new VirtualTerminal();
            VirtualTerminal.VTCommandResult vtCommandResult = virtualTerminal.runCommand("id");
            if (vtCommandResult.success()) {//root
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRootBtn.setVisibility(View.GONE);
                        mDetailTv.setText("设备已经获取root。");
                    }
                });
            }
            virtualTerminal.shutdown();
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUnRootBtn.setVisibility(View.GONE);
                    mDetailTv.setText("设备未获取root。");
                }
            });
            e.printStackTrace();
        }
        if (new File("/system/bin/su").exists() || new File("/system/xbin/su").exists())
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDetailTv.setText(mDetailTv.getText() + "su文件已存在。");
                }
            });


    }

    private void initView() {
        mRootBtn = (Button) findViewById(R.id.activity_main_btn_root);
        mUnRootBtn = (Button) findViewById(R.id.activity_main_btn_unroot);
        mRootBtn.setOnClickListener(this);
        mUnRootBtn.setOnClickListener(this);
        mDetailTv = (TextView) findViewById(R.id.activity_main_tx);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.activity_main_tb);
        setSupportActionBar(mToolbar);
        setTitle("Root");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_main_btn_root:
                root();
                break;
            case R.id.activity_main_btn_unroot:
                unroot();
                break;
        }
    }

    private void unroot() {
        SharedPreferences setting = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putInt(MODE, MODE_UNROOT);
        editor.commit();
        startActivity(new Intent(this, Phase1.class));
        finish();
    }

    private void root() {
        SharedPreferences setting = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();
        editor.putInt(MODE, MODE_ROOT);
        editor.commit();
        startActivity(new Intent(this, Phase1.class));
        finish();
    }
}
