package com.mpaas.demo.artvc.artvc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.alipay.mobile.artvc.client.ARTVCView;
import com.alipay.mobile.artvc.constants.PublishEventCode;
import com.alipay.mobile.artvc.constants.PublishVideoSource;
import com.alipay.mobile.artvc.constants.SubscribeEventCode;
import com.alipay.mobile.artvc.constants.VideoProfile;
import com.alipay.mobile.artvc.engine.AlipayRtcEngine;
import com.alipay.mobile.artvc.engine.AlipayRtcEngineCustomPublishListener;
import com.alipay.mobile.artvc.engine.AlipayRtcEngineEventListener;
import com.alipay.mobile.artvc.engine.AlipayRtcEngineIMListener;
import com.alipay.mobile.artvc.engine.AlipayRtcEngineInviteListener;
import com.alipay.mobile.artvc.params.CreateRoomParams;
import com.alipay.mobile.artvc.params.FeedInfo;
import com.alipay.mobile.artvc.params.InviteInfo;
import com.alipay.mobile.artvc.params.InviteParams;
import com.alipay.mobile.artvc.params.JoinRoomParams;
import com.alipay.mobile.artvc.params.Msg4Receive;
import com.alipay.mobile.artvc.params.ParticipantInfo;
import com.alipay.mobile.artvc.params.ParticipantLeaveInfo;
import com.alipay.mobile.artvc.params.PublishConfig;
import com.alipay.mobile.artvc.params.ReplyOfInviteInfo;
import com.alipay.mobile.artvc.params.RoomInfo;
import com.alipay.mobile.artvc.params.SubscribeConfig;
import com.alipay.mobile.artvc.params.UnpublishConfig;
import com.alipay.mobile.artvc.params.UnsubscribeConfig;
import com.alipay.mobile.artvc.statistic.RealTimeStatisticReport;
import com.alipay.mobile.artvc.statistic.StatisticInfoForDebug;
import com.alipay.mobile.artvc.utilities.ServerAddressUtility;
import com.alipay.mobile.artvccore.api.enums.APScalingType;
import com.mpaas.demo.artvc.R;

import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class CallActivity extends AppCompatActivity {

    private final static String TAG = "CallActivity";

    private AlipayRtcEngine rtcEngine;

    private HandlerThread eventHandlerThread = null;
    private Handler eventHandler;
    private Handler handler;

    private PublishConfig publishConfig;

    //提示操作
    private Toast logToast;

    private final static int REQUEST_CODE = 1024;

    private String serverType = "Default";
    private String customServer = null;
    private String customUid = null;
    private String videoSource = null;
    private String bizName = null;
    private String subBiz = null;
    private String signature = null;
    private String workspaceId = null;

    private boolean autoPublish = false;
    private boolean autoSubscribe = false;
    private boolean videoCall = false;
    private int     videoResolution = Constants.RESOLUTION_540P;
    private int     videoFPS = 15;
    private boolean maxVideoBitrateModeDefault = true;
    private int     maxVideoBitrateValue = 0;
    private boolean videoHwAcceleration = true;
    private boolean audioBitrateModeDefault = true;
    private int     audioBitrateValue = 0;
    private boolean videoRecord = false;

    private String roomId;
    private String rtoken;

    private TextView roomIdTV;
    private TextView peerIdTV;
    private TextView rtokenTV;
    private Button exitBTN;
    private Button unpublishBTN;
    private Button publishBTN;
    private Button subscribeBTN;
    private Button unsubscribeBTN;
    private Button switchCameraBTN;
    private Button muteMicBTN;
    private Button changeResolutionBTN;
    private FrameLayout smallViewRight;
    private FrameLayout smallViewLeft;
    private FrameLayout mainView;
    private EditText inviteeUidTextEdit;
    private Button inviteBTN;

    private ParticipantInfo peerInfo = null;

    private boolean     micMuted = false;
    private boolean     isBlack = true;
    private byte[]      black = null;
    private byte[]      white = null;
    private int         index = 0;

    private PublishVideoSource publishVideoSource = PublishVideoSource.VIDEO_SOURCE_CAMERA;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call);

        inviteeUidTextEdit = findViewById( R.id.invitee_uid );
        inviteBTN = findViewById( R.id.invite_btn );

        roomIdTV = findViewById( R.id.roomId );
        peerIdTV = findViewById( R.id.peer_id );
        rtokenTV = findViewById( R.id.rtoken );
        exitBTN = findViewById( R.id.btn_exit );
        smallViewRight = findViewById( R.id.small_render_right );
        smallViewLeft = findViewById( R.id.small_render_left );
        mainView = findViewById( R.id.main_render );
        unpublishBTN = findViewById( R.id.unpublish_btn );
        publishBTN = findViewById( R.id.publish_btn );
        subscribeBTN = findViewById( R.id.subscribe_btn );
        unsubscribeBTN = findViewById( R.id.unsubscribe_btn );
        switchCameraBTN = findViewById( R.id.switch_camera_btn );
        muteMicBTN = findViewById( R.id.mute_mic_btn );
        changeResolutionBTN = findViewById( R.id.change_resolution_btn );

        inviteBTN.setOnClickListener( inviteBtnListener );

        exitBTN.setOnClickListener( exitListener );
        unpublishBTN.setOnClickListener( unpublishListener );
        publishBTN.setOnClickListener( publishListener );
        subscribeBTN.setOnClickListener( subscribeListener );
        unsubscribeBTN.setOnClickListener( unsubscribeListener );
        switchCameraBTN.setOnClickListener( switchCameraListener );
        muteMicBTN.setOnClickListener( muteMicListener );
        changeResolutionBTN.setOnClickListener( changeResolutionListener );

        eventHandlerThread = new HandlerThread( "rtc_engine_event_handler_thread" );
        eventHandlerThread.start();
        eventHandler = new Handler( eventHandlerThread.getLooper() );
        handler = new Handler();

        final Intent intent = getIntent();
        //获取参数配置
        serverType = getIntentExtra( intent, Constants.SERVER_TYPE, String.class, null );
        customServer = getIntentExtra( intent, Constants.CUSTOM_SERVER, String.class, null );
        customUid = getIntentExtra( intent, Constants.CUSTOM_UID, String.class, null );
        videoSource = getIntentExtra( intent, Constants.VIDEO_SOURCE, String.class, null );
        bizName = getIntentExtra( intent, Constants.BIZ_NAME, String.class, null );
        subBiz = getIntentExtra( intent, Constants.SUB_BIZ, String.class, null );
        signature = getIntentExtra( intent, Constants.SIGNATURE, String.class, null );
        workspaceId = getIntentExtra( intent, Constants.WORKSPACE_ID, String.class, null );

        autoPublish = getIntentExtra( intent, Constants.AUTO_PUBLISH, Boolean.class, false );
        autoSubscribe = getIntentExtra( intent, Constants.AUTO_SUBSCRIBE, Boolean.class, false );
        videoCall = getIntentExtra( intent, Constants.VIDEO_CALL, Boolean.class, true );
        videoResolution = getIntentExtra( intent, Constants.VIDEO_RESOLUTION, Integer.class, Constants.RESOLUTION_540P );
        videoFPS = getIntentExtra( intent, Constants.VIDEO_FPS, Integer.class, 15 );
        maxVideoBitrateModeDefault = getIntentExtra( intent, Constants.MAX_VIDEO_BITRATE_MODE, Boolean.class, true );
        maxVideoBitrateValue = getIntentExtra( intent, Constants.MAX_VIDEO_BITRATE_VALUE, Integer.class, 800 );
        videoHwAcceleration = getIntentExtra( intent, Constants.VIDEO_HW_ACCELERATION, Boolean.class, true );
        audioBitrateModeDefault = getIntentExtra( intent, Constants.AUDIO_BITRATE_MODE, Boolean.class, true );
        audioBitrateValue = getIntentExtra( intent, Constants.MAX_VIDEO_BITRATE_VALUE, Integer.class, 32 );

        videoRecord = getIntentExtra( intent, Constants.VIDEO_RECORD, Boolean.class, false );

        roomId = getIntentExtra( intent, Constants.ROOM_ID, String.class, null );
        rtoken = getIntentExtra( intent, Constants.ROOM_TOKEN, String.class, null );

        rtcEngine = AlipayRtcEngine.getInstance( this );
        rtcEngine.setRtcListenerAndHandler( engineEventListener, eventHandler );
        rtcEngine.setImListener( imListener );
        rtcEngine.setInviteListener( inviteListener );
        rtcEngine.setCustomPublishListener( customPublishListener );

        /*String serverAddr = null;
        if ( serverType.equalsIgnoreCase( "Default" ) || serverType.equalsIgnoreCase( "Online" ) ) {
            serverAddr = ServerAddressUtility.SERVER_ONLINE;
        } else if ( serverType.equalsIgnoreCase( "Pre" )) {
            serverAddr = ServerAddressUtility.SERVER_PRE;
        } else if ( serverType.equalsIgnoreCase( "Test" )) {
            serverAddr = ServerAddressUtility.SERVER_TEST;
        } else if ( serverType.equalsIgnoreCase( "Dev" )) {
            serverAddr = ServerAddressUtility.SERVER_DEV;
        } else if ( serverType.equalsIgnoreCase( "Sandbox" )) {
            serverAddr = ServerAddressUtility.SERVER_SANDBOX;
        } else if ( serverType.equalsIgnoreCase( "Custom" )) {
            serverAddr = customServer;
        }
        rtcEngine.setServerAddr( serverAddr );*/

        rtcEngine.setAutoPublishSubscribe( autoPublish, autoSubscribe );
        configPublishConfig();
        rtcEngine.configAutoPublish( publishConfig );

        if ( roomId == null ) {
            createRoom();
        } else {
            joinRoom();
        }

        if ( videoCall && publishVideoSource == PublishVideoSource.VIDEO_SOURCE_CUSTOM ) {
            initYUVData();
            handler.postDelayed( dataRunnable, 1000 );
        }

    }

    private void createRoom() {
        CreateRoomParams createRoomParams = new CreateRoomParams();
        createRoomParams.uid = customUid;
        createRoomParams.bizName = bizName;
        createRoomParams.subBiz = subBiz;
        createRoomParams.type = CreateRoomParams.TYPE_RTC;
        createRoomParams.signature = signature;
        createRoomParams.worksapceId = workspaceId;
        createRoomParams.appid = subBiz;
        createRoomParams.extraInfo = null;
        createRoomParams.extraInfo = new JSONObject();
        createRoomParams.extraInfo.put( "defaultRecord", videoRecord );

        rtcEngine.createRoom( createRoomParams );
    }

    private void joinRoom() {
        JoinRoomParams joinRoomParams = new JoinRoomParams();
        joinRoomParams.uid = customUid;
        joinRoomParams.bizName = bizName;
        joinRoomParams.subBiz = subBiz;
        joinRoomParams.signature = signature;
        joinRoomParams.roomId = roomId;
        joinRoomParams.rtoken = rtoken;
        joinRoomParams.envType = 0;
        joinRoomParams.worksapceId = workspaceId;
        joinRoomParams.appid = subBiz;

        rtcEngine.joinRoom( joinRoomParams );

        roomIdTV.setText( roomId );
        rtokenTV.setText( rtoken );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data );
        super.onActivityResult(requestCode, resultCode, data);
        if ( rtcEngine != null ) {
            rtcEngine.onActivityResult( requestCode, resultCode, data );
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
        if ( rtcEngine != null ) {
            rtcEngine.leaveRoom();
        }

        if ( videoCall && publishVideoSource == PublishVideoSource.VIDEO_SOURCE_CUSTOM ) {
            handler.removeCallbacks( dataRunnable );
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed: ");
        super.onBackPressed();
        if ( rtcEngine != null ) {
            rtcEngine.leaveRoom();
        }

        if ( videoCall && publishVideoSource == PublishVideoSource.VIDEO_SOURCE_CUSTOM ) {
            handler.removeCallbacks( dataRunnable );
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    private View.OnClickListener publishListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                rtcEngine.publish( publishConfig );
            }
        }
    };

    private View.OnClickListener unpublishListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                UnpublishConfig unpublishConfig = new UnpublishConfig();
                unpublishConfig.audioSource = publishConfig.audioSource;
                unpublishConfig.videoSource = publishConfig.videoSource;

                rtcEngine.unpublish( unpublishConfig );
            }
        }
    };

    private View.OnClickListener subscribeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                SubscribeConfig subscribeConfig = new SubscribeConfig();
                if ( peerInfo != null && peerInfo.feedList.size() > 0 ) {
                    subscribeConfig.info = peerInfo.feedList.get(0);
                    rtcEngine.subscribe( subscribeConfig );
                }
            }
        }
    };

    private View.OnClickListener unsubscribeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                UnsubscribeConfig unsubscribeConfig = new UnsubscribeConfig();
                if ( peerInfo != null && peerInfo.feedList.size() > 0 ) {
                    unsubscribeConfig.feedInfo = peerInfo.feedList.get(0);
                    rtcEngine.unsubscribe( unsubscribeConfig );
                }
            }
        }
    };

    private View.OnClickListener exitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                rtcEngine.leaveRoom();
                finish();
            }
        }
    };

    private View.OnClickListener switchCameraListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                rtcEngine.switchCamera();
            }
        }
    };

    private View.OnClickListener muteMicListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( rtcEngine != null ) {
                micMuted = ! micMuted;
                rtcEngine.muteAllLocalAudio( micMuted );
            }
        }
    };

    private View.OnClickListener changeResolutionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( getVideoResolution() ) {
                changeResolutionBTN.setText( "RESOLUTIONDOWN" );
            } else {
                changeResolutionBTN.setText( "RESOLUTIONUP" );
            }

            if ( rtcEngine != null ) {
                if ( videoResolution == Constants.RESOLUTION_360P ) {
                    rtcEngine.updateVideoProfile( VideoProfile.PROFILE_360_640P_15, 0);
                } else {
                    rtcEngine.updateVideoProfile( VideoProfile.PROFILE_720_1280P_15, 0);
                }
            }
        }
    };

    private View.OnClickListener inviteBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String inviteeUid = inviteeUidTextEdit.getText().toString();
            if ( inviteeUid != null && inviteeUid.length() > 0 && rtcEngine != null) {
                InviteParams inviteParams = new InviteParams();
                inviteParams.inviteeUid = inviteeUid;
                rtcEngine.invite( inviteParams );
            }
        }
    };

    @TargetApi(23)
    private void requestPermission() {
        requestPermissions( new String[]{ RECORD_AUDIO, CAMERA, READ_PHONE_STATE, WRITE_EXTERNAL_STORAGE }, REQUEST_CODE );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d( TAG, "onRequestPermissionsResult: " + requestCode );
        rtcEngine.onRequestPermissionsResult( requestCode, permissions, grantResults );
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

    }

    private AlipayRtcEngineEventListener engineEventListener = new AlipayRtcEngineEventListener()  {
        @Override
        public void onParticipantsEnter(final List<ParticipantInfo> infos ) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for ( ParticipantInfo info : infos ) {
                        Log.i(TAG, "onParticipantsEnter: " + info.toString() );
                        if ( peerInfo == null ) {
                            peerInfo = info;
                            peerIdTV.setText( peerInfo.uid );
                        } else {
                            Toast( "room is full!" );
                        }
                        break;
                    }
                }
            });
            Toast( "user in, " + infos.get(0).uid );

        }

        @Override
        public void onParticipantsLeave(final List<ParticipantLeaveInfo> uidList) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for ( ParticipantLeaveInfo info : uidList ) {
                        Log.i( TAG, "onParticipantsLeave, " + info.uid + ", exitType = " + info.exitType );
                        if ( peerInfo != null && info.uid.equals( peerInfo.uid ) ) {
                            peerInfo = null;
                        }
                    }
                }
            });

            Toast( "user out, " + uidList.get(0) );
        }

        @Override
        public void onPublishNotify(final FeedInfo info ) {
            Log.i( TAG, "onPublishNotify, " + info.toString() );
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if ( peerInfo != null && peerInfo.uid.equals( info.uid ) ) {
                        peerInfo.feedList.add( info );
                    }
                }
            });

            Toast( "publish notify, " + info.feedId );

        }

        @Override
        public void onError(int errorCode, String errorMessage, Bundle extra) {
            Log.e( TAG, "onError, errorCode = " + errorCode + ", errorMessage = " + errorMessage + ", extra = " + extra );
            Toast( "Error, code = " + errorCode );
        }

        @Override
        public void onCommonEvent(int i, String s, Bundle bundle) {
            Log.i(TAG, "onCommonEvent: eventCode = " + i + ", eventMessage = " + s + ", extra = " + bundle );
        }

        @Override
        public void onRoomInfo( final RoomInfo info) {
            if ( info != null ) {
                Log.i(TAG, "onRoomInfo: " + info.toString());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    roomId = info.roomId;
                    rtoken = info.rtoken;
                    roomIdTV.setText( roomId );
                    rtokenTV.setText( rtoken );
                }
            });

            String result;
            if ( info != null ) {
                result = "create room success!";
            } else {
                result = "create room failed!";
            }
            Toast( result );
        }

        @Override
        public void onEnterRoom(int result) {
            Log.i( TAG, "onEnterRoom: " + result );
            Toast( "joinRoom result = " + result );
        }

        @Override
        public void onLeaveRoom(int reason) {
            Log.i(TAG, "onLeaveRoom: " + reason );
            Toast( "leave room" );
        }

        @Override
        public void onInviteReply(ReplyOfInviteInfo replyOfInviteInfo) {
            Log.i(TAG, "onInviteReply: " + replyOfInviteInfo.toString() );
        }

        @Override
        public void onRecordInfo(String s) {
            Log.i(TAG, "onRecordInfo: " + s);
        }

        /*@Override*/
        public void onRecordFinished(String recordId) {

        }

        @Override
        public void onRemoteViewFirstFrame(final FeedInfo info, final ARTVCView _view ) {
            Log.i(TAG, "onRemoteViewFirstFrame: " + info.toString() );
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mainView.getChildCount() <= 0 ) {
                        _view.setZOrderMediaOverlay(false);
                        mainView.setVisibility(View.VISIBLE);
                        mainView.addView(_view);
                        _view.setAPScalingType(APScalingType.SCALE_ASPECT_FILL);
                        _view.requestLayout();
                    } else if ( smallViewLeft.getChildCount() <= 0 ) {
                        _view.setZOrderMediaOverlay(true);
                        smallViewLeft.setVisibility(View.VISIBLE);
                        smallViewLeft.addView(_view);
                        _view.setAPScalingType(APScalingType.SCALE_ASPECT_FILL);
                        _view.requestLayout();

                    }
                }
            });
            Toast( info.uid + " view display" );
        }

        @Override
        public void onRemoteViewStop(FeedInfo feedInfo, ARTVCView view ) {
            Log.i(TAG, "onRemoteViewStop: feedInfo = " + feedInfo );
        }

        @Override
        public void onCurrentNetworkType(int type) {
            Log.i( TAG, "onCurrentNetworkType, type = " + type );
            Toast( "onCurrentNetworkType = " + type );
        }

        @Override
        public void onCurrentAudioPlayoutMode(int mode) {
            Log.i( TAG, "onCurrentAudioPlayoutMode, mode = " + mode );
            Toast( "onCurrentAudioPlayoutMode = " + mode );
        }

        @Override
        public void onBandwidthImportanceChangeNotify(boolean isLow, double currentBandwidth, FeedInfo feedInfo) {
            Log.i(TAG, "onBandwidthImportanceChangeNotify: isLow = " + isLow + ", currentBandwidth = " + currentBandwidth + ", feedInfo = " + feedInfo.toString() );
            if ( isLow ) {
                Toast( "当前用户网络较差，当前带宽: " + currentBandwidth );
            } else {
                Toast( "当前用户网络流畅，当前带宽: " + currentBandwidth );
            }

        }

        @Override
        public void onSnapShotComplete(final Bitmap image, FeedInfo feedInfo) {
            Log.i(TAG, "onSnapShotComplete: image = " + image + ", feedInfo = " + feedInfo.toString() );
        }

        @Override
        public void onStatisticDebugInfo(StatisticInfoForDebug infoForDebug, FeedInfo feedInfo) {
            //Log.i(TAG, "onStatisticDebugInfo: inforDebug = " + infoForDebug.toString() + ", feedInfo = " + feedInfo.toString() );
        }

        @Override
        public void onRealTimeStatisticInfo(RealTimeStatisticReport report, FeedInfo feedInfo) {
            //Log.i(TAG, "onRealTimeStatisticInfo: report = " + report.toString() + ", feedInfo = " + feedInfo.toString() );
        }

        @Override
        public void onCameraPreviewInfo(final ARTVCView view ) {
            Log.i(TAG, "onCameraPreviewInfo: " + view );
            if ( view != null ) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.setZOrderMediaOverlay( true );
                        smallViewRight.setVisibility(View.VISIBLE);
                        smallViewRight.addView(view);
                        //View view_1 = new View( ARTVCActivity.this );
                        //view_1.setBackgroundColor(Color.WHITE );
                        //addView( view_1, -1, new FrameLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        view.setAPScalingType(APScalingType.SCALE_ASPECT_FILL);
                        view.requestLayout();
                    }
                });
            }
        }

        @Override
        public void onCameraPreviewFirstFrame() {
            Log.i(TAG, "onCameraPreviewFirstFrame: ");
        }

        @Override
        public void onCameraPreviewStop() {
            Log.i(TAG, "onCameraPreviewStop: ");
        }

        @Override
        public void onPublishEvent(PublishConfig config, PublishEventCode eventCode, String eventDetail, FeedInfo feedInfo) {
            Log.i(TAG, "onPublishEvent: " + config + ", code = " + eventCode + ", detail = " + eventDetail + ", feedInfo = " + feedInfo );
            if ( eventCode.equals( PublishEventCode.PUBLISH_SUCCESS ) ) {
                //updateButtonStatus( publishButton, false );
                //updateButtonStatus( unpublishButton, true );
            } else if ( eventCode.equals( PublishEventCode.PUBLISH_FAIL ) || eventCode.equals( PublishEventCode.PUBLISH_DISCONNECT ) ) {
                //updateButtonStatus( unpublishButton, false );
                //updateButtonStatus( publishButton, true );
            } else if ( eventCode.equals( PublishEventCode.PUBLISH_START ) ) {
                //snapFeedInfo = feedInfo;
            }

            Toast( "publish event, " + eventCode );
        }

        @Override
        public void onUnpublishEvent(UnpublishConfig config, PublishEventCode eventCode, String eventDetail) {
            Log.i(TAG, "onUnpublishEvent: " + config + ", code = " + eventCode + ", detail = " + eventDetail );

            //updateButtonStatus( publishButton, true );
            //updateButtonStatus( unpublishButton, false );
            Toast( "unpublish event, " + eventCode );
        }

        @Override
        public void onSubscribeEvent(final FeedInfo publisherInfo, SubscribeEventCode eventCode, String eventDetail, final ARTVCView _view) {
            Log.i(TAG, "onSubscribeEvent: " + publisherInfo.toString() + ", code = " + eventCode + ", detail = " + eventDetail + ", view = " + _view );
            switch ( eventCode ) {
                case SUBSCRIBE_START:
                case SUBSCRIBE_FAIL:
                    break;
                case SUBSCRIBE_SUCCESS:
                    //snapFeedInfo = publisherInfo;
                    break;
                case SUBSCRIBE_DISCONNECT:
                    //关闭订阅的流，同时取消订阅
                    break;
            }

            Toast( "subscribe event, " + eventCode );
        }

        @Override
        public void onUnsubscribeEvent(FeedInfo publisherInfo, SubscribeEventCode eventCode, String eventDetail) {
            Log.i(TAG, "onUnsubscribeEvent: " + publisherInfo.toString() + ", code = " + eventCode + ", detail = " + eventDetail );

            Toast( "unsubscribe evnet, " + eventCode );
        }

        @Override
        public void onUnpublishNotify(FeedInfo info) {
            Log.i( TAG, "onUnpublishNotify: " + info.toString() );

            if ( info.uid.equals( peerInfo.uid ) ) {
                //TODO:取消订阅
                unsubscribeListener.onClick( null );
                peerInfo.feedList.clear();
            }

            Toast( "unpublish notify, " + info.feedId );

        }

        @Override
        public void onSubscribeNotify(FeedInfo publisherInfo, String subscribeInfo) {
            Log.i(TAG, "onSubscribeNotify: publisherInfo = " + publisherInfo.toString() + ", subscriberInfo = " + subscribeInfo.toString() );
        }

        @Override
        public void onUnsubscribeNotify(FeedInfo publisherInfo, String subscribeInfo) {
            Log.i(TAG, "onUnubscribeNotify: publisherInfo = " + publisherInfo.toString() + ", subscriberInfo = " + subscribeInfo.toString() );
        }
    };

    private AlipayRtcEngineIMListener imListener = new AlipayRtcEngineIMListener() {
        @Override
        public void onMsgReceive(Msg4Receive msg4Receive) {
            Log.i( TAG, "onMsgReceive, " + msg4Receive.toString() );
            Toast( msg4Receive.toString() );
        }

        @Override
        public void onMsgSend(int error, String errorMessage, long msgId) {
            Log.i( TAG, "onMsgSend, error = " + error + ", errorMessage = " + errorMessage + ", msgId = " + msgId );
            Toast( "onMsgSend, error = " + error + ", errorMessage = " + errorMessage + ", msgId = " + msgId );
        }
    };

    private AlipayRtcEngineInviteListener inviteListener = new AlipayRtcEngineInviteListener() {
        @Override
        public void onInviteResponse(String inviteTaskId, int code, String msg) {
            Log.i(TAG, "onInviteResponse: inviteTaskId = " + inviteTaskId + ", code = " + code + ", msg = " + msg );
            Toast( "onInviteResponse: inviteTaskId = " + inviteTaskId + ", code = " + code + ", msg = " + msg );
        }

        @Override
        public void onInviteNotify(InviteInfo inviteInfo) {
            Log.i(TAG, "onInviteNotify: " + inviteInfo.toString() );
            Toast( "onInviteNotify: " + inviteInfo.toString() );
        }

        @Override
        public void onReplyResponse(String inviteTaskId, int code, String msg) {
            Log.i(TAG, "onReplyResponse: inviteTaskId = " + inviteTaskId + ", code = " + code + ", msg = " + msg );
            Toast( "onReplyResponse: inviteTaskId = " + inviteTaskId + ", code = " + code + ", msg = " + msg );
        }

        @Override
        public void onReplyOfInviteNotify(ReplyOfInviteInfo replyInfo) {
            Log.i(TAG, "onReplyOfInviteNotify: " + replyInfo.toString() );
            Toast( "onReplyOfInviteNotify: " + replyInfo.toString() );
        }
    };

    private void Toast(String msg) {
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }


    private <T> T getIntentExtra(Intent intent, String key, Class<T> type, Object defaultValue ) {
        T result = null;
        try {
            if (type == String.class) {
                result = (T)intent.getStringExtra( key );
            } else if ( type == Boolean.class ) {
                result = (T)((Boolean)intent.getBooleanExtra( key, (boolean)defaultValue ));
            } else if ( type == Integer.class ) {
                result = (T) ((Integer)intent.getIntExtra( key, (int)defaultValue ));
            }
        } catch ( Exception e ) {
            Log.e( TAG, "get intent value from " + key + " failed, " + e.getMessage() );
            result = (T)defaultValue;
        }

        return result;
    }

    private boolean getVideoResolution() {
        if ( videoResolution == Constants.RESOLUTION_720P ) {
            videoResolution = Constants.RESOLUTION_360P;
            return false;
        }
        videoResolution = Constants.RESOLUTION_720P;
        return true;

    }

    private void configPublishConfig() {
        publishConfig = new PublishConfig();
        parsePublishVideoSource();
        publishConfig.videoSource = videoCall ? publishVideoSource : PublishVideoSource.VIDEO_SOURCE_NULL;
        if ( maxVideoBitrateModeDefault ) {
            if ( videoFPS == 15 ) {
                if (videoResolution == Constants.RESOLUTION_720P) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_720_1280P_15;
                } else if ( videoResolution == Constants.RESOLUTION_540P ) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_540_960P_15;
                } else if ( videoResolution == Constants.RESOLUTION_360P ) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_360_640P_15;
                } else if ( videoResolution == Constants.RESOLUTION_1080P ) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_1080_1920P_15;
                }
            } else {
                if (videoResolution == Constants.RESOLUTION_720P) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_720_1280P_30;
                } else if ( videoResolution == Constants.RESOLUTION_540P ) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_540_960P_30;
                } else if ( videoResolution == Constants.RESOLUTION_360P ) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_360_640P_30;
                } else if ( videoResolution == Constants.RESOLUTION_1080P ) {
                    publishConfig.videoProfile = VideoProfile.PROFILE_1080_1920P_30;
                }
            }
        } else {
            publishConfig.videoProfile = VideoProfile.PROFILE_CUSTOM;
            publishConfig.videoCustomFps = videoFPS;
            publishConfig.videoCustomMaxBitrate = maxVideoBitrateValue;
            if (videoResolution == Constants.RESOLUTION_720P) {
                publishConfig.videoCustomWidth = 720;
                publishConfig.videoCustomHeight = 1280;
            } else if ( videoResolution == Constants.RESOLUTION_540P ) {
                publishConfig.videoCustomWidth = 540;
                publishConfig.videoCustomHeight = 960;
            } else if ( videoResolution == Constants.RESOLUTION_360P ) {
                publishConfig.videoCustomWidth = 360;
                publishConfig.videoCustomHeight = 640;
            } else if ( videoResolution == Constants.RESOLUTION_1080P ) {
                publishConfig.videoCustomWidth = 1080;
                publishConfig.videoCustomHeight = 1920;
            }
        }
    }

    private void parsePublishVideoSource() {
        //默认使用摄像头
        if ( videoSource != null ) {
            if ( videoSource.equals( "ScreenShare" ) ) {
                publishVideoSource = PublishVideoSource.VIDEO_SOURCE_SCREEN;
            } else if ( videoSource.equals( "Custom" ) ) {
                publishVideoSource = PublishVideoSource.VIDEO_SOURCE_CUSTOM;
            }
        }
    }

    private void initYUVData() {
        int imageLength = 1920 * 1080 * 3 / 2;
        black = new byte[ imageLength ];
        white = new byte[ imageLength ];

        for ( int i = 0; i < imageLength; i++ ) {
            black[i] = (byte)0x80;
            white[i] = (byte)0xff;
        }
    }

    //自定义推流，使用yuv数据模拟
    private Runnable dataRunnable = new Runnable() {
        @Override
        public void run() {
            if ( rtcEngine != null ) {
                if ( isBlack ) {
                    rtcEngine.pushCustomVideoData( black, 1920, 1080, 90 );
                } else {
                    rtcEngine.pushCustomVideoData( white, 1920, 1080, 90 );
                }
                if ( index % 25 == 0 ) {
                    index = 0;
                    isBlack = !isBlack;
                }
                index++;
            }
            handler.postDelayed( this, 40 );
        }
    };

    private AlipayRtcEngineCustomPublishListener customPublishListener = new AlipayRtcEngineCustomPublishListener() {
        @Override
        public void onCustomPublishPreviewInfo(PublishVideoSource videoSource, final ARTVCView view) {
            Log.i(TAG, "onCustomPublishPreviewInfo: " + videoSource + ", " + view );

            if ( view != null ) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.setZOrderMediaOverlay( true );
                        smallViewRight.setVisibility(View.VISIBLE);
                        smallViewRight.addView(view);
                        //View view_1 = new View( MainActivity.this );
                        //view_1.setBackgroundColor(Color.WHITE );
                        //addView( view_1, -1, new FrameLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        view.setAPScalingType(APScalingType.SCALE_ASPECT_FILL);
                        view.requestLayout();
                    }
                });
            }
        }

        @Override
        public void onCustomPublishPreviewFirstFrame(PublishVideoSource videoSource) {
            Log.i(TAG, "onCustomPublishPreviewFirstFrame: " + videoSource );
        }

        @Override
        public void onCustomPublishPreviewStop(PublishVideoSource videoSource) {
            Log.i(TAG, "onCustomPublishPreviewStop: " + videoSource );
        }
    };
}
