package com.mpaas.demo.artvc.artvc;

public class Constants {
    public static final String uid = "" + System.currentTimeMillis() % 10000;

    //配置参数
    public final static String AUTO_PUBLISH = "pref_auto_publish";
    public final static String AUTO_SUBSCRIBE = "pref_auto_subscribe";
    public final static String VIDEO_CALL = "pref_video_call";
    public final static String VIDEO_SOURCE = "pref_video_source_key";
    public final static String VIDEO_RESOLUTION = "pref_video_resolution";
    public final static String VIDEO_FPS = "pref_video_fps";
    public final static String MAX_VIDEO_BITRATE_MODE = "pref_max_video_bitrate_mode";
    public final static String MAX_VIDEO_BITRATE_VALUE = "pref_max_video_bitrate_value";
    public final static String VIDEO_HW_ACCELERATION = "pref_video_hw_acceleration";
    public final static String AUDIO_BITRATE_MODE = "pref_audio_bitrate_mode";
    public final static String AUDIO_BITRATE_VALUE = "pref_audio_bitrate_mode";
    public final static String VIDEO_RECORD = "pref_video_record_key";

    public final static String ROOM_ID = "pref_room_id";
    public final static String ROOM_TOKEN = "pref_room_token";

    public final static String SERVER_TYPE = "pref_server_key";
    public final static String CUSTOM_SERVER = "pref_custom_server_key";
    public final static String CUSTOM_UID = "pref_custom_uid_key";
    public final static String BIZ_NAME = "pref_biz_name_key";
    public final static String SUB_BIZ = "pref_sub_biz_key";
    public final static String SIGNATURE = "pref_signature_key";
    public final static String WORKSPACE_ID = "pref_workspace_biz_key";

    //分辨率
    public final static int RESOLUTION_720P = 0;
    public final static int RESOLUTION_540P = 1;
    public final static int RESOLUTION_360P = 2;
    public final static int RESOLUTION_1080P = 3;
}
