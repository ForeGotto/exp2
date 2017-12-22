package cc.hisong.exp2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    volatile boolean isListening;
    Object lock;

    private Button button;
    private RippleBackground ripple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.RECORD_AUDIO},1);
        }

        lock = new Object();
        button = findViewById(R.id.button);
        ripple = (RippleBackground) findViewById(R.id.ripple0);

        button.setOnClickListener(view -> {
            if (!isListening) {
                Log.d(TAG, "开始录制");
                getNoiseLevel();
                button.setText("停止录制");

                ripple.startRippleAnimation();
            } else {
                Log.d(TAG, "停止录制");
                isListening = false;
                button.setText("开始录制");
                ripple.stopRippleAnimation();
            }
        });

    }

    public void getNoiseLevel() {
        if (isListening) {
            Log.e(TAG, "还在录着呢");
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }
        isListening = true;

        new Thread(() -> {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isListening) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "分贝值:" + volume);

                    MainActivity.this.runOnUiThread(() -> updateView(volume));
                    // 大概一秒2次
                    synchronized (lock) {
                        try {
                            lock.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        ).start();
    }

    private void updateView(double volume) {
        ripple.updateView(volume);
    }

}
