package com.huawei.audiodevicekit.bluetoothsample.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.huawei.audiodevicekit.R;

public class location extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        Button loc_event = findViewById(R.id.location_event);
        loc_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "location event", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setClass(location.this, header.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent);
                Intent intent1 = new Intent();
                intent1.setClass(location.this, event.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
                startActivity(intent1);
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
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setClass(location.this, header.class);//this前面为当前activty名称，class前面为要跳转到得activity名称
        startActivity(intent);
    }
}