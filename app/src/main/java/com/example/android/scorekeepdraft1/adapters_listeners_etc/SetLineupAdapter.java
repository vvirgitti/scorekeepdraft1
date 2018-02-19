package com.example.android.scorekeepdraft1.adapters_listeners_etc;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.android.scorekeepdraft1.R;
import com.example.android.scorekeepdraft1.adapters_listeners_etc.helper.ItemTouchHelperAdapter;
import com.example.android.scorekeepdraft1.adapters_listeners_etc.helper.ItemTouchHelperViewHolder;
import com.example.android.scorekeepdraft1.adapters_listeners_etc.helper.OnStartDragListener;
import com.example.android.scorekeepdraft1.objects.Player;

import java.util.Collections;
import java.util.List;

/**
 * Created by Eddie on 02/09/2017.
 */

public class SetLineupAdapter extends RecyclerView.Adapter<SetLineupAdapter.LineupListViewHolder>
        implements ItemTouchHelperAdapter {

    private List<Player> mPlayerList;
    private Context mContext;
    private boolean isBench;
    private boolean genderSettingsOff;
    private int colorMale;
    private int colorFemale;
    private final OnStartDragListener mDragStartListener;

    public SetLineupAdapter(List<Player> list, Context context, boolean isBench, int genderSorter,
                            OnStartDragListener dragListener) {
        this.mDragStartListener = dragListener;
        this.mPlayerList = list;
        this.mContext = context;
        this.isBench = isBench;
        genderSettingsOff = genderSorter == 0;
        if (genderSettingsOff) {
            colorMale = Color.TRANSPARENT;
            colorFemale = Color.TRANSPARENT;
        } else {
            colorMale = ContextCompat.getColor(context, R.color.male);
            colorFemale = ContextCompat.getColor(context, R.color.female);
        }
    }

    @Override
    public SetLineupAdapter.LineupListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lineup, parent, false);

        return new LineupListViewHolder(frameLayout);
    }

    @Override
    public void onBindViewHolder(final SetLineupAdapter.LineupListViewHolder holder, int position) {
        Player player = mPlayerList.get(position);
        String name = player.getName();
        int gender = player.getGender();

        if (gender == 0) {
            holder.mFrameLayout.setBackgroundColor(colorMale);
        } else {
            holder.mFrameLayout.setBackgroundColor(colorFemale);
        }

        if(isBench) {
            String benchPlayer = "B:   " + name;
            holder.mTextView.setText(benchPlayer);
        } else {
            String positionText = (position + 1) + ". " + name;
            holder.mTextView.setText(positionText);
        }

        holder.mFrameLayout.setTag(position);
//        holder.mFrameLayout.setOnTouchListener(this);
        holder.mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPlayerList.size();
    }

//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                ClipData data = ClipData.newPlainText("", "");
//                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    v.startDragAndDrop(data, shadowBuilder, v, 0);
//                } else {
//                    v.startDrag(data, shadowBuilder, v, 0);
//                }
//                return true;
//        }
//        return false;
//    }



    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mPlayerList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {

    }

    public boolean changeColors(boolean genderSettingsOn){
        if (genderSettingsOn) {
            if (!genderSettingsOff) {
                return false;
            }
            colorMale = ContextCompat.getColor(mContext, R.color.male);
            colorFemale = ContextCompat.getColor(mContext, R.color.female);
            genderSettingsOff = false;
        } else {
            if (genderSettingsOff) {
                return false;
            }
            colorMale = Color.TRANSPARENT;
            colorFemale = Color.TRANSPARENT;
            genderSettingsOff = true;
        }
        return true;
    }

    public List<Player> getPlayerList() {return mPlayerList;    }

    void updateList(List<Player> list) {
        this.mPlayerList = list;
    }


    public DragListener getDragInstance() {
            return new DragListener();
    }

    static class LineupListViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {
        FrameLayout mFrameLayout;
        TextView mTextView;


        LineupListViewHolder(View itemView) {
            super(itemView);
            mFrameLayout = (FrameLayout) itemView;
            mTextView = mFrameLayout.findViewById(R.id.lineup_text);
        }

        @Override
        public void onItemSelected() {

        }

        @Override
        public void onItemClear() {

        }
    }
}