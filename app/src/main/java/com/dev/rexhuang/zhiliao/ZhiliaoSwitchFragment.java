package com.dev.rexhuang.zhiliao;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.dev.rexhuang.zhiliao.detail.DetailFragment;
import com.dev.rexhuang.zhiliao.find.queue.QueueAdapter;
import com.dev.rexhuang.zhiliao.search.SearchFragment;
import com.dev.rexhuang.zhiliao_core.base.BaseActivity;
import com.dev.rexhuang.zhiliao_core.base.BaseFragment;
import com.dev.rexhuang.zhiliao_core.base.FragmentKeys;
import com.dev.rexhuang.zhiliao_core.base.ZhiliaoFragment;
import com.dev.rexhuang.zhiliao_core.callback.SwitchFragmentListener;
import com.dev.rexhuang.zhiliao_core.config.ConfigKeys;
import com.dev.rexhuang.zhiliao_core.config.Zhiliao;
import com.dev.rexhuang.zhiliao_core.entity.MusicEntity;
import com.dev.rexhuang.zhiliao_core.player2.manager.MusicManager;
import com.dev.rexhuang.zhiliao_core.player2.manager.OnPlayerEventListener;
import com.dev.rexhuang.zhiliao_core.player2.playback.QueueManager;
import com.dev.rexhuang.zhiliao_core.utils.AnimHelper;
import com.mikepenz.iconics.view.IconicsTextView;
import com.orhanobut.logger.Logger;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;
import me.yokeyword.fragmentation.ISupportActivity;
import me.yokeyword.fragmentation.ISupportFragment;
import me.yokeyword.fragmentation.SupportHelper;

/**
 * *  created by RexHuang
 * *  on 2019/7/26
 */
public abstract class ZhiliaoSwitchFragment extends ZhiliaoFragment implements SwitchFragmentListener {

    private static final String TAG = ZhiliaoSwitchFragment.class.getSimpleName();
    protected static final LinkedHashMap<String, BaseFragment> FRAGMENTS = new LinkedHashMap<>();
    protected static final LinkedList<String> NAMES = new LinkedList<>();
    protected static final BaseFragment[] mFragments = new BaseFragment[4];
    private static final int DEFAULT_FRAGMENT_INDEX = 0;
    private int mCurrentFragment = DEFAULT_FRAGMENT_INDEX;
    private int mPreviousFragment = DEFAULT_FRAGMENT_INDEX;
    private SwitchFragmentListener mSwitchFragmentListener;
    private String play = "{faw-play}";
    private String pause = "{faw-pause}";

    //Animator
    private ObjectAnimator cover_play;
    private float startRotate = 0f;
    private float endRotate = 360f;
    private long duration = 10000;

    private QueueAdapter queueAdapter;
    private RecyclerView recyclerView;
    private QueueManager queueManager = QueueManager.getInstance();
    private Dialog dialog;
    private OnPlayerEventListener onPlayerEventListener;
    private SwitchHandler handler = new SwitchHandler(this);
    private static final int UPDATE_QUEUE = 102000000;

    @BindView(R.id.search_bar)
    LinearLayout search_bar;

    @BindView(R.id.song_cover)
    AppCompatImageView song_cover;

    @BindView(R.id.song_description)
    AppCompatTextView song_description;

    @BindView(R.id.song_play_button)
    IconicsTextView song_play_button;

    @BindView(R.id.song_list_button)
    IconicsTextView song_list_button;

    @OnClick(R.id.song_list_button)
    void onClickList() {
        //1、使用Dialog、设置style
        dialog = new Dialog(get_mActivity(), R.style.DialogTheme);
        //2、设置布局
        View view = View.inflate(get_mActivity(), R.layout.dialog_playqueue, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        //设置弹出位置
        window.setGravity(Gravity.BOTTOM);
        //设置弹出动画
        window.setWindowAnimations(R.style.main_menu_animStyle);
        //设置对话框大小
        final float scale = ((Context) Zhiliao.getConfig(ConfigKeys.APPLICATION_CONTEXT.name())).getResources().getDisplayMetrics().density;
        int height = (int) (400 * scale + 0.5f);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
        dialog.show();

        recyclerView = dialog.findViewById(R.id.rcv_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(get_mActivity()));
        queueAdapter = new QueueAdapter(R.layout.item_queue, MusicManager.getInstance().getPlayList());
        recyclerView.setAdapter(queueAdapter);
        queueAdapter.bindToRecyclerView(recyclerView);
        recyclerView.scrollToPosition(MusicManager.getInstance().getNowPlayingIndex());
//        queueAdapter = new QueueAdapter(R.layout.item_queue, queueManager.getPlayingQueue());
        queueAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                MusicManager.getInstance().playMusicByIndex(position);
            }
        });
        queueAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (view.getId() == R.id.iv_more) {
//                    MusicEntity item = MusicManager.getInstance().getPlayList().get(position);
//                    MusicProvider.getInstance().
//                    queueAdapter.notifyItemRemoved(position);
                }
            }
        });

        dialog.show();
    }

    @OnClick(R.id.song_play_button)
    void onClickPlay() {
        Logger.d(MusicManager.getInstance().isPlaying());
        if (MusicManager.getInstance().isPlaying()) {
            pauseMusic();
        } else {
            playMusic();
        }
    }

    @OnClick(R.id.search_bar)
    void onClickSearch() {
        MusicEntity musicEntity = MusicManager.getInstance().getNowPlayingSongInfo();
        SearchFragment searchFragment;
        if ((searchFragment = SupportHelper.findFragment(getFragmentManager(), SearchFragment.class)) == null) {
            searchFragment = SearchFragment.newInstance(
                    musicEntity != null ? musicEntity.getId() : null,
                    musicEntity != null ? song_cover.getRotation() : 0f, MainSwitchFragment.class.getSimpleName());
            getSupportDelegate().start(searchFragment, ISupportFragment.SINGLETASK);
        } else {
            Bundle args = searchFragment.getArguments() == null ? new Bundle() : searchFragment.getArguments();
            if (musicEntity != null) {
                args.putString(BaseActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, musicEntity.getId());
                args.putFloat(BaseActivity.EXTRA_CURRENT_MEDIA_ROTATION, song_cover.getRotation());
                args.putString(BaseActivity.FRGMENT_FROM, MainSwitchFragment.class.getSimpleName());
                searchFragment.setArguments(args);
            }
            ((ISupportActivity) get_mActivity()).getSupportDelegate().showHideFragment(searchFragment, this);
        }

    }

    @OnClick(R.id.controlbar)
    void onClickControl() {
        MusicEntity musicEntity = MusicManager.getInstance().getNowPlayingSongInfo();
        DetailFragment detailFragment;
        if ((detailFragment = SupportHelper.findFragment(getFragmentManager(), DetailFragment.class)) == null) {
            detailFragment = DetailFragment.newInstance(
                    musicEntity != null ? musicEntity.getId() : null,
                    musicEntity != null ? song_cover.getRotation() : 0f, MainSwitchFragment.class.getSimpleName());
            getSupportDelegate().start(detailFragment, ISupportFragment.SINGLETASK);
        } else {
            Bundle args = detailFragment.getArguments() == null ? new Bundle() : detailFragment.getArguments();
            if (musicEntity != null) {
                args.putString(BaseActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, musicEntity.getId());
                args.putFloat(BaseActivity.EXTRA_CURRENT_MEDIA_ROTATION, song_cover.getRotation());
                args.putString(BaseActivity.FRGMENT_FROM, MainSwitchFragment.class.getSimpleName());
                detailFragment.setArguments(args);
            }
            ((ISupportActivity) get_mActivity()).getSupportDelegate().showHideFragment(detailFragment, this);
        }
    }

    @BindView(R.id.title)
    protected AppCompatTextView mTitleTv;

    @BindViews({R.id.iv_music_hall, R.id.iv_recommend, R.id.iv_find, R.id.iv_profile})
    protected IconicsTextView[] sw_ivs;

    @BindViews({R.id.tv_music_hall, R.id.tv_recommend, R.id.tv_find, R.id.tv_profile})
    protected TextView[] sw_tvs;

    @OnClick(R.id.layout_bottom_music_hall)
    void onClickMusicHall() {
        Toast.makeText(_mActivity, "onClickMusicHall", Toast.LENGTH_SHORT).show();
        getSupportDelegate().showHideFragment(FRAGMENTS.get(FragmentKeys.MUSIC_HALL.name())
                , mFragments[getCurrentFragment()]);
        setCurrentFragment(FragmentKeys.MUSIC_HALL.ordinal());
        if (mSwitchFragmentListener != null) {
            mSwitchFragmentListener.onSwitchEnd();
        }
    }

    @OnClick(R.id.layout_bottom_recommend)
    void onClickRecommend() {
        Toast.makeText(_mActivity, "onClickRecommend", Toast.LENGTH_SHORT).show();
        getSupportDelegate().showHideFragment(FRAGMENTS.get(FragmentKeys.RECOMMEND.name())
                , mFragments[getCurrentFragment()]);
        setCurrentFragment(FragmentKeys.RECOMMEND.ordinal());
        if (mSwitchFragmentListener != null) {
            mSwitchFragmentListener.onSwitchEnd();
        }
    }

    @OnClick(R.id.layout_bottom_find)
    void onClickFind() {
        Toast.makeText(_mActivity, "onClickFind", Toast.LENGTH_SHORT).show();
        getSupportDelegate().showHideFragment(FRAGMENTS.get(FragmentKeys.FIND.name())
                , mFragments[getCurrentFragment()]);
        setCurrentFragment(FragmentKeys.FIND.ordinal());
        if (mSwitchFragmentListener != null) {
            mSwitchFragmentListener.onSwitchEnd();
        }
    }

    @OnClick(R.id.layout_bottom_profile)
    void onClickProfile() {
        Toast.makeText(_mActivity, "onClickProfile", Toast.LENGTH_SHORT).show();
        getSupportDelegate().showHideFragment(FRAGMENTS.get(FragmentKeys.PROFILE.name())
                , mFragments[getCurrentFragment()]);
        setCurrentFragment(FragmentKeys.PROFILE.ordinal());
        if (mSwitchFragmentListener != null) {
            mSwitchFragmentListener.onSwitchEnd();
        }
    }

    /**
     * data：setFragments
     */
    public abstract void setFragments();

    public abstract void loadFragments(int containerId);

    private void setCurrentFragment(int index) {
        setPreviousFragment(this.mCurrentFragment);
        this.mCurrentFragment = index;
    }

    protected int getCurrentFragment() {
        return this.mCurrentFragment;
    }

    @Override
    public Object setLayout() {
        return R.layout.fragment_zhiliao;
    }

    @Override
    public void onBindView(Bundle savedInstanceState, View view) {
        setFragments();
        loadFragments(R.id.layout_container);
        cover_play = AnimHelper.rotate(song_cover, "rotation", AnimHelper.DEFAULT_START_ROTATE,
                AnimHelper.DEFAULT_END_ROTATE, AnimHelper.DEFAULT_DURATION,
                ValueAnimator.INFINITE, ValueAnimator.RESTART);
        onPlayerEventListener = new OnPlayerEventListener() {
            @Override
            public void onMusicSwitch(MusicEntity musicEntity) {
                showPlaying(musicEntity, false);
                if (queueAdapter != null) {
                    queueAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onPlayerStart() {
                song_play_button.setText(pause);
                playAnimation();
            }

            @Override
            public void onPlayerPause() {
                showStopped();
            }

            @Override
            public void onPlayerStop() {
                showStopped();
            }

            @Override
            public void onPlayCompletion(MusicEntity musicEntity) {
                showStopped();
            }

            @Override
            public void onBuffering() {
                Toast.makeText(get_mActivity(), "正在缓冲,请稍等!!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Toast.makeText(get_mActivity(), "播放出错!!", Toast.LENGTH_SHORT).show();
            }
        };
        MusicManager.getInstance().addPlayerEventListener(onPlayerEventListener);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        MusicManager.getInstance().removePlayerEventListener(onPlayerEventListener);
        stopAnimation();
        super.onDestroyView();
    }


    public void setISwitchFragmentListener(SwitchFragmentListener mSwitchFragmentListener) {
        this.mSwitchFragmentListener = mSwitchFragmentListener;
    }

    public int getPreviousFragment() {
        return mPreviousFragment;
    }

    public void setPreviousFragment(int mPreviousFragment) {
        this.mPreviousFragment = mPreviousFragment;
    }

    @Override
    public void onSwitchEnd() {
        mTitleTv.setText((String) Objects.requireNonNull(NAMES.toArray())[getCurrentFragment()]);
        final int mPreviousIndex = getPreviousFragment();
        final int mCurrentIndex = getCurrentFragment();
        sw_ivs[mPreviousIndex].setTextColor(_mActivity.getResources().getColor(R.color.switch_unselected));
        sw_tvs[mPreviousIndex].setTextColor(_mActivity.getResources().getColor(R.color.switch_unselected));
        sw_ivs[mCurrentIndex].setTextColor(_mActivity.getResources().getColor(R.color.switch_selected));
        sw_tvs[mCurrentIndex].setTextColor(_mActivity.getResources().getColor(R.color.switch_selected));
    }

    private void showStopped() {
        song_play_button.setText(play);
        pauseAnimation();
    }

    protected void showPlaying(MusicEntity musicEntity, boolean isPlayStart) {
        if (musicEntity != null) {
            song_description.setText(String.format("%s - %s", musicEntity.getName(), musicEntity.getSingers().get(0).getName()));
            Glide.with(get_mActivity())
                    .load(musicEntity.getCover())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .into(song_cover);
            if (isPlayStart) {
                song_play_button.setText(pause);
                playAnimation();
            }
        }
    }

    private void playAnimation() {
        if (cover_play != null) {
            if (!cover_play.isStarted()) {
                cover_play.start();
            } else if (cover_play.isPaused()) {
                cover_play.resume();
            }
        }
    }

    private void pauseAnimation() {
        if (cover_play != null) {
            if (cover_play.isRunning()) {
                cover_play.pause();
            }
        }
    }

    private void stopAnimation() {
        if (cover_play != null) {
            cover_play.cancel();
        }
    }

    private void playMusic() {
        MusicManager.getInstance().playMusic();
    }

    private void pauseMusic() {
        MusicManager.getInstance().pauseMusic();
    }

    private static class SwitchHandler extends Handler {
        private WeakReference<ZhiliaoSwitchFragment> zhiliaoSwitchFragmentWeakReference;

        public SwitchHandler(ZhiliaoSwitchFragment zhiliaoSwitchFragment) {
            zhiliaoSwitchFragmentWeakReference = new WeakReference<>(zhiliaoSwitchFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ZhiliaoSwitchFragment zhiliaoSwitchFragment = zhiliaoSwitchFragmentWeakReference.get();
            switch (msg.what) {
                case UPDATE_QUEUE:
                    zhiliaoSwitchFragment.queueAdapter.setNewData(MusicManager.getInstance().getPlayList());
                    zhiliaoSwitchFragment.queueAdapter.notifyDataSetChanged();
            }
        }
    }
}