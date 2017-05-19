package tara.com.radiostreamservice.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;

import tara.com.radiostreamservice.MainActivity;
import tara.com.radiostreamservice.R;
import tara.com.radiostreamservice.utils.Constants;

public class RadioService extends Service {

    private static final String TAG = "RadioService" ;
    private static MediaPlayer player;
    private String radioStationUrl;
    private static final int NOTIFICATION_ID = 1;
    private WifiManager wifiLock;

    private IBinder mBinder = new MyBinder();

    // Registered callbacks
    private ServiceCallbacks serviceCallbacks;  // to give response back to activity

    Context context;

    public RadioService() {
    }


    @Override
    public IBinder onBind(Intent intent) {  //Binds service to activity to access method within service
        Log.i(TAG, "onBind:, ");
        return mBinder; //return null;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {  //on service create
        super.onCreate();
        initializeMediaPlayer();

        Log.i(TAG, "onCreate: , Service started...");
    }

    /** class which inherits Binder class which in-turn implements IBinder interface. This class has a method which returns
     * the object of this RadioService class.
     * Though this object any android application component would be able to access public methods of this class.**/
    public class MyBinder extends Binder {

        public RadioService getService() {  //main service class - RadioService will bound to an activity.
            return RadioService.this;
        }

    }

    public static void pausePlayer(){

        if (player.isPlaying() == true){
            player.pause();
            //notificationManagerPlay.cancel(5);
            //pauseplay = 0;
        }
    }

    private void initializeMediaPlayer() {
        player = new MediaPlayer();
        //player.setLooping(true); // Set looping
        player.setVolume(100,100);
        //player should stay available even if the phone is idle but may reduce phone battery
        //To ensure that the CPU continues running while your MediaPlayer is playing, since it holds the specified lock while playing and releases the lock when paused or stopped:
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        //when you start preparing the MediaPlayer with the remote URL, you should create and acquire the Wi-Fi lock
//        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
//                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

       // wifiLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //on service start

        //Start Notification
        initNotification();

        //return super.onStartCommand(intent, flags, startId);
        radioStationUrl = intent.getExtras().getString(Constants.RADIO_STATION_URL);
        player.reset();

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        if(!player.isPlaying()) {
            try {
                player.setDataSource(radioStationUrl);
                player.prepareAsync(); //might take long for buffering, etc.

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }

            /** Called when MediaPlayer is ready */
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //radioProgressBar.setVisibility(View.INVISIBLE);
                    if (serviceCallbacks != null) {
                        serviceCallbacks.doSomething(player.isPlaying());
                    }

                    if(!player.isPlaying()){
                        player.start();
                    }

                }
            });

            // if error occur playing mediaplayer
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                            Toast.makeText(RadioService.this, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK : " + extra, Toast.LENGTH_SHORT).show();
                            break;
                        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                            Toast.makeText(RadioService.this, "MEDIA_ERROR_SERVER_DIED : " + extra, Toast.LENGTH_SHORT).show();
                            break;
                        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                            Toast.makeText(RadioService.this, "MEDIA_ERROR_UNKNOWN : " + extra, Toast.LENGTH_SHORT).show();
                            break;
                    }
                    return false;
                }
            });

            //Media player on buffering
//            player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
//                @Override
//                public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                    // radioProgressBar.setSecondaryProgress(percent);
//                     Log.i(TAG, "onBufferingUpdate : " + percent);
//                }
//            });

        }

        return Service.START_STICKY;  //if not put service will end after its done so we need service to play until we stop it
    }


    public void setCallbacks(ServiceCallbacks callbacks){
        serviceCallbacks = callbacks;
    }

    //for stopping progress bar wnen player start playing
    public interface ServiceCallbacks{
        void doSomething(boolean playerPlaying);
    }
    
    
    @Override
    public void onDestroy() {  //on destroy service
        super.onDestroy();
        if(player != null){
            if(player.isPlaying()){
                player.stop();
            }
            player.release(); //release all memory that player uses
          //  wifiLock.release();
            Log.i(TAG, "onDestroy:, service stopped...");
        }

        //Cancel the notification
        cancelNotification();
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory:, ");
        super.onLowMemory();
    }

    //Create Notification
    private void initNotification(){

        CharSequence tickertext = "My FM Station";
        //long when = System.currentTimeMillis(); //current time


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_fm_radio)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_fm_radio))
                        .setContentTitle(tickertext)
                        .setOngoing(true) //this method lets the notification to stay.
                        .setContentText("Work while Listen!");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }

    //Cancel Notification
    private void cancelNotification() {
        String notf = Context.NOTIFICATION_SERVICE;
        NotificationManager nNotificationManager = (NotificationManager) getSystemService(notf);
        nNotificationManager.cancel(NOTIFICATION_ID);
    }
}
