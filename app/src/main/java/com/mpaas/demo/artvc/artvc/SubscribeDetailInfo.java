package com.mpaas.demo.artvc.artvc;

import com.alipay.mobile.artvc.params.FeedInfo;
import com.alipay.mobile.artvc.params.ParticipantInfo;

/**
 * Created by baishui on 2019/9/5.
 */

public class SubscribeDetailInfo {
    public FeedInfo feedInfo;
    public boolean isSubscribed = false;

    public SubscribeDetailInfo( FeedInfo info ) {
        feedInfo = info;
    }

    public SubscribeDetailInfo( FeedInfo info, boolean isSubscribed ) {
        feedInfo = info;
        this.isSubscribed = isSubscribed;
    }
}
