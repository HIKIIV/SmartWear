package com.huawei.audiodevicekit.bluetoothsample.view;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.audiobluetooth.api.Cmd;
import com.huawei.audiobluetooth.api.data.SensorData;
import com.huawei.audiobluetooth.layer.protocol.mbb.DeviceInfo;
import com.huawei.audiobluetooth.utils.DateUtils;
import com.huawei.audiobluetooth.utils.LocaleUtils;
import com.huawei.audiobluetooth.utils.LogUtils;
import com.huawei.audiodevicekit.R;
import com.huawei.audiodevicekit.bluetoothsample.contract.SampleBtContract;
import com.huawei.audiodevicekit.bluetoothsample.presenter.SampleBtPresenter;
import com.huawei.audiodevicekit.bluetoothsample.view.adapter.SingleChoiceAdapter;
import com.huawei.audiodevicekit.mvp.view.support.BaseAppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;

public class SampleBtActivity
    extends BaseAppCompatActivity<SampleBtContract.Presenter, SampleBtContract.View>
    implements SampleBtContract.View {
    private static final String TAG = "SampleBtActivity";

    private TextView tvDevice;

    private TextView tvStatus;

    private ListView listView;

    private TextView tvSendCmdResult;

    private Button btnSearch;

    private Button btn1;

    private Button btn2;

    private Spinner spinner;

    private Button btnSendCmd;

    private RecyclerView rvFoundDevice;

    private SingleChoiceAdapter mAdapter;

    private Cmd mATCmd = Cmd.VERSION;

    private String mMac;

    private List<Map<String, String>> maps;

    private SimpleAdapter simpleAdapter;

    private TextView tvDataCount;

    private TextView tips;

    private StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
            tips.setText("开始录音");
        }

        @Override
        public void onRecordData(short[] data, int length) {
            Log.d("MainActivity", "current buffer size is " + length);
        }

        @Override
        public void onVoiceVolume(int volume) {
            Log.d("MainActivity", "current volume is " + volume);
        }

        @Override
        public void onRecordError(int code, String errorMsg) {
            tips.setText("录音错误" + errorMsg);
        }

        @Override
        public void onFileSaveFailed(String error) {
            Toast.makeText(getApplicationContext(), "文件保存失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveSuccess(String fileUri) {
            Toast.makeText(getApplicationContext(), "文件保存成功,路径是" + fileUri, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecording() {
            tips.setText("录音结束");
        }
    };

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public SampleBtContract.Presenter createPresenter() {
        return new SampleBtPresenter();
    }

    @Override
    public SampleBtContract.View getUiImplement() {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        tvDevice = findViewById(R.id.tv_device);
        tvStatus = findViewById(R.id.tv_status);
        btn1 = findViewById(R.id.button1);
        btn2 = findViewById(R.id.button2);
        tips = findViewById(R.id.recordStatus);
        tvDataCount = findViewById(R.id.tv_data_count);
        listView = findViewById(R.id.listview);
        tvSendCmdResult = findViewById(R.id.tv_send_cmd_result);
        btnSearch = findViewById(R.id.btn_search);
        spinner = findViewById(R.id.spinner);
        btnSendCmd = findViewById(R.id.btn_send_cmd);
        rvFoundDevice = findViewById(R.id.found_device);
        initSpinner();
        initRecyclerView();
        maps = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(this, maps, android.R.layout.simple_list_item_1,
            new String[] {"data"}, new int[] {android.R.id.text1});
        listView.setAdapter(simpleAdapter);

        locationDataInit();

        IdealRecorder.getInstance().init(this);
        btn1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                IdealRecorder idealRecorder = IdealRecorder.getInstance();

                idealRecorder.setRecordFilePath(getSaveFilePath());
                //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
                IdealRecorder.RecordConfig recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(20000).setVolumeInterval(200);
                //设置录音配置 最长录音时长 以及音量回调的时间间隔
                idealRecorder.setStatusListener(statusListener);
                idealRecorder.start();
                //开始录音
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "fin", Toast.LENGTH_SHORT).show();
                IdealRecorder idealRecorder = IdealRecorder.getInstance();
                idealRecorder.stop();
                //停止录音
            }
        });
    }

    private void initSpinner() {
        List<Map<String, String>> data = new ArrayList<>();
        for (Cmd cmd : Cmd.values()) {
            if (cmd.isEnable()) {
                HashMap<String, String> map = new HashMap<>();
                Boolean isChinese = LocaleUtils.isChinese(this);
                String name = isChinese ? cmd.getNameCN() : cmd.getName();
                map.put("title", cmd.getType() + "-" + name);
                data.add(map);
            }
        }
        spinner.setAdapter(
            new SimpleAdapter(this, data, R.layout.item_spinner, new String[] {"title"},
                new int[] {R.id.tv_name}));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LogUtils.i(TAG, "onItemSelected position = " + position);
                String title = data.get(position).get("title");
                String type = Objects.requireNonNull(title).split("-")[0];
                try {
                    int typeValue = Integer.parseInt(type);
                    mATCmd = Cmd.getATCmdByType(typeValue);
                } catch (NumberFormatException e) {
                    LogUtils.e(TAG, "parseInt fail e = " + e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                LogUtils.i(TAG, "onNothingSelected parent = " + parent);
            }
        });
    }

    private void initRecyclerView() {
        SingleChoiceAdapter.SaveOptionListener mOptionListener = new SingleChoiceAdapter.SaveOptionListener() {
            @Override
            public void saveOption(String optionText, int pos) {
                LogUtils.i(TAG, "saveOption optionText = " + optionText + ",pos = " + pos);
                mMac = optionText.substring(1, 18);
                boolean connected = getPresenter().isConnected(mMac);
                if (connected) {
                    getPresenter().disConnect(mMac);
                } else {
                    getPresenter().connect(mMac);
                }
            }

            @Override
            public void longClickOption(String optionText, int pos) {
                LogUtils.i(TAG, "longClickOption optionText = " + optionText + ",pos = " + pos);
            }
        };
        mAdapter = new SingleChoiceAdapter(this, new ArrayList<>());
        mAdapter.setSaveOptionListener(mOptionListener);
        rvFoundDevice.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        rvFoundDevice.setAdapter(mAdapter);
    }

    public String getSaveFilePath(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 222);
        File file = new File(Environment.getExternalStorageDirectory(), "EyeWear");
        if (!file.exists()) {
            file.mkdirs();
        }
        File wavFile = new File(file, "ideal.wav");
        if(wavFile.exists()) {
            wavFile.delete();
        }

        try {
            wavFile.createNewFile();
            Toast.makeText(getApplicationContext(), wavFile.toString()+"Create successful", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), wavFile.toString() + "创建文件失败", Toast.LENGTH_SHORT).show();
        }
        return wavFile.getAbsolutePath();
    }

    // location utilities
    private void locationDataInit() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                TextView loctext1 = findViewById(R.id.textLoc1);
                TextView loctext2 = findViewById(R.id.textLoc2);
                loctext1.setText(String.valueOf(longitude));
                loctext2.setText(String.valueOf(latitude));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                //状态发生改变监听
            }

            @Override
            public void onProviderEnabled(String s) {
                // ProviderEnabled
            }

            @Override
            public void onProviderDisabled(String s) {
                // ProviderDisabled
            }
        });
    }

    @Override
    protected void initData() {
        getPresenter().initBluetooth(this);
    }

    @Override
    protected void setOnclick() {
        super.setOnclick();
        btnSendCmd.setOnClickListener(v -> getPresenter().sendCmd(mMac, mATCmd.getType()));
        btnSearch.setOnClickListener(v -> getPresenter().checkLocationPermission(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        getPresenter().processLocationPermissionsResult(requestCode, grantResults);
    }

    @Override
    public void onDeviceFound(DeviceInfo info) {
        if (mAdapter == null) {
            return;
        }
        runOnUiThread(() -> mAdapter
            .pushData(String.format("[%s] %s", info.getDeviceBtMac(), "HUAWEI Eyewear")));
    }

    @Override
    public void onStartSearch() {
        if (mAdapter != null) {
            runOnUiThread(() -> mAdapter.clearData());
        }
    }

    @Override
    public void onDeviceChanged(BluetoothDevice device) {
        if (tvDevice != null) {
            runOnUiThread(() -> tvDevice
                .setText(String.format("[%s] %s", device.getAddress(), "HUAWEI Eyewear")));
        }
    }

    @Override
    public void onConnectStateChanged(String stateInfo) {
        if (tvStatus != null) {
            runOnUiThread(() -> tvStatus.setText(stateInfo));
        }
    }

    @Override
    public void onSensorDataChanged(SensorData sensorData) {
//        runOnUiThread(() -> {
//            Map<String, String> map = new HashMap<>();
//            map.put("data", sensorData.toString());
//            maps.add(0, map);
//            tvDataCount.setText(getString(R.string.sensor_data, maps.size()));
//            simpleAdapter.notifyDataSetChanged();
//        });


    }

    @Override
    public void onSendCmdSuccess(Object result) {
        runOnUiThread(() -> {
            String info = DateUtils.getCurrentDate() + "\n" + result.toString();
            tvSendCmdResult.setText(info);
        });
    }

    @Override
    public void onError(String errorMsg) {
        runOnUiThread(
            () -> Toast.makeText(SampleBtActivity.this, errorMsg, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPresenter().deInit();
    }
}
