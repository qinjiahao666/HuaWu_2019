package com.example.zqvideolibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Nathen
 * On 2016/04/18 16:15
 */
public class ZQVideoPlayerStandard extends ZQVideoPlayer {

    protected static Timer DISMISS_CONTROL_VIEW_TIMER;

    public ImageView backButton;
    public ProgressBar bottomProgressBar, loadingProgressBar;
    public TextView titleTextView;
    public ImageView thumbImageView;
    public ImageView tinyBackImageView;
    public LinearLayout batteryTimeLayout;
    public ImageView batteryLevel;
    public TextView videoCurrentTime;
    public TextView replayTextView;
    public TextView clarity;
    public PopupWindow clarityPopWindow;
    public TextView mRetryBtn;
    public LinearLayout mRetryLayout;

    protected DismissControlViewTimerTask mDismissControlViewTimerTask;
    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;
    protected Dialog mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;
    protected TextView mDialogVolumeTextView;
    protected ImageView mDialogVolumeImageView;
    protected Dialog mBrightnessDialog;
    protected ProgressBar mDialogBrightnessProgressBar;
    protected TextView mDialogBrightnessTextView;
    private boolean brocasting = false;
    private BroadcastReceiver battertReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                int percent = level * 100 / scale;
                if (percent < 15) {
                    batteryLevel.setBackgroundResource(R.drawable.zq_battery_level_10);
                } else if (percent >= 15 && percent < 40) {
                    batteryLevel.setBackgroundResource(R.drawable.zq_battery_level_30);
                } else if (percent >= 40 && percent < 60) {
                    batteryLevel.setBackgroundResource(R.drawable.zq_battery_level_50);
                } else if (percent >= 60 && percent < 80) {
                    batteryLevel.setBackgroundResource(R.drawable.zq_battery_level_70);
                } else if (percent >= 80 && percent < 95) {
                    batteryLevel.setBackgroundResource(R.drawable.zq_battery_level_90);
                } else if (percent >= 95 && percent <= 100) {
                    batteryLevel.setBackgroundResource(R.drawable.zq_battery_level_100);
                }
                getContext().unregisterReceiver(battertReceiver);
                brocasting = false;
            }
        }
    };

    public ZQVideoPlayerStandard(Context context) {
        super(context);
    }

    public ZQVideoPlayerStandard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        batteryTimeLayout = (LinearLayout) findViewById(R.id.battery_time_layout);
        bottomProgressBar = (ProgressBar) findViewById(R.id.bottom_progress);
        titleTextView = (TextView) findViewById(R.id.title);
        backButton = (ImageView) findViewById(R.id.back);
        thumbImageView = (ImageView) findViewById(R.id.thumb);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loading);
        tinyBackImageView = (ImageView) findViewById(R.id.back_tiny);
        batteryLevel = (ImageView) findViewById(R.id.battery_level);
        videoCurrentTime = (TextView) findViewById(R.id.video_current_time);
        replayTextView = (TextView) findViewById(R.id.replay_text);
        clarity = (TextView) findViewById(R.id.clarity);
        mRetryBtn = (TextView) findViewById(R.id.retry_btn);
        mRetryLayout = (LinearLayout) findViewById(R.id.retry_layout);

        thumbImageView.setOnClickListener(this);
        backButton.setOnClickListener(this);
        tinyBackImageView.setOnClickListener(this);
        clarity.setOnClickListener(this);
        mRetryBtn.setOnClickListener(this);
    }

    public void setUp(Object[] dataSourceObjects, int defaultUrlMapIndex, int screen, Object... objects) {
        super.setUp(dataSourceObjects, defaultUrlMapIndex, screen, objects);
        if (objects.length != 0) titleTextView.setText(objects[0].toString());
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            fullscreenButton.setImageResource(R.drawable.zq_shrink);
            backButton.setVisibility(View.VISIBLE);
            tinyBackImageView.setVisibility(View.INVISIBLE);
            batteryTimeLayout.setVisibility(View.VISIBLE);
            if (((LinkedHashMap) dataSourceObjects[0]).size() == 1) {
                clarity.setVisibility(GONE);
            } else {
                clarity.setText(ZQUtils.getKeyFromDataSource(dataSourceObjects, currentUrlMapIndex));
                clarity.setVisibility(View.VISIBLE);
            }
            changeStartButtonSize((int) getResources().getDimension(R.dimen.zq_start_button_w_h_fullscreen));
        } else if (currentScreen == SCREEN_WINDOW_NORMAL
                || currentScreen == SCREEN_WINDOW_LIST) {
            fullscreenButton.setImageResource(R.drawable.zq_enlarge);
            backButton.setVisibility(View.GONE);
            tinyBackImageView.setVisibility(View.INVISIBLE);
            changeStartButtonSize((int) getResources().getDimension(R.dimen.zq_start_button_w_h_normal));
            batteryTimeLayout.setVisibility(View.GONE);
            clarity.setVisibility(View.GONE);
        } else if (currentScreen == SCREEN_WINDOW_TINY) {
            tinyBackImageView.setVisibility(View.VISIBLE);
            setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                    View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
            batteryTimeLayout.setVisibility(View.GONE);
            clarity.setVisibility(View.GONE);
        }
        setSystemTimeAndBattery();


        if (tmp_test_back) {
            tmp_test_back = false;
            ZQVideoPlayerManager.setFirstFloor(this);
            backPress();
        }
    }

    public void changeStartButtonSize(int size) {
        ViewGroup.LayoutParams lp = startButton.getLayoutParams();
        lp.height = size;
        lp.width = size;
        lp = loadingProgressBar.getLayoutParams();
        lp.height = size;
        lp.width = size;
    }

    @Override
    public int getLayoutId() {
        return R.layout.zq_layout_standard;
    }

    @Override
    public void onStateNormal() {
        super.onStateNormal();
        changeUiToNormal();
    }

    @Override
    public void onStatePreparing() {
        super.onStatePreparing();
        changeUiToPreparing();
    }

    @Override
    public void onStatePreparingChangingUrl(int urlMapIndex, int seekToInAdvance) {
        super.onStatePreparingChangingUrl(urlMapIndex, seekToInAdvance);
        loadingProgressBar.setVisibility(VISIBLE);
        startButton.setVisibility(INVISIBLE);
    }

    @Override
    public void onStatePlaying() {
        super.onStatePlaying();
        changeUiToPlayingClear();
    }

    @Override
    public void onStatePause() {
        super.onStatePause();
        changeUiToPauseShow();
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStateError() {
        super.onStateError();
        changeUiToError();
    }

    @Override
    public void onStateAutoComplete() {
        super.onStateAutoComplete();
        changeUiToComplete();
        cancelDismissControlViewTimer();
        bottomProgressBar.setProgress(100);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    if (mChangePosition) {
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        bottomProgressBar.setProgress(progress);
                    }
                    if (!mChangePosition && !mChangeVolume) {
                        onEvent(ZQUserActionStandard.ON_CLICK_BLANK);
                        onClickUiToggle();
                    }
                    break;
            }
        } else if (id == R.id.bottom_seek_progress) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    break;
            }
        }
        return super.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int i = v.getId();
        if (i == R.id.thumb) {
            if (dataSourceObjects == null || ZQUtils.getCurrentFromDataSource(dataSourceObjects, currentUrlMapIndex) == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!ZQUtils.getCurrentFromDataSource(dataSourceObjects, currentUrlMapIndex).toString().startsWith("file") &&
                        !ZQUtils.getCurrentFromDataSource(dataSourceObjects, currentUrlMapIndex).toString().startsWith("/") &&
                        !ZQUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog(ZQUserActionStandard.ON_CLICK_START_THUMB);
                    return;
                }
                onEvent(ZQUserActionStandard.ON_CLICK_START_THUMB);
                startVideo();
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onClickUiToggle();
            }
        } else if (i == R.id.surface_container) {
            startDismissControlViewTimer();
        } else if (i == R.id.back) {
            backPress();
        } else if (i == R.id.back_tiny) {
            if (ZQVideoPlayerManager.getFirstFloor().currentScreen == ZQVideoPlayer.SCREEN_WINDOW_LIST) {
                quitFullscreenOrTinyWindow();
            } else {
                backPress();
            }
        } else if (i == R.id.clarity) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.zq_layout_clarity, null);

            View.OnClickListener mQualityListener = new View.OnClickListener() {
                public void onClick(View v) {
                    int index = (int) v.getTag();
                    onStatePreparingChangingUrl(index, getCurrentPositionWhenPlaying());
                    clarity.setText(ZQUtils.getKeyFromDataSource(dataSourceObjects, currentUrlMapIndex));
                    for (int j = 0; j < layout.getChildCount(); j++) {//设置点击之后的颜色
                        if (j == currentUrlMapIndex) {
                            ((TextView) layout.getChildAt(j)).setTextColor(Color.parseColor("#fff85959"));
                        } else {
                            ((TextView) layout.getChildAt(j)).setTextColor(Color.parseColor("#ffffff"));
                        }
                    }
                    if (clarityPopWindow != null) {
                        clarityPopWindow.dismiss();
                    }
                }
            };

            for (int j = 0; j < ((LinkedHashMap) dataSourceObjects[0]).size(); j++) {
                String key = ZQUtils.getKeyFromDataSource(dataSourceObjects, j);
                TextView clarityItem = (TextView) View.inflate(getContext(), R.layout.zq_layout_clarity_item, null);
                clarityItem.setText(key);
                clarityItem.setTag(j);
                layout.addView(clarityItem, j);
                clarityItem.setOnClickListener(mQualityListener);
                if (j == currentUrlMapIndex) {
                    clarityItem.setTextColor(Color.parseColor("#fff85959"));
                }
            }

            clarityPopWindow = new PopupWindow(layout, FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, true);
            clarityPopWindow.setContentView(layout);
            clarityPopWindow.showAsDropDown(clarity);
            layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            clarityPopWindow.update(clarity, -40, 46, Math.round(layout.getMeasuredWidth() * 2), layout.getMeasuredHeight());
        } else if (i == R.id.retry_btn) {
            if (!ZQUtils.getCurrentFromDataSource(dataSourceObjects, currentUrlMapIndex).toString().startsWith("file") && !
                    ZQUtils.getCurrentFromDataSource(dataSourceObjects, currentUrlMapIndex).toString().startsWith("/") &&
                    !ZQUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                showWifiDialog(ZQUserAction.ON_CLICK_START_ICON);
                return;
            }
            initTextureView();//和开始播放的代码重复
            addTextureView();
            ZQMediaManager.setDataSource(dataSourceObjects);
            ZQMediaManager.setCurrentDataSource(ZQUtils.getCurrentFromDataSource(dataSourceObjects, currentUrlMapIndex));
            onStatePreparing();
            onEvent(ZQUserAction.ON_CLICK_START_ERROR);
        }
    }

    @Override
    public void showWifiDialog(int action) {
        super.showWifiDialog(action);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getResources().getString(R.string.tips_not_wifi));
        builder.setPositiveButton(getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onEvent(ZQUserActionStandard.ON_CLICK_START_THUMB);
                startVideo();
                WIFI_TIP_DIALOG_SHOWED = true;
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        if (currentState == CURRENT_STATE_PLAYING) {
            dissmissControlView();
        } else {
            startDismissControlViewTimer();
        }
    }

    public void onClickUiToggle() {
        if (bottomContainer.getVisibility() != View.VISIBLE) {
            setSystemTimeAndBattery();
            clarity.setText(ZQUtils.getKeyFromDataSource(dataSourceObjects, currentUrlMapIndex));
        }
        if (currentState == CURRENT_STATE_PREPARING) {
            changeUiToPreparing();
            if (bottomContainer.getVisibility() == View.VISIBLE) {
            } else {
                setSystemTimeAndBattery();
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            } else {
                changeUiToPlayingShow();
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            } else {
                changeUiToPauseShow();
            }
        }
    }

    public void setSystemTimeAndBattery() {
        SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        videoCurrentTime.setText(dateFormater.format(date));
        if (!brocasting) {
            getContext().registerReceiver(
                    battertReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );
        }
    }

    public void onCLickUiToggleToClear() {
        if (currentState == CURRENT_STATE_PREPARING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPreparing();
            } else {
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            } else {
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            } else {
            }
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToComplete();
            } else {
            }
        }
    }

    @Override
    public void setProgressAndText(int progress, int position, int duration) {
        super.setProgressAndText(progress, position, duration);
        if (progress != 0) bottomProgressBar.setProgress(progress);
    }

    @Override
    public void setBufferProgress(int bufferProgress) {
        super.setBufferProgress(bufferProgress);
        if (bufferProgress != 0) bottomProgressBar.setSecondaryProgress(bufferProgress);
    }

    @Override
    public void resetProgressAndTime() {
        super.resetProgressAndTime();
        bottomProgressBar.setProgress(0);
        bottomProgressBar.setSecondaryProgress(0);
    }

    public void changeUiToNormal() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    public void changeUiToPreparing() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingShow() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingClear() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPauseShow() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    public void changeUiToPauseClear() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToComplete() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToError() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisiblity(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void setAllControlsVisiblity(int topCon, int bottomCon, int startBtn, int loadingPro,
                                        int thumbImg, int bottomPro, int retryLayout) {
        topContainer.setVisibility(topCon);
        bottomContainer.setVisibility(bottomCon);
        startButton.setVisibility(startBtn);
        loadingProgressBar.setVisibility(loadingPro);
        thumbImageView.setVisibility(thumbImg);
        bottomProgressBar.setVisibility(bottomPro);
        mRetryLayout.setVisibility(retryLayout);
    }

    public void updateStartImage() {
        if (currentState == CURRENT_STATE_PLAYING) {
            startButton.setImageResource(R.drawable.zq_click_pause_selector);
            replayTextView.setVisibility(INVISIBLE);
        } else if (currentState == CURRENT_STATE_ERROR) {
            startButton.setVisibility(View.INVISIBLE);
            replayTextView.setVisibility(INVISIBLE);
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            startButton.setImageResource(R.drawable.zq_click_replay_selector);
            replayTextView.setVisibility(VISIBLE);
        } else {
            startButton.setImageResource(R.drawable.zq_click_play_selector);
            replayTextView.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.zq_dialog_progress, null);
            mDialogProgressBar = (ProgressBar) localView.findViewById(R.id.duration_progressbar);
            mDialogSeekTime = (TextView) localView.findViewById(R.id.tv_current);
            mDialogTotalTime = (TextView) localView.findViewById(R.id.tv_duration);
            mDialogIcon = (ImageView) localView.findViewById(R.id.duration_image_tip);
            mProgressDialog = createDialogWithView(localView);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        mDialogProgressBar.setProgress(totalTimeDuration <= 0 ? 0 : (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.zq_forward_icon);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.zq_backward_icon);
        }
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void showVolumeDialog(float deltaY, int volumePercent) {
        super.showVolumeDialog(deltaY, volumePercent);
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.zq_dialog_volume, null);
            mDialogVolumeImageView = (ImageView) localView.findViewById(R.id.volume_image_tip);
            mDialogVolumeTextView = (TextView) localView.findViewById(R.id.tv_volume);
            mDialogVolumeProgressBar = (ProgressBar) localView.findViewById(R.id.volume_progressbar);
            mVolumeDialog = createDialogWithView(localView);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (volumePercent <= 0) {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.zq_close_volume);
        } else {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.zq_add_volume);
        }
        if (volumePercent > 100) {
            volumePercent = 100;
        } else if (volumePercent < 0) {
            volumePercent = 0;
        }
        mDialogVolumeTextView.setText(volumePercent + "%");
        mDialogVolumeProgressBar.setProgress(volumePercent);
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissVolumeDialog() {
        super.dismissVolumeDialog();
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    @Override
    public void showBrightnessDialog(int brightnessPercent) {
        super.showBrightnessDialog(brightnessPercent);
        if (mBrightnessDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.zq_dialog_brightness, null);
            mDialogBrightnessTextView = (TextView) localView.findViewById(R.id.tv_brightness);
            mDialogBrightnessProgressBar = (ProgressBar) localView.findViewById(R.id.brightness_progressbar);
            mBrightnessDialog = createDialogWithView(localView);
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        if (brightnessPercent > 100) {
            brightnessPercent = 100;
        } else if (brightnessPercent < 0) {
            brightnessPercent = 0;
        }
        mDialogBrightnessTextView.setText(brightnessPercent + "%");
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissBrightnessDialog() {
        super.dismissBrightnessDialog();
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
        }
    }

    public Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(getContext(), R.style.jc_style_dialog_progress);
        dialog.setContentView(localView);
        Window window = dialog.getWindow();
        window.addFlags(Window.FEATURE_ACTION_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        window.setLayout(-2, -2);
        WindowManager.LayoutParams localLayoutParams = window.getAttributes();
        localLayoutParams.gravity = Gravity.CENTER;
        window.setAttributes(localLayoutParams);
        return dialog;
    }

    public void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        DISMISS_CONTROL_VIEW_TIMER = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISMISS_CONTROL_VIEW_TIMER.schedule(mDismissControlViewTimerTask, 2500);
    }

    public void cancelDismissControlViewTimer() {
        if (DISMISS_CONTROL_VIEW_TIMER != null) {
            DISMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
        }

    }

    @Override
    public void onAutoCompletion() {
        super.onAutoCompletion();
        cancelDismissControlViewTimer();
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        cancelDismissControlViewTimer();
        if (clarityPopWindow != null) {
            clarityPopWindow.dismiss();
        }
    }

    public void dissmissControlView() {
        if (currentState != CURRENT_STATE_NORMAL
                && currentState != CURRENT_STATE_ERROR
                && currentState != CURRENT_STATE_AUTO_COMPLETE) {
            if (getContext() != null && getContext() instanceof Activity) {
                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bottomContainer.setVisibility(View.INVISIBLE);
                        topContainer.setVisibility(View.INVISIBLE);
                        startButton.setVisibility(View.INVISIBLE);
                        if (clarityPopWindow != null) {
                            clarityPopWindow.dismiss();
                        }
                        if (currentScreen != SCREEN_WINDOW_TINY) {
                            bottomProgressBar.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }
    }

    public class DismissControlViewTimerTask extends TimerTask {

        @Override
        public void run() {
            dissmissControlView();
        }
    }
}
