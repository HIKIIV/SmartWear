package com.huawei.audiodevicekit.bluetoothsample.view;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;

public class SampleBtActivity
    extends BaseAppCompatActivity<SampleBtContract.Presenter, SampleBtContract.View>
    implements SampleBtContract.View {
    private static final String TAG = "SampleBtActivity";

    private TextView tvDevice;

    private TextView tvStatus;

    private ListView listView;

    private TextView tvSendCmdResult;

    private Button btnSearch;

    private Button btnConnect;

    private Button btnDisconnect;

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

    //  media player, for play the record
    private MediaPlayer mediaPlayer = new MediaPlayer();    // 怀疑是否需要改成NULL

    //  play, pause and stop for media player
    private Button btnplay;
    private Button btnpause;
    private Button btnstop;
    private boolean isMediaPlayerRelease = true;

    private double loc_longitude = 0;   // 持久化在内存中记录的经纬度
    private double loc_latitude = 0;    // 持久化在内存中记录的经纬度

    private double last_loc_longitude = 0;  // 上次播放音频时的经纬度
    private double last_loc_latitude = 0;   // 上次播放音频时的经纬度
    private int last_serial_num = -1;       // 上次播放音频时播放音频的序号

    private int last_begin = 0;             // 上次播放音频的开始检测时间（60 * hour + minute）
    private int last_end = 0;               // 上次播放音频的结束检测时间（60 * hour + minute）
    private int last_time_num = -1;         // 上次按照时间播放的音频序号

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
        tvDataCount = findViewById(R.id.tv_data_count);
        listView = findViewById(R.id.listview);
        tvSendCmdResult = findViewById(R.id.tv_send_cmd_result);
        btnSearch = findViewById(R.id.btn_search);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        Button btn1 = findViewById(R.id.btx1);
        Button btn2 = findViewById(R.id.btx2);
        tips = findViewById(R.id.RecordStatus);
        spinner = findViewById(R.id.spinner);
        btnSendCmd = findViewById(R.id.btn_send_cmd);
        rvFoundDevice = findViewById(R.id.found_device);

        //  play, pause and stop buttons
        btnplay = findViewById(R.id.Play);
        btnpause = findViewById(R.id.Pause);
        btnstop = findViewById(R.id.Stop);

        Button start = findViewById(R.id.start); // starting the main program (entering header)
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), header.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent);
            }
        });

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
                idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(300000).setVolumeInterval(200);
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

        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
                    if (isMediaPlayerRelease) {
                        initMediaPlayer("ideal.wav");
                        isMediaPlayerRelease = false;
                    }
                    mediaPlayer.start();
                    btnplay.setEnabled(false);
                    btnpause.setEnabled(true);
                    btnstop.setEnabled(true);
                }
            }
        });

        btnpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                btnplay.setEnabled(true);
                btnpause.setEnabled(false);
                btnstop.setEnabled(false);
            }
        });

        btnstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    isMediaPlayerRelease = true;
                }
                btnplay.setEnabled(true);
                btnpause.setEnabled(false);
                btnstop.setEnabled(false);
            }
        });
    }

    private void initMediaPlayer(String dataName) {    // 加一个作为参数的文件名
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        } else {
            mediaPlayer = new MediaPlayer();
        }
        File file = new File(this.getExternalFilesDir("").getAbsolutePath(),
                dataName);
        try {
            tips.setText("initMediaPlayer: " + file.getPath());
            mediaPlayer.reset();
            mediaPlayer.setDataSource(file.getPath());          //  path to mp3
            tips.setText("initMediaPlayer: set DataSource failed");
            mediaPlayer.prepare();                              //  let MediaPlayer prepare
            tips.setText("initMediaPlayer: out");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static String doubleToString(double num){
        //使用0.000000不足位补0，#.##仅保留有效位
        return new DecimalFormat("0.0000").format(num);
    }

    public String getSaveFilePath(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 222);
        File file = new File(this.getExternalFilesDir("").getAbsolutePath(), "");
        if (!file.exists()) {
            file.mkdirs();
        }
        // 文件的前后加入时间、经纬度数据
        Calendar calendars = Calendar.getInstance();

        calendars.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));

        String year = String.valueOf(calendars.get(Calendar.YEAR));
        String month = String.valueOf(calendars.get(Calendar.MONTH));
        String day = String.valueOf(calendars.get(Calendar.DATE));
        String hour = String.valueOf(calendars.get(Calendar.HOUR));
        String min = String.valueOf(calendars.get(Calendar.MINUTE));
        String second = String.valueOf(calendars.get(Calendar.SECOND));

        // 文件前加入经度
        // 文件后加入纬度
        // 纬度之后加入时间
        String filePath = doubleToString(loc_latitude) + "ideal" + doubleToString(loc_longitude)
                + "##" + year + "#" + month + "#" + day + "#" + hour + "#" + min + "#" + second + ".wav";

        File wavFile = new File(file, filePath);
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

    /**
     *  play the audio according the time
     */
    private void time_play() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 222);
        // 询问权限
        File file = new File(this.getExternalFilesDir("").getAbsolutePath(), "");
        if (!file.exists()) {
            file.mkdirs();
        }
        File[] subFile = file.listFiles();
        boolean play_flag = false;
        Calendar calendars = Calendar.getInstance();

        calendars.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));

        String hour = String.valueOf(calendars.get(Calendar.HOUR));
        String min = String.valueOf(calendars.get(Calendar.MINUTE));
        int now_time = Integer.parseInt(hour) * 60 + Integer.parseInt(min);

        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            // 判断是否为文件夹
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                if(filename.indexOf("ideal") <= 0) {
                    continue;
                }
                int index_0 = filename.indexOf("##");   // 检测时间：时-分
                if(index_0 <=  5) {
                    continue;
                }
                String sub_filename = filename.substring(index_0 + 2);  // 此时格式为 年#月#日#时#分#秒
                sub_filename = sub_filename.substring(sub_filename.indexOf("#") + 1);   // 此时格式为 月#日#时#分#秒.wav
                sub_filename = sub_filename.substring(sub_filename.indexOf("#") + 1);   // 此时格式为 日#时#分#秒.wav
                sub_filename = sub_filename.substring(sub_filename.indexOf("#") + 1);   // 此时格式为 时#分#秒.wav
                int out_hour = Integer.parseInt(sub_filename.substring(0, sub_filename.indexOf("#")));
                sub_filename = sub_filename.substring(sub_filename.indexOf("#") + 1);   // 此时格式为 分#秒.wav
                int out_minute = Integer.parseInt(sub_filename.substring(0, sub_filename.indexOf("#")));

                int mod_num = 12 * 60;

                int end_time = (out_hour * 60 + out_minute + 5) % mod_num;
                int begin_time = (out_hour * 60 + out_minute - 5 >= 0) ? out_hour * 60 + out_minute - 5 : out_hour * 60 + out_minute - 5 + mod_num;

                if(now_time >= begin_time && now_time <= end_time){
                    if(now_time >= last_begin && now_time <= last_end && iFileLength <= last_time_num) {
                        continue;   // 不重复播放同一个时间的同样的音频，除非重启应用
                    }
                    if (mediaPlayer == null || (!mediaPlayer.isPlaying() && !mediaPlayer.isLooping())) {
                        play_flag = true;

                        initMediaPlayer(filename);
                        isMediaPlayerRelease = false;

                        mediaPlayer.start();
                        last_begin = begin_time;
                        last_end = end_time;
                        last_time_num = iFileLength;      // 记录上一次播放时的文件序号
                        btnplay.setEnabled(false);
                        btnpause.setEnabled(true);
                        btnstop.setEnabled(true);
                    }
                    break;
                }
            }
            else{
                continue;
            }
        }
        if(!play_flag && now_time > last_end ) {
            last_begin = -1;
            last_end = -1;
            last_time_num = -1;
        }
    }

    /**
     *  play the audio according the location
     */
    private void loc_play() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 222);
        // 询问权限
        File file = new File(this.getExternalFilesDir("").getAbsolutePath(), "");
        if (!file.exists()) {
            file.mkdirs();
        }
        File[] subFile = file.listFiles();
        boolean play_flag = false;
        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            // 判断是否为文件夹
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                int index_0 = filename.indexOf("ideal"); // 后续拆分成time、location、event三大模式之后，需要把文件名的识别加上去
                if(index_0 <= 0){
                    continue;
                }
                int index_1 = filename.indexOf("##");
                if(index_1 <= index_0 + 5) {
                    continue;
                }
                double out_latitude = Double.parseDouble(filename.substring(0, index_0));
                double out_longitude = Double.parseDouble(filename.substring(index_0 + 5, index_1));
                if(Math.abs(loc_latitude - out_latitude) + Math.abs(loc_longitude - out_longitude) < 0.0003){
                    if(Math.abs(last_loc_latitude - out_latitude) + Math.abs(last_loc_longitude - out_longitude) < 0.0003 && iFileLength <= last_serial_num) {
                        continue;   // 不重复播放同一个地点的同样的音频，如果游客长时间呆在同一个地点的话
                    }
                    if (mediaPlayer == null || (!mediaPlayer.isPlaying() && !mediaPlayer.isLooping())) {
                        play_flag = true;

                        initMediaPlayer(filename);
                        isMediaPlayerRelease = false;

                        mediaPlayer.start();
                        last_loc_latitude = out_latitude;   // 记录上一次播放时的经纬度
                        last_loc_longitude = out_longitude; // 记录上一次播放时的经纬度
                        last_serial_num = iFileLength;      // 记录上一次播放时的文件序号
                        btnplay.setEnabled(false);
                        btnpause.setEnabled(true);
                        btnstop.setEnabled(true);
                    }
                    break;
                }
            }
            else{
                continue;
            }
        }
        if(!play_flag && Math.abs(last_loc_latitude - loc_latitude) + Math.abs(last_loc_longitude - loc_longitude) >= 0.0006) {
            last_loc_longitude = 0;
            last_loc_latitude = 0;
            last_serial_num = -1;
        }

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
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                loc_longitude = longitude;  // 更新类的纬度
                loc_latitude = latitude;    // 更新类的经度
                time_play();
//                loc_play();
                TextView loctext1 = findViewById(R.id.longitude);
                TextView loctext2 = findViewById(R.id.latitude);
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
        btnConnect.setOnClickListener(v -> getPresenter().connect(mMac));
        btnDisconnect.setOnClickListener(v -> getPresenter().disConnect(mMac));
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

    private static int gravityUp = 4655 * 4655;
    private int allAccPowLen = 0;
    private int nearestDown = -1;
    private int accPlayNum = 1;
    private int maxAccPowOneSense(SensorData sensorData) {
        int x, y, totalAccPow, maxAccPow = 0;
        for (int i = 0; i < sensorData.accelDataLen; i++) {
            x = sensorData.accelData[i].x;
            y = sensorData.accelData[i].y;
            totalAccPow = x * x + y * y;
            if (maxAccPow < totalAccPow) {
                maxAccPow = totalAccPow;
            }
        }
        return maxAccPow;
    }


    @Override
    public void onSensorDataChanged(SensorData sensorData) {
        runOnUiThread(() -> {
            Map<String, String> map = new HashMap<>();
            map.put("data", sensorData.toString());
            maps.add(0, map);
            tvDataCount.setText(getString(R.string.sensor_data, maps.size()));

            if (sensorData.serviceId == 43) {
                int maxAccPow = maxAccPowOneSense(sensorData);

                if (maxAccPow > 0) {
                    tips.setText(String.valueOf(maxAccPow));

                    if (allAccPowLen < 3) {
                        if (maxAccPow < gravityUp) {
                            nearestDown = allAccPowLen;
                        }
                        allAccPowLen++;
                    } else {
                        if (maxAccPow < gravityUp) {
                            nearestDown = 3;
                        } else {
                            if (nearestDown >= 0) {
                                nearestDown--;
                            }
                        }

                        if (nearestDown == -1) {
                            if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
                                String filename = String.valueOf(accPlayNum) + ".mp3";
                                accPlayNum = (accPlayNum == 5) ? 1 : (accPlayNum + 1);

                                if (isMediaPlayerRelease) {
                                    initMediaPlayer(filename);
                                    isMediaPlayerRelease = false;
                                }
                                mediaPlayer.start();
                                btnplay.setEnabled(false);
                                btnpause.setEnabled(true);
                                btnstop.setEnabled(true);
                            }
                        }
                    }
                }
            }

//            simpleAdapter.notifyDataSetChanged();
        });
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
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        getPresenter().deInit();
    }
}
