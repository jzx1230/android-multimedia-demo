package com.mpaas.demo.artvc.artvc;

import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by baishui on 2019/9/9.
 */

public class RemoteView {
    public FrameLayout view;
    public TextView name;

    public RemoteView ( FrameLayout _view, TextView _name ) {
        view = _view;
        name = _name;
    }

    public void setVisibility( int visibility ) {
        view.setVisibility( visibility );
        name.setVisibility( visibility );
    }
}
