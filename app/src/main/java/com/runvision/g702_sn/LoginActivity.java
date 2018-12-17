package com.runvision.g702_sn;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.github.ybq.android.spinkit.style.Wave;
import com.google.gson.Gson;
import com.runvision.bean.Login;
import com.runvision.bean.LoginResponse;
import com.runvision.core.Const;
import com.runvision.db.User;
import com.runvision.utils.CameraHelp;
import com.runvision.utils.DateTimeUtils;
import com.runvision.utils.FileUtils;
import com.runvision.utils.IDUtils;
import com.runvision.utils.RSAUtils;
import com.runvision.utils.SPUtil;
import com.runvision.utils.SharedPreferencesHelper;
import com.runvision.utils.TimeUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import customview.ConfirmDialog;
import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.MediaType;
import util.UpdateAppReceiver;
import util.UpdateAppUtils;

/**
 * 登录
 * Created by ChaoLi on 2018/10/13 0013 - 12:48
 * Email: lichao3140@gmail.com
 * Version: v1.0
 */
public class LoginActivity extends FragmentActivity {

    @BindView(R.id.logo)
    ImageView logo;
    @BindView(R.id.et_user)
    EditText etUser;
    @BindView(R.id.iv_clean_user)
    ImageView ivCleanUser;
    @BindView(R.id.et_password)
    EditText etPassword;
    @BindView(R.id.clean_password)
    ImageView cleanPassword;
    @BindView(R.id.iv_show_pwd)
    ImageView ivShowPwd;
    @BindView(R.id.btn_login)
    Button btnLogin;
    @BindView(R.id.regist)
    TextView regist;
    @BindView(R.id.forget_password)
    TextView forgetPassword;
    @BindView(R.id.content)
    LinearLayout content;
    @BindView(R.id.scrollView)
    ScrollView scrollView;
    @BindView(R.id.service)
    LinearLayout service;
    @BindView(R.id.root)
    RelativeLayout root;

    private int screenHeight = 0;//屏幕高度
    private int keyHeight = 0; //软件盘弹起后所占高度
    private float scale = 0.6f; //logo缩放比例

    private ProgressBar progressBar;
    private SharedPreferencesHelper faceSP;
    private Context mContext;

    Gson gson = new Gson();

    private BroadcastReceiver receiver = new UpdateAppReceiver();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // 全屏代码
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ButterKnife.bind(this);
        screenHeight = this.getResources().getDisplayMetrics().heightPixels; //获取屏幕高度
        keyHeight = screenHeight / 3;//弹起高度为屏幕高度的1/3
        mContext = this;
        faceSP = new SharedPreferencesHelper(mContext, "faceInfo");
        initListener();
        initData();

        // 动态注册receiver
        IntentFilter intentFilter = new IntentFilter("teprinciple.update");
        registerReceiver(receiver,intentFilter);
        checkAndUpdate();
    }

    private void initData() {
        etUser.setText("lichao");
        etPassword.setText("123");
        progressBar = findViewById(R.id.spin_kit);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
        etUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!TextUtils.isEmpty(editable) && ivCleanUser.getVisibility() == View.GONE) {
                    ivCleanUser.setVisibility(View.VISIBLE);
                } else if (TextUtils.isEmpty(editable)) {
                    ivCleanUser.setVisibility(View.GONE);
                }
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!TextUtils.isEmpty(editable) && cleanPassword.getVisibility() == View.GONE) {
                    cleanPassword.setVisibility(View.VISIBLE);
                } else if (TextUtils.isEmpty(editable)) {
                    cleanPassword.setVisibility(View.GONE);
                }
            }
        });

        /**
         * 禁止键盘弹起的时候可以滚动
         */
        scrollView.setOnTouchListener((view, motionEvent) -> true);

        scrollView.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (oldBottom != 0 && bottom != 0 && (oldBottom - bottom > keyHeight)) {
                int dist = content.getBottom() - bottom;
                if (dist > 0) {
                    ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(content, "translationY", 0.0f, -dist);
                    mAnimatorTranslateY.setDuration(300);
                    mAnimatorTranslateY.setInterpolator(new LinearInterpolator());
                    mAnimatorTranslateY.start();
                    zoomIn(logo, dist);
                }
                service.setVisibility(View.INVISIBLE);

            } else if (oldBottom != 0 && bottom != 0 && (bottom - oldBottom > keyHeight)) {
                if ((content.getBottom() - oldBottom) > 0) {
                    ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(content, "translationY", content.getTranslationY(), 0);
                    mAnimatorTranslateY.setDuration(300);
                    mAnimatorTranslateY.setInterpolator(new LinearInterpolator());
                    mAnimatorTranslateY.start();
                    //键盘收回后，logo恢复原来大小，位置同样回到初始位置
                    zoomOut(logo);
                }
                service.setVisibility(View.VISIBLE);
            }
        });
    }

    @OnClick({R.id.regist, R.id.iv_clean_user, R.id.clean_password, R.id.iv_show_pwd, R.id.btn_login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.regist:
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.iv_clean_user:
                etUser.setText("");
                break;
            case R.id.clean_password:
                etPassword.setText("");
                break;
            case R.id.iv_show_pwd:
                if (etPassword.getInputType() != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ivShowPwd.setImageResource(R.mipmap.pass_visuable);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ivShowPwd.setImageResource(R.mipmap.pass_gone);
                }
                String pwd = etPassword.getText().toString();
                if (!TextUtils.isEmpty(pwd))
                    etPassword.setSelection(pwd.length());
                break;
            case R.id.btn_login:
                if (!SPUtil.getString(Const.DEV_NUM,"").equals("")) {
                    progressBar.setVisibility(View.VISIBLE);
                    Wave doubleBounce = new Wave();
                    progressBar.setIndeterminateDrawable(doubleBounce);
                    login();
                } else {
                    Toasty.error(mContext, "请先注册考勤终端", Toast.LENGTH_LONG, true).show();
                }
                break;
        }
    }

    /**
     * 用户登录
     */
    private void login() {
        try {
            String privateKey = SPUtil.getString(Const.PRIVATE_KEY,"");
            String devnum = SPUtil.getString(Const.DEV_NUM,"");
            String username = etUser.getText().toString().trim();
            String passwd = etPassword.getText().toString().trim();
            String ts = TimeUtils.getTime13();
            String sign = devnum + username + passwd + ts;
            byte[] ss = sign.getBytes();
            String sign_str = RSAUtils.sign(ss, privateKey);

            OkHttpUtils.postString()
                    .url(Const.LOGIN + "ts=" + TimeUtils.getTime13() + "&sign=" + sign_str)
                    .content(new Gson().toJson(new Login(sign_str, devnum, username, passwd, ts)))
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build()
                    .execute(new StringCallback() {

                        @Override
                        public void onError(Call call, Exception e, int id) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toasty.error(mContext, getString(R.string.toast_request_error), Toast.LENGTH_LONG, true).show();
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            Log.i("lichao", "success:" + response);
                            if (!response.equals("resource/500")) {
                                LoginResponse gsonLogin = gson.fromJson(response, LoginResponse.class);
                                if (gsonLogin.getErrorcode() == 0) {
                                    if (gsonLogin.getData().getFace() != null) {
                                        addFace(gsonLogin.getData().getFace());
                                        SPUtil.putString("username", username);
                                        SPUtil.putString("passwd", passwd);
                                        faceSP.put("face", gsonLogin.getData().getFace());
                                    } else {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Toasty.error(mContext, getString(R.string.toast_login_error_no_face), Toast.LENGTH_LONG, true).show();
                                    }
                                }  else if (gsonLogin.getErrorcode() == 1) {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    Toasty.error(mContext, getString(R.string.toast_login_error) + gsonLogin.getMessage(), Toast.LENGTH_LONG, true).show();
                                } else {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    Toasty.error(mContext, getString(R.string.toast_login_error_code) + gsonLogin.getErrorcode(), Toast.LENGTH_LONG, true).show();
                                }
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                Toasty.error(mContext, getString(R.string.toast_server_error), Toast.LENGTH_LONG, true).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存人脸模板
     *
     * @param faceInfo
     */
    private void addFace(String faceInfo) {
        if (faceInfo.isEmpty()) {
            Toasty.warning(mContext, "获取模板图片失败", Toast.LENGTH_SHORT, true).show();
            return;
        }
        String userName = etUser.getText().toString().trim();
        String userPhone = "15888888888";
        String userage = "18";
        String useridnum= "888888";
        String imageID= IDUtils.genImageName();

        byte[] decode = Base64.decode(faceInfo, Base64.DEFAULT);
        Bitmap faceBitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);

        if (faceSP.getSharedPreference("face", "").equals(faceInfo)) {
            Intent intent = new Intent(mContext, MainActivity.class);
            startActivity(intent);
            finish();
            Toasty.success(mContext, getString(R.string.toast_login_success), Toast.LENGTH_SHORT, true).show();
        } else {
            if (mSaveTemplate(faceBitmap, imageID)) {
                User user = new User(userName, "白名单", "1", Integer.parseInt(userage), userPhone, useridnum, imageID, DateTimeUtils.getTime());
                MyApplication.faceProvider.addUserOutId(user);
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
                Toasty.success(mContext, getString(R.string.toast_login_success), Toast.LENGTH_SHORT, true).show();
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                Toasty.warning(mContext, "模板图片保存失败", Toast.LENGTH_SHORT, true).show();
            }
        }
    }

    /**
     * 保存模板
     * @param bitmap
     * @param fileName
     * @return
     */
    public boolean mSaveTemplate(Bitmap bitmap, String fileName) {
        boolean generateTemplate = false;
        String path = Environment.getExternalStorageDirectory() + "/FaceAndroid/Template/";
        String ImagePath = Environment.getExternalStorageDirectory() + "/FaceAndroid/Face/";
        int w = bitmap.getWidth() % 2 == 0 ? bitmap.getWidth() : bitmap.getWidth() - 1;
        int h = bitmap.getHeight() % 2 == 0 ? bitmap.getHeight() : bitmap.getHeight() - 1;

        byte[] nv21 = CameraHelp.getNV21(w, h, bitmap);

        List<FaceInfo> result = new ArrayList<FaceInfo>();
        MyApplication.mFaceLibCore.FaceDetection(nv21, w, h, result);
        if ((MyApplication.mFaceLibCore.FaceDetection(nv21, w, h, result) == 0)&&(result.size() > 0)) {
            FaceFeature faceFeature = new FaceFeature();
            int ret = MyApplication.mFaceLibCore.FaceFeatureExtract(nv21, w, h,result.get(0), faceFeature);
            if (ret == 0) {
                CameraHelp.saveFile(path, fileName+".data", faceFeature.getFeatureData());
                CameraHelp.saveImgToDisk(ImagePath, fileName+".jpg", bitmap);
                FileUtils.saveFile(bitmap, fileName, "FaceTemplate");
                MyApplication.mList.put(fileName, faceFeature.getFeatureData());
                generateTemplate=true;
                System.out.println("存入模板库");
            } else {
                generateTemplate = false;
                System.out.println("提取模版失败");
            }
        } else {
            generateTemplate = false;
            System.out.println("无人脸");
        }
        return generateTemplate;
    }

    /**
     * 缩小
     *
     * @param view
     */
    public void zoomIn(final View view, float dist) {
        view.setPivotY(view.getHeight());
        view.setPivotX(view.getWidth() / 2);
        AnimatorSet mAnimatorSet = new AnimatorSet();
        ObjectAnimator mAnimatorScaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, scale);
        ObjectAnimator mAnimatorScaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, scale);
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(view, "translationY", 0.0f, -dist);

        mAnimatorSet.play(mAnimatorTranslateY).with(mAnimatorScaleX);
        mAnimatorSet.play(mAnimatorScaleX).with(mAnimatorScaleY);
        mAnimatorSet.setDuration(300);
        mAnimatorSet.start();
    }

    /**
     * f放大
     *
     * @param view
     */
    public void zoomOut(final View view) {
        view.setPivotY(view.getHeight());
        view.setPivotX(view.getWidth() / 2);
        AnimatorSet mAnimatorSet = new AnimatorSet();

        ObjectAnimator mAnimatorScaleX = ObjectAnimator.ofFloat(view, "scaleX", scale, 1.0f);
        ObjectAnimator mAnimatorScaleY = ObjectAnimator.ofFloat(view, "scaleY", scale, 1.0f);
        ObjectAnimator mAnimatorTranslateY = ObjectAnimator.ofFloat(view, "translationY", view.getTranslationY(), 0);

        mAnimatorSet.play(mAnimatorTranslateY).with(mAnimatorScaleX);
        mAnimatorSet.play(mAnimatorScaleX).with(mAnimatorScaleY);
        mAnimatorSet.setDuration(300);
        mAnimatorSet.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Debug.stopMethodTracing();
        unregisterReceiver(receiver);
    }

    private void checkAndUpdate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            update3();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                update3();
            } else {//申请权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    //基本更新
    private void update2() {
        UpdateAppUtils.from(this)
                .serverVersionCode(2)
                .serverVersionName("1.0.1")
                .apkPath(Const.APK_UPDATE_PATH)
                .updateInfo("1.修复若干bug\n2.美化部分页面\n")
                .update();
    }

    //通过浏览器下载
    private void update3() {
        UpdateAppUtils.from(this)
                .serverVersionCode(1)
                .serverVersionName("1.0.0")
                .apkPath(Const.APK_UPDATE_PATH)
                .downloadBy(UpdateAppUtils.DOWNLOAD_BY_BROWSER)
                .update();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    update3();
                } else {
                    new ConfirmDialog(this, position -> {
                        if (position==1){
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                            startActivity(intent);
                        }
                    }).setContent("暂无读写SD卡权限\n是否前往设置？").show();
                }
                break;
        }
    }
}
