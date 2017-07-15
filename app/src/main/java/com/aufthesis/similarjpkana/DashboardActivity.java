package com.aufthesis.similarjpkana;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// Created by yoichi75jp2 on 2017/03/04.
public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {

    static public final List<Integer> m_listMax =
            new ArrayList<>(Arrays.asList(50,100,200,300,400,500,750,1000,1500,2000,2500,3000,3500,4000,4500,5000,6000,7000,8000,9000,10000,
                    11000,12000,13000,14000,15000,16000,17000,18000,19000,20000,25000,30000,35000,40000,45000,50000,60000,70000,80000,90000,
                    100000,110000,120000,130000,140000,150000,160000,170000,180000,190000,200000));

    private CheckBox m_checkBoxMaxTime = null;
    private CheckBox m_checkBoxTimeAttack = null;
    private TextView m_textMaxTime = null;
    private TextView m_textTimeAttack = null;
    private Button m_buttonClear = null;

    private Drawable m_drawable_clear = null;
    private Drawable m_drawable_clear_disable = null;

    private SharedPreferences m_prefs;

    private AdView m_adView;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        m_prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        long max_time = m_prefs.getLong(getString(R.string.max_time), 0);
        int count_time_attack = m_prefs.getInt(getString(R.string.time_attack), 0);
        int play_count = m_prefs.getInt(getString(R.string.play_count), 0);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        m_drawable_clear = ResourcesCompat.getDrawable(getResources(), R.drawable.clear, null);
        m_drawable_clear_disable = ResourcesCompat.getDrawable(getResources(), R.drawable.clear_disable, null);

        TextView deskText = (TextView)findViewById(R.id.description);
        TextView maxText = (TextView)findViewById(R.id.max);
        ProgressBar bar = (ProgressBar)findViewById(R.id.progressbar);
        //bar.setBackgroundColor(Color.parseColor("#00FF00"));
        bar.setMinimumHeight(50);
        bar.setMax(50);
        bar.setProgress(0);
        this.setTitle(getString(R.string.dashboard_title, 1, 50));

        for(int i = 0; i < m_listMax.size(); i++)
        {
            int max = m_listMax.get(i);
            if(play_count <= max)
            {
                bar.setMax(max);
                maxText.setText(String.valueOf(max));
                this.setTitle(getString(R.string.dashboard_title, i+1, max-play_count));
                break;
            }
        }
        bar.setProgress(play_count);
        deskText.setText(getString(R.string.desc_result, play_count));

        m_checkBoxMaxTime = (CheckBox)findViewById(R.id.check_max_time);
        m_checkBoxMaxTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean isEnable = buttonView.isChecked() || m_checkBoxTimeAttack.isChecked();
                if(isEnable)
                    m_buttonClear.setCompoundDrawablesWithIntrinsicBounds(null, m_drawable_clear , null, null);
                else
                    m_buttonClear.setCompoundDrawablesWithIntrinsicBounds(null, m_drawable_clear_disable , null, null);
                m_buttonClear.setEnabled(isEnable);
            }
        });

        m_textMaxTime = (TextView)findViewById(R.id.max_time_score);
        SimpleDateFormat dataFormat = new SimpleDateFormat("ss.SSS", Locale.getDefault());
        String time_score = getString(R.string.record_max_time_score, dataFormat.format(max_time));
        m_textMaxTime.setText(time_score);

        m_checkBoxTimeAttack = (CheckBox)findViewById(R.id.check_time_attack);
        m_checkBoxTimeAttack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean isEnable = buttonView.isChecked() || m_checkBoxMaxTime.isChecked();
                if(isEnable)
                    m_buttonClear.setCompoundDrawablesWithIntrinsicBounds(null, m_drawable_clear , null, null);
                else
                    m_buttonClear.setCompoundDrawablesWithIntrinsicBounds(null, m_drawable_clear_disable , null, null);
                m_buttonClear.setEnabled(isEnable);
            }
        });

        m_textTimeAttack = (TextView)findViewById(R.id.time_attack_score);
        m_textTimeAttack.setText(getString(R.string.record_time_attack_score, count_time_attack));

        m_buttonClear = (Button)findViewById(R.id.clear_button);
        m_buttonClear.setEnabled(false);
        m_buttonClear.setOnClickListener(this);
        m_buttonClear.setCompoundDrawablesWithIntrinsicBounds(null, m_drawable_clear_disable , null, null);

        //バナー広告
        m_adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_adView.loadAd(adRequest);
    }

    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.clear_button)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.confirm));
            dialog.setMessage(getString(R.string.confirm_message));
            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = m_prefs.edit();
                    if(m_checkBoxMaxTime.isChecked())
                    {
                        editor.putLong(getString(R.string.max_time), 0);
                        SimpleDateFormat dataFormat = new SimpleDateFormat("ss.SSS", Locale.getDefault());
                        String time_score = getString(R.string.record_max_time_score, dataFormat.format(0));
                        m_textMaxTime.setText(time_score);
                    }
                    if(m_checkBoxTimeAttack.isChecked())
                    {
                        editor.putInt(getString(R.string.time_attack), 0);
                        m_textTimeAttack.setText(getString(R.string.record_time_attack_score, 0));
                    }
                    editor.apply();
                    m_checkBoxMaxTime.setChecked(false);
                    m_checkBoxTimeAttack.setChecked(false);
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
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
        getMenuInflater().inflate(R.menu.menu_dashboad, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
