package com.aufthesis.similarjpkana;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
// Created by yoichi75jp2 on 2017/04/27.
 */

public class PlayActivity extends AppCompatActivity implements View.OnClickListener, Runnable {

    private LinearLayout m_layout;
    private Context m_context;
    //private List<Button> m_listButton = new ArrayList<>();
    private TextView m_timerText;
    private int m_targetID = -1;
    private int m_min = 6;

    private List<Integer> m_listMatrix = new ArrayList<>();
    private Map<Integer, Integer> m_mapMatrixSize = new ConcurrentHashMap<>();

    private List<_kana> m_listKana = new ArrayList<>();

    private class _kana
    {
        String kana1;
        String kana2;
    }

    private _kana m_targetKana = new _kana();

    // 効果音用
    final int SOUND_POOL_MAX = 7;
    private SoundPool m_soundPool;
    private int m_kana1Sound;
    private int m_kana2Sound;
    private int m_correctSound;
    private int m_clearSoundID;
    private int m_longClearSoundID;
    private int m_levelUpID;
    private int m_clickSound;
    private Map<String, Integer> m_mapKanaSound = new ConcurrentHashMap<>();

    // 10 m_sec order
    private long m_startTime;
    private long m_pauseTime;
    private long m_diffTime;
    private SimpleDateFormat m_dataFormat = new SimpleDateFormat("mm:ss.SSS", Locale.getDefault());
    private final Handler m_handler = new Handler();
    private volatile boolean m_stopRun = false;
    private Thread m_thread;

    private SharedPreferences m_prefs;

    private boolean m_isTimeAttackMode = false;
    private int m_timeAttackCount = 0;

    private AdView m_adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        m_context = this;

        Intent intent = getIntent();
        int mode = intent.getExtras().getInt("key");

        for(int i = 5; i <= 16; i++)
        {
            m_listMatrix.add(i);
            m_mapMatrixSize.put(i, 55-((i-5)*2));
        }

        m_layout = (LinearLayout)findViewById(R.id.field);
        DBOpenHelper dbHelper = new DBOpenHelper(this);
        SQLiteDatabase db = dbHelper.getDataBase();

        String sql = "select * from kana";
        if(mode == 1)
            sql += " where mode = 1";
        if(mode == 2)
            sql += " where mode = 2";
        Cursor cursor = db.rawQuery(sql, null);
        cursor.moveToFirst();
        if (cursor.getCount() != 0)
        {
            for (int i = 0; i < cursor.getCount(); i++)
            {
                String kana1 = cursor.getString(0);
                String kana2 = cursor.getString(1);
                _kana tmp = new _kana();
                tmp.kana1 = kana1;
                tmp.kana2 = kana2;
                m_listKana.add(tmp);
                cursor.moveToNext();
            }
        }
        cursor.close();

        m_isTimeAttackMode = mode == 4;
        m_thread = new Thread(this);

        Button renewButton = (Button)findViewById(R.id.renew);
        renewButton.setOnClickListener(this);

        m_timerText = (TextView)findViewById(R.id.record);
        TextView m_instructionText = (TextView)findViewById(R.id.description) ;
        float textSize = 13.f*setScaleSize(this);
        m_timerText.setTextSize(textSize);
        m_instructionText.setTextSize(textSize);

        m_prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        this.setKanaSound();
        this.setData();

        if(m_isTimeAttackMode)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.time_attack_title1));
            dialog.setCancelable(false);
            dialog.setMessage(getString(R.string.time_attack_message1));
            dialog.setPositiveButton(getString(R.string.time_attack_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_thread.start();
                    m_startTime = System.currentTimeMillis();
                    m_timeAttackCount = 0;
                }
            });
            dialog.setNegativeButton(getString(R.string.time_attack_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            dialog.show();
        }

        //バナー広告
        m_adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_adView.loadAd(adRequest);
    }

    private void setData()
    {
        if(!m_isTimeAttackMode)
        {
            m_stopRun = false;
            m_thread = new Thread(this);
        }
        m_min = 6;
        m_targetID = -1;
        //m_listButton.clear();
        m_layout.removeAllViews();

        if(m_listKana.size() == 0 || m_listMatrix.size() == 0)
            return;

        Collections.shuffle(m_listMatrix);
        Collections.shuffle(m_listKana);

        int count = 0;
        int matrix = m_listMatrix.get(0);
        m_targetKana = m_listKana.get(0);
        int targetCount = this.getRandomNumber(matrix);
        if(matrix > 12) m_min = 7;
        float textSize = m_mapMatrixSize.get(matrix)*setScaleSize(this);
        for(int row = 1; row <= matrix; row++)
        {
            if(row > m_min)
                break;
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            //rowParam.weight = 1.0f;
            rowLayout.setLayoutParams(rowParam);
            rowLayout.setGravity(Gravity.CENTER);
            for(int column = 1; column <= matrix; column++)
            {
                count++;
                Button button = new Button(this);
                LinearLayout.LayoutParams colParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                colParam.weight = 1.0f;
                button.setLayoutParams(colParam);
                if(count == targetCount)
                    button.setText(m_targetKana.kana2);
                else
                    button.setText(m_targetKana.kana1);
                button.setTextSize(textSize);
                button.setTextColor(Color.WHITE);
                button.setId(count);
                rowLayout.addView(button);
                button.setOnClickListener(this);
                /*
                button.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                        }
                        return false;
                    }
                });
                */
                button.setBackgroundColor(Color.parseColor("#00000000"));
            }
            m_layout.addView(rowLayout);
        }
        this.setSound(m_targetKana.kana1, m_targetKana.kana2);

        if(!m_isTimeAttackMode)
        {
            m_thread.start();
            m_startTime = System.currentTimeMillis();
        }
    }

    private void setSound(String kana1, String kana2)
    {
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
        m_kana1Sound = m_soundPool.load(getApplicationContext(), m_mapKanaSound.get(kana1), 1);
        m_kana2Sound = m_soundPool.load(getApplicationContext(), m_mapKanaSound.get(kana2), 1);
        m_levelUpID = m_soundPool.load(getApplicationContext(), R.raw.ji_023, 1);
        m_correctSound = m_soundPool.load(getApplicationContext(), R.raw.correct2, 1);
        m_clearSoundID = m_soundPool.load(getApplicationContext(), R.raw.cheer, 1);
        m_clickSound = m_soundPool.load(getApplicationContext(), R.raw.tiny_button_push, 1);
        m_longClearSoundID = m_soundPool.load(getApplicationContext(), R.raw.cheer_long, 1);
    }

    private int getRandomNumber(int matrix)
    {
        int min = matrix;
        if(matrix > m_min) min = m_min;
        int max = matrix*min;

        List<Integer> list = new ArrayList<>();
        for(int i = 1; i <= max; i++)
            list.add(i);

        Collections.shuffle(list);

        return list.get(0);
    }

    public void onClick(View view)
    {
        int id = view.getId();
        try
        {
            if(id == R.id.renew)
            {
                m_soundPool.play(m_clickSound, 1.0F, 1.0F, 0, 0, 1.0F);
                Thread.sleep(100);
                Intent intent = new Intent(this, DummyActivity.class);
                startActivityForResult(intent, 1);
            }
            else
            {
                Button kanaButton = (Button)findViewById(id);
                if(kanaButton != null)
                {
                    String kana = kanaButton.getText().toString();
                    if(m_targetKana.kana1.equals(kana))
                    {
                        m_soundPool.play(m_kana1Sound, 1.0F, 1.0F, 0, 0, 0.9F);
                    }
                    else if(m_targetKana.kana2.equals(kana))
                    {
                        m_soundPool.play(m_kana2Sound, 1.0F, 1.0F, 0, 0, 1.5F);
                        kanaButton.setTextColor(Color.parseColor("#d7003a"));
                        m_targetID = id;
                        if(m_isTimeAttackMode)
                        {
                            m_timeAttackCount++;
                            m_soundPool.play(m_correctSound, 1.0F, 1.0F, 0, 0, 1.0F);
                            if(!m_stopRun)
                                this.setData();
                        }
                        else
                        {
                            m_stopRun = true;
                            long diffTime = m_diffTime;
                            m_timerText.setText(m_dataFormat.format(diffTime));
                            long max_time = m_prefs.getLong(getString(R.string.max_time), 0);
                            if(diffTime < max_time || max_time == 0)
                            {
                                if(diffTime < 1000)
                                {
                                    m_soundPool.play(m_longClearSoundID, 1.0F, 1.0F, 0, 0, 1.0F);
                                    LayoutInflater inflater = getLayoutInflater();
                                    view = inflater.inflate(R.layout.toast_layout, null);
                                    TextView text = (TextView) view.findViewById(R.id.toast_text);
                                    text.setText(getString(R.string.goal_achievement));
                                    text.setGravity(Gravity.CENTER);
                                    text.setTextSize(30*setScaleSize(this));
                                    Toast toast = Toast.makeText(this, getString(R.string.goal_achievement), Toast.LENGTH_LONG);
                                    toast.setView(view);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();

                                    this.induceReview();
                                }
                                SharedPreferences.Editor editor = m_prefs.edit();
                                editor.putLong(getString(R.string.max_time), diffTime);
                                editor.apply();
                            }
                            this.setCountAnswer(view);

                            int count = m_prefs.getInt(getString(R.string.count_induce), 0);
                            count++;
                            SharedPreferences.Editor editor = m_prefs.edit();
                            editor.putInt(getString(R.string.count_induce), count);
                            editor.apply();
                        }
                    }
                }
            }
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setCountAnswer(View view)
    {
        SharedPreferences.Editor editor = m_prefs.edit();
        int preCount = m_prefs.getInt(getString(R.string.play_count), 0);
        preCount++;
        editor.putInt(getString(R.string.play_count), preCount);

        editor.apply();
        if (DashboardActivity.m_listMax.indexOf(preCount) >= 0)
        {
            m_soundPool.play(m_levelUpID, 1.0F, 1.0F, 0, 0, 1.0F);
            LayoutInflater inflater = getLayoutInflater();
            view = inflater.inflate(R.layout.toast_layout, null);
            TextView text = (TextView) view.findViewById(R.id.toast_text);
            text.setText(getString(R.string.goal_achievement2, preCount));
            text.setGravity(Gravity.CENTER);
            text.setTextSize(30*setScaleSize(this));
            Toast toast = Toast.makeText(this, getString(R.string.goal_achievement), Toast.LENGTH_LONG);
            toast.setView(view);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
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
            finish();
            overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.setSound(m_targetKana.kana1, m_targetKana.kana2);
        if (m_adView != null) {
            m_adView.resume();
        }
    }

    @Override
    public void onPause() {
        if (m_adView != null) {
            m_adView.pause();
        }
        super.onPause();
        m_soundPool.release();
        m_pauseTime = System.currentTimeMillis();
        m_soundPool.release();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        long endTime = System.currentTimeMillis();
        m_startTime = m_startTime - (endTime - m_pauseTime);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                this.setData();
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:break;
        }
    }

    private void setKanaSound()
    {
        m_mapKanaSound.put("あ" , R.raw.a_1 );
        m_mapKanaSound.put("い" , R.raw.i_1 );
        m_mapKanaSound.put("う" , R.raw.u_1 );
        m_mapKanaSound.put("え" , R.raw.e_1 );
        m_mapKanaSound.put("お" , R.raw.o_1 );
        m_mapKanaSound.put("か" , R.raw.ka_1 );
        m_mapKanaSound.put("き" , R.raw.ki_1 );
        m_mapKanaSound.put("く" , R.raw.ku_1 );
        m_mapKanaSound.put("け" , R.raw.ke_1 );
        m_mapKanaSound.put("こ" , R.raw.ko_1 );
        m_mapKanaSound.put("さ" , R.raw.sa_1 );
        m_mapKanaSound.put("し" , R.raw.si_1 );
        m_mapKanaSound.put("す" , R.raw.su_1 );
        m_mapKanaSound.put("せ" , R.raw.se_1 );
        m_mapKanaSound.put("そ" , R.raw.so_1 );
        m_mapKanaSound.put("た" , R.raw.ta_1 );
        m_mapKanaSound.put("ち" , R.raw.ti_1 );
        m_mapKanaSound.put("つ" , R.raw.tu_1 );
        m_mapKanaSound.put("て" , R.raw.te_1 );
        m_mapKanaSound.put("と" , R.raw.to_1 );
        m_mapKanaSound.put("な" , R.raw.na_1 );
        m_mapKanaSound.put("に" , R.raw.ni_1 );
        m_mapKanaSound.put("ぬ" , R.raw.nu_1 );
        m_mapKanaSound.put("ね" , R.raw.ne_1 );
        m_mapKanaSound.put("の" , R.raw.no_1 );
        m_mapKanaSound.put("は" , R.raw.ha_1 );
        m_mapKanaSound.put("ひ" , R.raw.hi_1 );
        m_mapKanaSound.put("ふ" , R.raw.hu_1 );
        m_mapKanaSound.put("へ" , R.raw.he_1 );
        m_mapKanaSound.put("ほ" , R.raw.ho_1 );
        m_mapKanaSound.put("ま" , R.raw.ma_1 );
        m_mapKanaSound.put("み" , R.raw.mi_1 );
        m_mapKanaSound.put("む" , R.raw.mu_1 );
        m_mapKanaSound.put("め" , R.raw.me_1 );
        m_mapKanaSound.put("も" , R.raw.mo_1 );
        m_mapKanaSound.put("や" , R.raw.ya_1 );
        m_mapKanaSound.put("ゆ" , R.raw.yu_1 );
        m_mapKanaSound.put("よ" , R.raw.yo_1 );
        m_mapKanaSound.put("ら" , R.raw.ra_1 );
        m_mapKanaSound.put("り" , R.raw.ri_1 );
        m_mapKanaSound.put("る" , R.raw.ru_1 );
        m_mapKanaSound.put("れ" , R.raw.re_1 );
        m_mapKanaSound.put("ろ" , R.raw.ro_1 );
        m_mapKanaSound.put("わ" , R.raw.wa_1 );
        m_mapKanaSound.put("を" , R.raw.o_1 );
        m_mapKanaSound.put("ん" , R.raw.n_1);
        m_mapKanaSound.put("ア" , R.raw.a_1 );
        m_mapKanaSound.put("イ" , R.raw.i_1 );
        m_mapKanaSound.put("ウ" , R.raw.u_1 );
        m_mapKanaSound.put("エ" , R.raw.e_1 );
        m_mapKanaSound.put("オ" , R.raw.o_1 );
        m_mapKanaSound.put("カ" , R.raw.ka_1 );
        m_mapKanaSound.put("キ" , R.raw.ki_1 );
        m_mapKanaSound.put("ク" , R.raw.ku_1 );
        m_mapKanaSound.put("ケ" , R.raw.ke_1 );
        m_mapKanaSound.put("コ" , R.raw.ko_1 );
        m_mapKanaSound.put("サ" , R.raw.sa_1 );
        m_mapKanaSound.put("シ" , R.raw.si_1 );
        m_mapKanaSound.put("ス" , R.raw.su_1 );
        m_mapKanaSound.put("セ" , R.raw.se_1 );
        m_mapKanaSound.put("ソ" , R.raw.so_1 );
        m_mapKanaSound.put("タ" , R.raw.ta_1 );
        m_mapKanaSound.put("チ" , R.raw.ti_1 );
        m_mapKanaSound.put("ツ" , R.raw.tu_1 );
        m_mapKanaSound.put("テ" , R.raw.te_1 );
        m_mapKanaSound.put("ト" , R.raw.to_1 );
        m_mapKanaSound.put("ナ" , R.raw.na_1 );
        m_mapKanaSound.put("ニ" , R.raw.ni_1 );
        m_mapKanaSound.put("ヌ" , R.raw.nu_1 );
        m_mapKanaSound.put("ネ" , R.raw.ne_1 );
        m_mapKanaSound.put("ノ" , R.raw.no_1 );
        m_mapKanaSound.put("ハ" , R.raw.ha_1 );
        m_mapKanaSound.put("ヒ" , R.raw.hi_1 );
        m_mapKanaSound.put("フ" , R.raw.hu_1 );
        m_mapKanaSound.put("ヘ" , R.raw.he_1 );
        m_mapKanaSound.put("ホ" , R.raw.ho_1 );
        m_mapKanaSound.put("マ" , R.raw.ma_1 );
        m_mapKanaSound.put("ミ" , R.raw.mi_1 );
        m_mapKanaSound.put("ム" , R.raw.mu_1 );
        m_mapKanaSound.put("メ" , R.raw.me_1 );
        m_mapKanaSound.put("モ" , R.raw.mo_1 );
        m_mapKanaSound.put("ヤ" , R.raw.ya_1 );
        m_mapKanaSound.put("ユ" , R.raw.yu_1 );
        m_mapKanaSound.put("ヨ" , R.raw.yo_1 );
        m_mapKanaSound.put("ラ" , R.raw.ra_1 );
        m_mapKanaSound.put("リ" , R.raw.ri_1 );
        m_mapKanaSound.put("ル" , R.raw.ru_1 );
        m_mapKanaSound.put("レ" , R.raw.re_1 );
        m_mapKanaSound.put("ロ" , R.raw.ro_1 );
        m_mapKanaSound.put("ワ" , R.raw.wa_1 );
        m_mapKanaSound.put("ヲ" , R.raw.o_1 );
        m_mapKanaSound.put("ン" , R.raw.n_1);
    }

    public void run() {

        while (!m_stopRun) {
            // sleep: period m_sec
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                m_stopRun = true;
            }

            m_handler.post(new Runnable() {
                @Override
                public void run() {
                    long endTime = System.currentTimeMillis();
                    // カウント時間 = 経過時間 - 開始時間
                    m_diffTime = (endTime - m_startTime);
                    if(!m_stopRun)
                        m_timerText.setText(m_dataFormat.format(m_diffTime));
                    if(m_isTimeAttackMode && m_diffTime >= 30000 && !m_stopRun)
                    {
                        int max_count = m_prefs.getInt(getString(R.string.time_attack), 0);
                        AlertDialog.Builder dialog = new AlertDialog.Builder(m_context);
                        dialog.setTitle(getString(R.string.time_attack_title2));
                        dialog.setCancelable(false);
                        String message = getString(R.string.time_attack_message2, m_timeAttackCount);
                        if(max_count < m_timeAttackCount)
                        {
                            m_soundPool.play(m_levelUpID, 1.0F, 1.0F, 0, 0, 1.0F);
                            message += "\n" + getString(R.string.time_attack_message3);
                            SharedPreferences.Editor editor = m_prefs.edit();
                            editor.putInt(getString(R.string.time_attack), m_timeAttackCount);
                            editor.apply();
                        }
                        else
                            m_soundPool.play(m_clearSoundID, 1.0F, 1.0F, 0, 0, 1.0F);

                        dialog.setMessage(message);
                        dialog.setPositiveButton(getString(R.string.time_attack_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                            }
                        });
                        dialog.show();
                        m_stopRun = true;
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_play, menu);
        //m_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
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

    private void induceReview()
    {
        int count = m_prefs.getInt(getString(R.string.count_induce), 0);
        if(count >= 500)
        {
            try
            {
                Thread.sleep(500);
            }
            catch(Exception e){}
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.induce_title));
            dialog.setMessage(getString(R.string.induce_message));
            dialog.setPositiveButton(getString(R.string.induce_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = m_prefs.edit();
                    editor.putInt(getString(R.string.count_induce), 0);
                    editor.apply();
                    Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                    googlePlayIntent.setData(Uri.parse("market://details?id=com.aufthesis.similarjpkana"));
                    startActivity(googlePlayIntent);
                }
            });
            dialog.setNegativeButton(getString(R.string.induce_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        }
    }

}
