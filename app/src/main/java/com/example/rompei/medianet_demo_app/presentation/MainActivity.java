package com.example.rompei.medianet_demo_app.presentation;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.rompei.medianet_demo_app.R;
import com.example.rompei.medianet_demo_app.bgm.api.ThreadApi;
import com.example.rompei.medianet_demo_app.bgm.models.ThreadEntity;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.converter.GsonConverter;
import rx.Observer;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.recycler)RecyclerView mRecycler;
    @Bind(R.id.progress)
    ProgressBar mProgress;
    @Bind(R.id.root)
    CoordinatorLayout mRootView;
    @Bind(R.id.swipeLayout)
    SwipeRefreshLayout mSwipeLayout;

    private RestAdapter mAdapter;
    private MyAdapter mRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRecycler.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mProgress.setVisibility(View.VISIBLE);
                getThread();
            }
        });


        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();

        mAdapter = new RestAdapter.Builder()
                .setEndpoint("http://medianet.inf.uec.ac.jp/~t1310077")
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setLog(new AndroidLog("=NETWORK="))
                .build();

        getThread();
    }

    private void getThread(){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(View.VISIBLE);
            }
        });
        mAdapter.create(ThreadApi.class).get()
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<ThreadEntity>() {


                    @Override
                    public void onCompleted() {
                        Log.d("MainActivity", "onCompleted()");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onFinishRequest();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("MainActivity", "Error : " + e.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onFinishRequest();
                            }
                        });
                    }

                    @Override
                    public void onNext(final ThreadEntity threadEntity) {
                        Log.d("MainActivity", "onNext()");
                        if (threadEntity != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(mRecycler.getAdapter()==null) {
                                        mRecyclerAdapter = new MyAdapter(MainActivity.this, threadEntity.reply);
                                        mRecycler.setAdapter(mRecyclerAdapter);
                                    } else{
                                        mRecyclerAdapter.clearAll();
                                        mRecyclerAdapter.addAll(threadEntity.reply);
                                    }

                                }
                            });

                        }
                    }
                });
    }

    private void postReply(ThreadEntity.Reply reply){
        mAdapter.create(ThreadApi.class).post(reply)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<ThreadEntity.Reply>() {
                    @Override
                    public void onCompleted() {
                        Log.d("MainActivity", "onCompleted()");
                        getThread();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(mRootView, getString(R.string.post_message), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("MainActivity", "Error : " + e.toString());
                    }

                    @Override
                    public void onNext(ThreadEntity.Reply thread) {
                        Log.d("MainActivity", "onNext()");
                    }
                });
    }

    private void onFinishRequest(){
        mProgress.setVisibility(View.GONE);
        mSwipeLayout.setRefreshing(false);
    }

    @OnClick(R.id.fab)
    public void onFabClicked(){
        new MaterialDialog.Builder(this)
                .title(R.string.send_reply_title)
                .customView(R.layout.send_dialog_layout, true)
                .positiveText(R.string.send)
                .positiveColorRes(R.color.primary_color)
                .negativeText(R.string.cancel)
                .negativeColorRes(R.color.primary_dark)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        View view = dialog.getCustomView();
                        if (view != null) {
                            EditText name = ButterKnife.findById(view, R.id.reply_name);
                            EditText text = ButterKnife.findById(view, R.id.reply_text);
                            if (name.getText() != null && text.getText() != null && !text.getText().toString().equals("")) {
                                Log.d("Dialog", "name : " + name.getText() + " text : " + text.getText());
                                if (name.getText().toString().equals(""))
                                    name.setText(R.string.name_placeholder);
                                ThreadEntity.Reply reply = new ThreadEntity.Reply(name.getText().toString(), text.getText().toString());
                                postReply(reply);
                            } else {
                                Snackbar.make(mRootView, getString(R.string.send_error), Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                    }
                })
                .show();
    }


    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{
        private LayoutInflater mInflater;
        private List<ThreadEntity.Reply> mItems;

        public MyAdapter(Context context, List<ThreadEntity.Reply> items) {
            if(items != null)
                this.mItems = items;
            else
                this.mItems = new ArrayList<>();
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.card_view_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ThreadEntity.Reply thread= mItems.get(position);
            holder.name.setText(thread.name);
            holder.text.setText(thread.text);
        }

        @Override
        public int getItemCount() {
            return mItems != null && !mItems.isEmpty() ? mItems.size() : 0;
        }

        public void clearAll(){
            mItems.clear();
            notifyDataSetChanged();
        }

        public void addAll(List<ThreadEntity.Reply> threads){
            mItems.addAll(threads);
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView text;

            public ViewHolder(View itemView) {
                super(itemView);
                name = ButterKnife.findById(itemView, R.id.name);
                text = ButterKnife.findById(itemView, R.id.text);
            }
        }

    }
}
