package com.runvision.HttpCallback;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.runvision.bean.IDCard;
import com.runvision.bean.IDCardDao;
import com.runvision.bean.LogOutResponse;
import com.runvision.bean.LoginResponse;
import com.runvision.bean.Stulogin;
import com.runvision.core.Const;
import com.runvision.g702_sn.MainActivity;
import com.runvision.g702_sn.R;
import com.runvision.utils.IDUtils;
import com.runvision.utils.RSAUtils;
import com.runvision.utils.SPUtil;
import com.runvision.utils.TimeUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import es.dmoral.toasty.Toasty;
import okhttp3.Call;
import okhttp3.MediaType;

/**
 * 学员登录、登出网络请求
 */
public class HttpStudent {

    private static MediaPlayer mPlayer;//音频

    /**
     * 学员登录
     * @param context
     * @param devnum
     * @param time
     * @param stucode
     * @param cardtype
     * @param gps
     * @param imgstr
     * @param classcode
     * @param sn
     * @param studentName
     */
    public static void Stulogin(Context context, String devnum, String time, String stucode, String cardtype,
                                String gps, String imgstr, String classcode, String sn, String studentName, String sex, String snap, String card) {
        try {
            String privateKey = SPUtil.getString(Const.PRIVATE_KEY, "");
            String ts = TimeUtils.getTime13();
            String sign = devnum + time + stucode + cardtype + gps + imgstr + classcode + sn + studentName + ts;
            byte[] ss = sign.getBytes();
            String sign_str = RSAUtils.sign(ss, privateKey);

//            LogUtil.i("lichao", "Stulogin JSON:" + new Gson().toJson(new Stulogin(sign_str, devnum, time, stucode, cardtype, gps, imgstr, classcode, sn, studentName, ts)));

            String snapImageID = IDUtils.genImageName();
            String cardImageID = snapImageID + "_card";

            OkHttpUtils.postString()
                    .url(Const.STULOGIN + "ts=" + TimeUtils.getTime13() + "&sign=" + sign_str)
                    .content(new Gson().toJson(new Stulogin(sign_str, devnum, time, stucode, cardtype, gps, imgstr, classcode, sn, studentName, ts)))
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {
                            playMusic(context, R.raw.sign_fail);
                            Toasty.error(context, context.getString(R.string.toast_request_error), Toast.LENGTH_LONG, true).show();
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            Log.i("lichao", "success:" + response);
                            if (!response.equals("resource/500")) {
                                Gson gson = new Gson();
                                LoginResponse gsonLogin = gson.fromJson(response, LoginResponse.class);
                                if (gsonLogin.getErrorcode() == 0) {
                                    if (gsonLogin.getMessage().equals("操作成功")) {
                                        IDCard idCard = new IDCard();
                                        idCard.setName(studentName);
                                        idCard.setGender(sex);
                                        idCard.setId_card(stucode);
                                        idCard.setFacepic(snap);
                                        idCard.setIdcardpic(card);
                                        idCard.setSign_in(time);
                                        idCard.setSn(sn);
                                        MainActivity.idCardDao.insert(idCard);
                                        playMusic(context, R.raw.sign_success);
                                        Toasty.success(context, context.getString(R.string.toast_update_success), Toast.LENGTH_SHORT, true).show();
                                    } else {
                                        playMusic(context, R.raw.sign_fail);
                                        Toasty.warning(context, context.getString(R.string.toast_update_fail) + gsonLogin.getMessage(), Toast.LENGTH_LONG, true).show();
                                    }
                                } else {
                                    playMusic(context, R.raw.sign_fail);
                                    Toasty.error(context, context.getString(R.string.toast_update_fail) + gsonLogin.getMessage(), Toast.LENGTH_LONG, true).show();
                                }
                            } else {
                                playMusic(context, R.raw.sign_fail);
                                Toasty.error(context, context.getString(R.string.toast_server_error), Toast.LENGTH_LONG, true).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 学员登出
     * @param context
     * @param devnum
     * @param time
     * @param stucode
     * @param cardtype
     * @param gps
     * @param imgstr
     * @param classcode
     * @param sn
     * @param period
     * @param studentName
     */
    public static void Stulogout(Context context, String devnum, String time, String stucode, String cardtype, String gps, String imgstr, String classcode, String sn, int period, String studentName) {
        try {
            String privateKey = SPUtil.getString(Const.PRIVATE_KEY, "");
            String ts = TimeUtils.getTime13();
            String sign = devnum + time + stucode + cardtype + gps + imgstr + classcode + sn + period + studentName + ts;
            byte[] ss = sign.getBytes();
            String sign_str = RSAUtils.sign(ss, privateKey);

//            LogUtil.i("lichao", "StulogOut JSON:" + new Gson().toJson(new Stulogin(sign_str, devnum, time, stucode, cardtype, gps, imgstr, classcode, sn, period, studentName, ts)));

            OkHttpUtils.postString()
                    .url(Const.STULOGOUT + "ts=" + TimeUtils.getTime13() + "&sign=" + sign_str)
                    .content(new Gson().toJson(new Stulogin(sign_str, devnum, time, stucode, cardtype, gps, imgstr, classcode, sn, period, studentName, ts)))
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {
                            Toasty.error(context, context.getString(R.string.toast_request_error), Toast.LENGTH_LONG, true).show();
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            Log.i("lichao", "success:" + response);
                            if (!response.equals("resource/500")) {
                                Gson gson = new Gson();
                                LogOutResponse gsonLogOut = gson.fromJson(response, LogOutResponse.class);
                                if (gsonLogOut.getErrorcode().equals("0")) {
                                    if (gsonLogOut.getMessage().equals("操作成功")) {
                                        for (LogOutResponse.DataBean.CompleteDataBean cdb : gsonLogOut.getData().getCompleteData()) {
                                            Log.e("lichao", "完成科目:" + cdb.getSubject());
                                            Log.e("lichao", "完成学时:" + cdb.getCompleteHour());
                                        }

                                        //查询出SN
                                        IDCard delete_sn =  MainActivity.idCardDao.queryBuilder().where(IDCardDao.Properties.Id_card.eq(stucode)).unique();
                                        if (delete_sn != null) {
                                            MainActivity.idCardDao.deleteByKey(delete_sn.getId());
                                            playMusic(context, R.raw.sign_out_success);
                                            Toasty.success(context, context.getString(R.string.toast_update_success), Toast.LENGTH_SHORT, true).show();
                                        } else {
                                            playMusic(context, R.raw.replay_sign_out);
                                            Toasty.error(context, context.getString(R.string.toast_update_fail) + "重复签退或无签到记录", Toast.LENGTH_LONG, true).show();
                                        }
                                    } else {
                                        Toasty.error(context, context.getString(R.string.toast_update_fail) + gsonLogOut.getMessage(), Toast.LENGTH_LONG, true).show();
                                    }
                                } else {
                                    Toasty.error(context, context.getString(R.string.toast_update_fail) + gsonLogOut.getMessage(), Toast.LENGTH_LONG, true).show();
                                }
                            } else {
                                Toasty.error(context, context.getString(R.string.toast_server_error), Toast.LENGTH_LONG, true).show();
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放语音
     */
    public static void playMusic(Context context, int musicID) {
        if (!SPUtil.getBoolean(Const.KEY_ISOPENMUSIC, Const.OPEN_MUSIC)) {
            return;
        }
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.release();
            }
        }
        mPlayer = MediaPlayer.create(context, musicID);
        mPlayer.start();
    }
}
