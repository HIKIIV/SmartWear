package com.huawei.audiodevicekit.bluetoothsample.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.audiodevicekit.R;

import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;

public class location extends AppCompatActivity {

    private StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
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
        }
    };

    private boolean on_recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        on_recording = false;

        Button loc_event = findViewById(R.id.location_event);
        loc_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(location.this, event.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent);
            }
        });

        Button loc_time = findViewById(R.id.location_time);
        loc_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(location.this, time.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent);
            }
        });

        Button loc_back = findViewById(R.id.location_back);
        loc_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(location.this, header.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent);
            }
        });

        TextView record_sig = findViewById(R.id.location_record_sig);
        record_sig.setBackgroundColor(0);
        Button location_record = findViewById(R.id.location_record);
        location_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!on_recording) {
                    Toast.makeText(getApplicationContext(), "location record start", Toast.LENGTH_SHORT).show();
                    on_recording = true;
                    IdealRecorder idealRecorder = IdealRecorder.getInstance();
                    idealRecorder.setRecordFilePath(SampleBtActivity.getSaveFilePath("GPS"));
                    //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
                    IdealRecorder.RecordConfig recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(300000).setVolumeInterval(200);
                    //设置录音配置 最长录音时长 以及音量回调的时间间隔
                    idealRecorder.setStatusListener(statusListener);
                    idealRecorder.start();

                    Animation alphaAnimation = new AlphaAnimation(1, 0); // 录制信号开始闪烁
                    alphaAnimation.setDuration(300);
                    alphaAnimation.setInterpolator(new LinearInterpolator());
                    alphaAnimation.setRepeatCount(Animation.INFINITE);
                    alphaAnimation.setRepeatMode(Animation.REVERSE);
                    record_sig.setBackgroundColor(Color.rgb(255, 0, 0));
                    record_sig.startAnimation(alphaAnimation);
                }else{
                    Toast.makeText(getApplicationContext(), "location record ends", Toast.LENGTH_SHORT).show();
                    on_recording = false;
                    IdealRecorder idealRecorder = IdealRecorder.getInstance();
                    idealRecorder.stop();
                    record_sig.setBackgroundColor(0);
                    record_sig.clearAnimation(); // 录制信号停止闪烁
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            record_sig.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    // 设置按钮圆角率为30
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 30);
                    // 设置按钮为圆形
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            record_sig.setClipToOutline(true);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setClass(location.this, header.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
        startActivity(intent);
    }
}