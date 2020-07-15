package com.mpaas.demo.artvc.artvc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alipay.mobile.artvc.report.ReportInterface;
import com.alipay.mobile.artvc.report.ReportType;
import com.alipay.mobile.artvc.report.params.PublishReportInfo;
import com.alipay.mobile.artvc.report.params.SubscribeReportInfo;
import com.mpaas.demo.artvc.R;

import static com.mpaas.demo.artvc.artvc.Constants.*;

public class ARTVCActivity extends AppCompatActivity {

    private String TAG = "mainActivity";

    private TextView idTextview;
    private Button createRoomButton;
    private Button joinRoomButton;
    private EditText roomIdET;
    private EditText rtokenET;

    //提示操作
    private Toast logToast;

    private boolean isCreateRoom = false;
    private SharedPreferences sharedPref;

    private static final String USERLEAVEHINT_ACTION = "com.alipay.mobile.framework.USERLEAVEHINT";
    private static final String BROUGHT_TO_FOREGROUND_ACTION = "com.alipay.mobile.framework.BROUGHT_TO_FOREGROUND";

    private String serverType;
    private String customServer;
    private String customUid;
    private String videoSource;
    private String bizName;
    private String subBiz;
    private String signature;
    private String workspaceId;

    private boolean autoPublish = false;
    private boolean autoSubscribe = false;
    private boolean videoCall = false;
    private boolean videoRecord = false;
    private int     videoResolution = RESOLUTION_540P;
    private int     videoFPS = 15;
    private boolean maxVideoBitrateModeDefault = true;
    private int     maxVideoBitrateValue = 0;
    private boolean videoHwAcceleration = true;
    private boolean audioBitrateModeDefault = true;
    private int     audioBitrateValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artvc);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        idTextview = (TextView) findViewById( R.id.id_content );

        createRoomButton = (Button) findViewById( R.id.create_room_button );
        joinRoomButton = (Button)findViewById( R.id.join_room_button );
        roomIdET = (EditText) findViewById( R.id.roomId );
        rtokenET = (EditText)findViewById( R.id.rtoken );
        rtokenET.addTextChangedListener( textWatcher );

        createRoomButton.setOnClickListener( createRoomListener );
        joinRoomButton.setOnClickListener( joinRoomListener );

        idTextview.setText( Constants.uid );

        init();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.connect_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected: item = " + item.toString() );
        // Handle presses on the action bar items.
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent( BROUGHT_TO_FOREGROUND_ACTION );
        sendBroadcast(i);
        getSharePrefParams();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent( USERLEAVEHINT_ACTION );
        sendBroadcast(i);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();

    }

    private void init() {
        initButtonStatus();
    }

    private void initButtonStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joinRoomButton.setEnabled( false );
            }
        });

    }

    private void updateButtonStatus(final Button button, final boolean enable ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled( enable );
            }
        });
    }



    private View.OnClickListener createRoomListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startCallActivity( null, null );
        }
    };

    private View.OnClickListener joinRoomListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startCallActivity( roomIdET.getText().toString(), rtokenET.getText().toString() );
        }
    };

    private void startCallActivity(String roomId, String rToken ) {
        Intent intent = new Intent( ARTVCActivity.this, CallActivity.class );
        intent.putExtra( SERVER_TYPE, serverType );
        intent.putExtra( CUSTOM_SERVER, customServer );
        intent.putExtra( CUSTOM_UID, customUid );
        intent.putExtra( VIDEO_SOURCE, videoSource );
        intent.putExtra( BIZ_NAME, bizName );
        intent.putExtra( SUB_BIZ, subBiz );
        intent.putExtra( SIGNATURE, signature );
        intent.putExtra( WORKSPACE_ID, workspaceId );
        intent.putExtra( AUTO_PUBLISH, autoPublish );
        intent.putExtra( AUTO_SUBSCRIBE, autoSubscribe );
        intent.putExtra( VIDEO_CALL, videoCall );
        intent.putExtra( VIDEO_RESOLUTION, videoResolution );
        intent.putExtra( VIDEO_FPS, videoFPS );
        intent.putExtra( MAX_VIDEO_BITRATE_MODE, maxVideoBitrateModeDefault );
        intent.putExtra( MAX_VIDEO_BITRATE_VALUE, maxVideoBitrateValue );
        intent.putExtra( VIDEO_HW_ACCELERATION, videoHwAcceleration );
        intent.putExtra( AUDIO_BITRATE_MODE, audioBitrateModeDefault );
        intent.putExtra( AUDIO_BITRATE_VALUE, audioBitrateValue );
        intent.putExtra( VIDEO_RECORD, videoRecord );

        if ( roomId == null && rToken == null ) {
            //create
            startActivity( intent );
        } else if ( roomId != null && rToken != null ) {
            //join
            intent.putExtra( ROOM_ID, roomId );
            intent.putExtra( ROOM_TOKEN, rToken );
            startActivity( intent );
        } else {
            //invalid params
            Toast( "startCallActivity failed with invalid params" );
        }

    }


    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            updateButtonStatus( joinRoomButton, !isCreateRoom && s.toString().trim().length() > 0 );
        }
    };

    private ReportInterface demoReport = new ReportInterface() {
        @Override
        public <T> void reportData(int type, T data) {
            if ( type == ReportType.TYPE_PUBLISH ) {
                PublishReportInfo info = (PublishReportInfo)data;
                Log.i(TAG, "reportData: type = " + type + ", info =" + info.toString() );
            } else if ( type == ReportType.TYPE_SUBSCRIBE ) {
                SubscribeReportInfo info = (SubscribeReportInfo)data;
                Log.i(TAG, "reportData: type = " + type + ", info =" + info.toString() );
            } else {
                Log.e(TAG, "reportData: error with invalid type" );
            }
        }
    };

    /**
     * 获取设置参数
     */
    private void getSharePrefParams() {
        if ( sharedPref != null ) {
            try {

                serverType = sharedPref.getString( getString(R.string.pref_server_key), getString(R.string.pref_server_default) );
                customServer = sharedPref.getString( getString(R.string.pref_custom_server_key), getString(R.string.pref_custom_server_default_value) );
                customUid = sharedPref.getString( getString( R.string.pref_custom_uid_key), "123456" );
                videoSource = sharedPref.getString( getString(R.string.pref_video_source_key), getString(R.string.pref_video_source_default) );
                bizName = sharedPref.getString( getString(R.string.pref_biz_name_key), getString(R.string.pref_biz_name_default_value) );
                subBiz = sharedPref.getString( getString(R.string.pref_sub_biz_key), getString(R.string.pref_sub_biz_default_value) );
                signature = sharedPref.getString( getString(R.string.pref_signature_key), getString(R.string.pref_signature_default_value) );
                workspaceId = sharedPref.getString( getString(R.string.pref_workspace_biz_key), getString(R.string.pref_workspace_biz_default_value) );

                autoPublish = sharedPref.getBoolean(getString(R.string.pref_auto_pub_key), false);
                autoSubscribe = sharedPref.getBoolean(getString(R.string.pref_auto_sub_key), false);
                videoCall = sharedPref.getBoolean(getString(R.string.pref_videocall_key), false);
                videoRecord = sharedPref.getBoolean( getString(R.string.pref_video_record_key), false );
                String videoResolutionPre = sharedPref.getString(getString(R.string.pref_resolution_key), "Default");

                videoResolution = RESOLUTION_540P;//默认是540p
                if ( videoResolutionPre.equals( "Default" ) || videoResolutionPre.equals( "540P" ) ) {
                    videoResolution = RESOLUTION_540P;
                } else if ( videoResolutionPre.equals( "720P" ) ) {
                    videoResolution = RESOLUTION_720P;
                } else if ( videoResolutionPre.equals( "360P" ) ) {
                    videoResolution = RESOLUTION_360P;
                } else if ( videoResolutionPre.equals( "1080P" ) ) {
                    videoResolution = RESOLUTION_1080P;
                }

                String cameraFpsPre = sharedPref.getString(getString(R.string.pref_fps_key), "Default");

                videoFPS = 15;
                if ( cameraFpsPre.equals( "30 fps" ) ) {
                    videoFPS = 30;
                }

                String maxVideoBitrateModePre = sharedPref.getString(getString(R.string.pref_maxvideobitrate_key), "Default");

                maxVideoBitrateModeDefault = true;
                if ( maxVideoBitrateModePre.equals( "Manual" ) ) {
                    maxVideoBitrateModeDefault = false;
                }

                maxVideoBitrateValue = Integer.parseInt( sharedPref.getString(getString(R.string.pref_maxvideobitratevalue_key), getString(R.string.pref_maxvideobitratevalue_default)) );
                videoHwAcceleration = sharedPref.getBoolean(getString(R.string.pref_hwcodec_key), false);
                String startAudioBitratePre = sharedPref.getString(getString(R.string.pref_startaudiobitrate_key), "Default");

                audioBitrateModeDefault = true;
                if ( startAudioBitratePre.equals( "Manual" ) ) {
                    audioBitrateModeDefault = false;
                }

                audioBitrateValue = Integer.parseInt( sharedPref.getString(getString(R.string.pref_startaudiobitratevalue_key), getString(R.string.pref_startaudiobitratevalue_default)) );
            } catch ( Exception e ) {
                Log.e(TAG, "getSharePrefParams: failed " + e.getMessage() );
            }

            Log.i(TAG, "getSharePrefParams: ");
        }
    }

    private void Toast(String msg) {
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }
}
