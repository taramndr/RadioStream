package tara.com.radiostreamservice;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.pkmmte.view.CircularImageView;

import tara.com.radiostreamservice.services.RadioService;
import tara.com.radiostreamservice.utils.Constants;

public class MainActivity extends AppCompatActivity implements RadioService.ServiceCallbacks {

    RadioService radioservice;
    private ProgressBar radioProgressBar;
    private Button btnPlayPause, btnStop;
    private ImageView ivMusic;
    private Boolean boolRadioPlaying = false;
    private Intent serviceIntent;
    private String radioStationUrl = "http://server2.crearradio.com:8371";
    private Boolean mBound = false;

    private boolean isOnline;
    private boolean isPause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            serviceIntent = new Intent(MainActivity.this, RadioService.class);
            //startService(intent);
            initializeViews();
            setListener();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        // Bind to RadioService
//        Intent intent = new Intent(this, RadioService.class);
//        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//    }

    private void initializeViews() {
        radioProgressBar = (ProgressBar) findViewById(R.id.progress_bar1);
        radioProgressBar.setMax(100);
        radioProgressBar.setVisibility(View.INVISIBLE);

        CircularImageView circularImageView = (CircularImageView) findViewById(R.id.iv_music);
        circularImageView.setBorderColor(R.color.colorLightPink);
        circularImageView.setBorderWidth(10);
        circularImageView.setSelectorColor(R.color.colorPrimary);
        circularImageView.setSelectorStrokeColor(R.color.colorPrimaryDark);
        circularImageView.setSelectorStrokeWidth(5);
        circularImageView.addShadow();

        btnPlayPause = (Button) findViewById(R.id.btn_PlayStop);
        btnStop = (Button) findViewById(R.id.btn_stop);

        if (!boolRadioPlaying) {
            btnPlayPause.setBackgroundResource(R.drawable.ic_btn_play);
        } else {
            if (boolRadioPlaying) {
                btnPlayPause.setBackgroundResource(R.drawable.ic_btn_pause);

            }
        }

    }

    private void setListener() {
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRadioService();
            }
        });
    }

    private void togglePlayPause() {
        if (!boolRadioPlaying) {
            btnPlayPause.setBackgroundResource(R.drawable.ic_btn_pause);

            //show progress bar while buffering
            radioProgressBar.setVisibility(View.VISIBLE);

            playRadio();
            boolRadioPlaying = true;
            //radioProgressBar.setVisibility(View.INVISIBLE);

        } else {
            if (boolRadioPlaying) {
                btnPlayPause.setBackgroundResource(R.drawable.ic_btn_play);
                pauseRadioService();
                boolRadioPlaying = false;
            }
        }
    }

    private void pauseRadioService() {
        radioservice.pausePlayer();
        isPause = true;
    }


    private void playRadio() {

        checkConnectivity();
        if (isOnline) {
            serviceIntent.putExtra(Constants.RADIO_STATION_URL, radioStationUrl);
            try {
                startService(serviceIntent);

                //added later for hiding progressbar


            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setTitle("Network not connected!");
            alertDialogBuilder.setMessage("Please connect to network and try again!");
            alertDialogBuilder.setIcon(R.drawable.ic_fm_radio);
            alertDialogBuilder.setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    restartActivity();
                }
            });
            alertDialogBuilder.setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alertDialogBuilder.create();
            btnPlayPause.setBackgroundResource(R.drawable.ic_btn_play);
            radioProgressBar.setVisibility(View.INVISIBLE);
            alertDialogBuilder.show();
        }


    }

    private void restartActivity() {
        Intent mIntent = getIntent();
        finish();
        startActivity(mIntent);
        overridePendingTransition(R.anim.fade_out, R.anim.fade_in);

    }

    private void stopRadioService() {
        try {
            stopService(serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolRadioPlaying = false;
    }

    private void checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            isOnline = true;
        } else {
            isOnline = false;
        }
    }

    @Override
    public void doSomething(boolean playerPlaying) {
        if (playerPlaying) {
            //player isd playing
        } else {
           // player is  not playing
        }
    }
}
