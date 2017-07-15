package com.aufthesis.similarjpkana;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 効果音用
    final int SOUND_POOL_MAX = 6;
    private SoundPool m_soundPool;
    private int m_clickSound;

    private SharedPreferences m_prefs;

    private AdView m_adView;
    private static InterstitialAd m_InterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        float stringSize = 40.f*setScaleSize(this);

        Button button = (Button)findViewById(R.id.hiragana);
        button.setOnClickListener(this);
        button.setTextSize(stringSize);

        button = (Button)findViewById(R.id.katakana);
        button.setOnClickListener(this);
        button.setTextSize(stringSize);

        button = (Button)findViewById(R.id.mixture);
        button.setOnClickListener(this);
        button.setTextSize(stringSize);

        button = (Button)findViewById(R.id.time_attack);
        button.setOnClickListener(this);
        button.setTextSize(stringSize);

        //バナー広告
        m_adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_adView.loadAd(adRequest);

        // AdMobインターステイシャル
        m_InterstitialAd = new InterstitialAd(this);
        m_InterstitialAd.setAdUnitId(getString(R.string.adUnitInterId));
        m_InterstitialAd.loadAd(new AdRequest.Builder().build());
        m_InterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                m_InterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    public void onClick(View view)
    {
        int id = view.getId();
        Intent intent = new Intent(this, PlayActivity.class);
        int requestCode;
        switch (id)
        {
            case R.id.hiragana:
                intent.putExtra("key", 1);
                requestCode = 1;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            case R.id.katakana:
                intent.putExtra("key", 2);
                requestCode = 2;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            case R.id.mixture:
                intent.putExtra("key", 3);
                requestCode = 3;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            case R.id.time_attack:
                intent.putExtra("key", 4);
                requestCode = 4;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            default:break;
        }
        m_soundPool.play(m_clickSound, 1.0F, 1.0F, 0, 0, 1.0F);
    }

    /**
     * drawableにdummy.png（480px×1px）を仕込ませて、幅サイズの基準値にして、
     * 画面サイズによって拡大縮小の調整をする。
     */
    public static float setScaleSize(Context context) {

        //stone.pngを読み込んでBitmap型で扱う
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.dummy);

        //画面サイズ取得の準備
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        float width = size.x;

        return width / (float)bitmap.getWidth();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.final_title));
            dialog.setCancelable(false);
            dialog.setMessage(getString(R.string.final_message));
            dialog.setPositiveButton(getString(R.string.final_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    //moveTaskToBack(true);
                }
            });
            dialog.setNegativeButton(getString(R.string.final_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (m_adView != null) {
            m_adView.resume();
        }

        // 予め音声データを読み込む
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            m_soundPool = new SoundPool(SOUND_POOL_MAX, AudioManager.STREAM_MUSIC, 0);
        }
        else
        {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            m_soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(SOUND_POOL_MAX)
                    .build();
        }
        m_clickSound = m_soundPool.load(getApplicationContext(), R.raw.tiny_button_push, 1);
    }

    @Override
    public void onPause() {
        if (m_adView != null) {
            m_adView.pause();
        }
        super.onPause();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onDestroy()
    {
        if (m_adView != null) {
            m_adView.destroy();
        }
        super.onDestroy();
        setResult(RESULT_OK);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //m_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.close)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.final_title));
            dialog.setMessage(getString(R.string.final_message));
            dialog.setCancelable(false);
            dialog.setPositiveButton(getString(R.string.final_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    //moveTaskToBack(true);
                }
            });
            dialog.setNegativeButton(getString(R.string.final_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        }
        if(id == R.id.dashboard)
        {
            Intent intent = new Intent(this, DashboardActivity.class);
            int requestCode = 5;
            startActivityForResult(intent, requestCode);
            //startActivity(intent);
            // アニメーションの設定
            overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                int count = m_prefs.getInt(getString(R.string.count_timeattack), 0);
                count++;
                if(count >= 5)
                {
                    count = 0;
                    if(m_InterstitialAd.isLoaded())
                        m_InterstitialAd.show();
                }
                SharedPreferences.Editor editor = m_prefs.edit();
                editor.putInt(getString(R.string.count_timeattack), count);
                editor.apply();
                break;
            case 5:
                break;
            default:break;
        }
    }

}