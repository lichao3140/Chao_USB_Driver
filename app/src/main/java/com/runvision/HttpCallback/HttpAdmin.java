package com.runvision.HttpCallback;

import android.content.Context;
import android.widget.Toast;
import com.google.gson.Gson;
import com.runvision.bean.AppData;
import com.runvision.bean.FaceVerifyResponse;
import com.runvision.bean.Login;
import com.runvision.core.Const;
import com.runvision.g702_sn.R;
import com.runvision.utils.RSAUtils;
import com.runvision.utils.SPUtil;
import com.runvision.utils.TimeUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.MediaType;

/**
 * 管理员登录
 */
public class HttpAdmin {

    public static void adminLogin(Context context) {
        try {
            String privateKey = SPUtil.getString(Const.PRIVATE_KEY,"");
            String devnum = SPUtil.getString(Const.DEV_NUM,"");
            String username = SPUtil.getString("username", "");
            String ts = TimeUtils.getTime13();
            String sign = devnum + username +ts;
            byte[] ss = sign.getBytes();
            String sign_str = RSAUtils.sign(ss, privateKey);

            OkHttpUtils.postString()
                    .url(Const.FACE_WITNESS + "ts=" + TimeUtils.getTime13() + "&sign=" + sign_str)
                    .content(new Gson().toJson(new Login(sign_str, devnum, username, ts)))
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {
                            Toasty.error(context, context.getString(R.string.toast_request_error), Toast.LENGTH_LONG, true).show();
                        }

                        @Override
                        public void onResponse(String response, int id) {
//                            Log.i("lichao", "success:" + response);
                            if (!response.equals("resource/500")) {
                                Gson gson = new Gson();
                                FaceVerifyResponse gsonFace = gson.fromJson(response, FaceVerifyResponse.class);
                                if (gsonFace.getErrorcode().equals("0")) {
                                    AppData.getAppData().setAdmin_login_flag(true);
                                    Toasty.success(context, context.getString(R.string.toast_face_verify_success), Toast.LENGTH_LONG, true).show();
                                } else {
                                    AppData.getAppData().setAdmin_login_flag(false);
                                    Toasty.error(context, context.getString(R.string.toast_face_verify_fail) + gsonFace.getMessage(), Toast.LENGTH_LONG, true).show();
                                }
                            } else {
                                AppData.getAppData().setAdmin_login_flag(false);
                                Toasty.error(context, context.getString(R.string.toast_server_error), Toast.LENGTH_LONG, true).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
