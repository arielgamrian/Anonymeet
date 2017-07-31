package com.example.gamrian.anonymeet.FireBaseChat;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

/**
 * Created by Or on 16/07/2017.
 */

public class CustomRecyclerViewLayoutManager extends LinearLayoutManager {

    boolean scrollEnabled;

    public CustomRecyclerViewLayoutManager(Context context) {
        super(context);
        scrollEnabled = true;
    }

    @Override
    public boolean canScrollVertically() {
        return scrollEnabled && super.canScrollVertically();
    }
}
