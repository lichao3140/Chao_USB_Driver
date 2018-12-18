package com.runvision.g702_sn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.LivenessInfo;
import com.common.pos.api.util.PosUtil;
import com.runvision.bean.AppData;
import com.runvision.bean.FaceInfoss;
import com.runvision.bean.ImageStack;
import com.runvision.broadcast.NetWorkStateReceiver;
import com.runvision.broadcast.UdiskReceiver;
import com.runvision.core.Const;
import com.runvision.db.Record;
import com.runvision.db.User;
import com.runvision.frament.DeviceSetFrament;
import com.runvision.gpio.GPIOHelper;
import com.runvision.myview.MyCameraSuf;
import com.runvision.service.ProximityService;
import com.runvision.thread.BatchImport;
import com.runvision.thread.FaceFramTask;
import com.runvision.thread.HeartBeatThread;
import com.runvision.thread.SocketThread;
import com.runvision.utils.CameraHelp;
import com.runvision.utils.ConversionHelp;
import com.runvision.utils.DateTimeUtils;
import com.runvision.utils.FileUtils;
import com.runvision.utils.IDUtils;
import com.runvision.utils.LogToFile;
import com.runvision.utils.SPUtil;
import com.runvision.utils.SendData;
import com.runvision.utils.TestDate;
import com.runvision.utils.TimeCompareUtil;
import com.runvision.webcore.ServerManager;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.IdentityInfo;
import com.telpo.tps550.api.util.ShellUtils;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements NetWorkStateReceiver.INetStatusListener, View.OnClickListener {

    public static Context mContext;
    private Intent intentService;
    private MyRedThread mMyRedThread;//红外线程
    private UIThread uithread;
    private UDPServerThread mUDPServerThread;

    //////////////////////////////////////////////////视图控件
    public MyCameraSuf mCameraSurfView;
    private RelativeLayout home_layout;

    private View promptshow_xml;//提示框
    private TextView loadprompt;

    private View oneVsMoreView;  //1:N
    // private ImageView oneVsMore_face, oneVsMore_temper;

    private ImageView oneVsMore_temper;
    private TextView oneVsMore_userName, oneVsMore_userID, oneVsMore_userType;

    private View alert; //1:1
    private ImageView faceBmp_view, cardBmp_view, idcard_Bmp, isSuccessComper;
    private TextView card_name, card_sex, card_nation, name, year, month, day, addr, cardNumber, version;

    private ImageView home_set;

    private View pro_xml;//刷卡标记

    public int logshowflag = 0;

    private MediaPlayer mPlayer;//音频

    private boolean TipsFlag = false;

    private FaceFramTask faceDetectTask = null;
    private GetIDInfoTask mAsyncTask = null;

    private boolean bStop = false;

    private boolean oneVsMoreThreadStauts = false;
    private boolean isOpenOneVsMore = true;//1:N是否对比
    private boolean Infra_red = true;
    private ImageStack imageStack;
    private int timingnum = 0;//待机
    private String TAG = "MainActivity";

    private MyApplication application;
    // ----------------------------------------读卡器参数----------------------------------
    private static final int VID = 1024; // IDR VID
    private static final int PID = 50010; // IDR PID
    private IDCardReader idCardReader = null;
    private boolean ReaderCardFlag = true;//1:1是否对比

    //这个按钮是设置或以开关的
    private NetWorkStateReceiver receiver;
    private TextView socket_status;

    private SocketThread socketThread;
    private HeartBeatThread heartBeatThread;
    private TextView showHttpUrl;
    private ServerManager serverManager;
    private int socketErrorNum = 0;

    private int templatenum = 0;
    private int template = 0;
    private Toast mToast;
    private int destemplatenum = 0;
    private int destemplate = 0;

    private Boolean SysTimeflag = true;

    private List<User> mList;

    /**
     * 消息响应
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Const.UPDATE_UI://更新UI
                    //删除模板
                    if (Const.DELETETEMPLATE == true) {
                        mHandler.removeMessages(Const.MSG_FACE);
                        mHandler.removeMessages(Const.COMPER_END);
                        if (faceDetectTask != null) {
                            faceDetectTask.isRuning = false;
                        }
                        isOpenOneVsMore = false;
                        Infra_red = false;
                        if (mMyRedThread != null) {
                            mMyRedThread.closeredThread();
                        }
                        // home_layout.setVisibility(View.VISIBLE);
                        destemplatenum = 0;
                        destemplate++;
                        ReaderCardFlag = false;
                        Const.DELETETEMPLATE = false;
                        showToast("正在删除模板,停止比对！");
                    }

                    if (Const.DELETETEMPLATE == false) {
                        destemplatenum++;
                    }

                    if (destemplatenum == 20) {
                        promptshow_xml.setVisibility(View.GONE);
                        cancelToast();
                        ReaderCardFlag = true;//1:1
                        isOpenOneVsMore = true;//1:n
                        if (faceDetectTask != null) {
                            faceDetectTask.isRuning = true;//人脸框
                        }
                        Infra_red = true;
                        if (mMyRedThread != null) {
                            mMyRedThread.startredThread();
                        }
                        destemplate = 0;
                        // home_layout.setVisibility(View.GONE);
                    }

                    /*更新VMS连接*/
                    if (Const.WEB_UPDATE == true) {
                        Const.WEB_UPDATE = false;
                        if (!SPUtil.getString(Const.KEY_VMSIP, "").equals("")
                                && SPUtil.getInt(Const.KEY_VMSPROT, 0) != 0
                                && !SPUtil.getString(Const.KEY_VMSUSERNAME, "").equals("")
                                && !SPUtil.getString(Const.KEY_VMSPASSWORD, "").equals("")) {
                            //开启socket线程
                            socketReconnect(SPUtil.getString(Const.KEY_VMSIP, ""), SPUtil.getInt(Const.KEY_VMSPROT, 0));
                        }
                    }

                    /*每天清除访客模版数据*/
                    DateFormat df = new SimpleDateFormat("HH:mm:ss");
                    if (df.format(new Date()).equals("23:30:00")) {
                        DeleteVisitorRecord();
                    }

                    /*每天重启操作*/
                    if (df.format(new Date()).equals("02:00:00")) {
                        Log.i("Gavin", "data" + df.format(new Date()));
                        rebootSU();
                    }

                    /*设置删除数据操作*/
                    String time1 = TestDate.SGetSysTime();
                    if ((df.format(new Date()).equals("00:00:00")) && SysTimeflag == true) {
                        SysTimeflag = false;

                        String time11 = TestDate.timetodate(TestDate.getTime(time1));
                        String time22 = TestDate.getDateBefore(new Date(), SPUtil.getInt(Const.KEY_PRESERVATION_DAY, 90));

                        if (MyApplication.faceProvider.quaryUserTableRowCount("select count(id) from tUser") != 0) {
                            mList = MyApplication.faceProvider.getAllPoints();
                            for (int i = 0; i < mList.size(); i++) {
                                if (TimeCompare(time11, time22, TestDate.timetodate(String.valueOf(mList.get(i).getTime())))) {
                                    List<User> mList1 = MyApplication.faceProvider.queryRecord("select * from tRecord where id=" + (mList.get(i).getId()));
                                    FileUtils.deleteTempter(mList1.get(0).getTemplateImageID());
                                    FileUtils.deleteTempter(mList1.get(0).getRecord().getSnapImageID());
                                    MyApplication.faceProvider.deleteRecord(mList.get(i).getId());
                                }
                            }
                        }
                    }

                    /*休眠显示*/


                    /*显示逻辑*/
                    if(promptshow_xml.getVisibility() == View.VISIBLE) {
                        oneVsMoreView.setVisibility(View.GONE);
                        pro_xml.setVisibility(View.GONE);
                        // home_layout.setVisibility(View.GONE);
                    }
                    if(alert.getVisibility() == View.VISIBLE) {
                        // AppData.getAppData().setCompareScore(0);
                        home_layout.setVisibility(View.GONE);
                        oneVsMoreView.setVisibility(View.GONE);
                        pro_xml.setVisibility(View.GONE);
                    }
                    if(home_layout.getVisibility() == View.VISIBLE) {
                        oneVsMoreView.setVisibility(View.GONE);
                        // promptshow_xml.setVisibility(View.GONE);
                        alert.setVisibility(View.GONE);
                        pro_xml.setVisibility(View.GONE);
                        Infra_red = false;
                    }
                    if(isOpenOneVsMore == false) {
                        mHandler.removeMessages(Const.COMPER_END);
                        mHandler.removeMessages(Const.MSG_FACE);
                    }

                    /*导入模板显示*/
                    if ((Const.BATCH_IMPORT_TEMPLATE == true) && (Const.BATCH_FLAG == 1)) {
                        if (faceDetectTask != null) {
                            faceDetectTask.isRuning = false;
                        }
                        isOpenOneVsMore = false;
                        Infra_red = false;
                        if (mMyRedThread != null) {
                            mMyRedThread.closeredThread();
                        }
                        // home_layout.setVisibility(View.VISIBLE);
                        templatenum = 0;
                        template++;
                        ReaderCardFlag = false;
                        Const.BATCH_IMPORT_TEMPLATE = false;
                        Const.BATCH_FLAG = 2;
                        showToast("正在导入模板,停止比对！");
                    }
                    if ((template >= 5) || (Const.VMS_BATCH_IMPORT_TEMPLATE == true)) {
                        if (faceDetectTask != null) {
                            faceDetectTask.isRuning = false;
                        }
                        isOpenOneVsMore = false;
                        Infra_red = false;
                        if (mMyRedThread != null) {
                            mMyRedThread.closeredThread();
                        }
                        ReaderCardFlag = false;
                        oneVsMoreView.setVisibility(View.GONE);
                        alert.setVisibility(View.GONE);
                        home_layout.setVisibility(View.VISIBLE);
                        ShowPromptMessage("模板批量导入中！", 2);
                        cancelToast();
                    }

                    if ((Const.BATCH_IMPORT_TEMPLATE == false) && (Const.BATCH_FLAG == 2)) {
                        templatenum++;
                    }

                    if ((templatenum == 20) || (Const.VMS_TEMPLATE >= 20)) {
                        Const.VMS_TEMPLATE = 0;
                        Log.i("Gavin_debug", "templatenum==20");
                        promptshow_xml.setVisibility(View.GONE);
                        cancelToast();
                        ReaderCardFlag = true;//1:1
                        isOpenOneVsMore = true;//1:n
                        if (faceDetectTask != null) {
                            faceDetectTask.isRuning = true;//人脸框
                        }
                        Infra_red = true;
                        if (mMyRedThread != null) {
                            mMyRedThread.startredThread();
                        }
                        template = 0;
                        // home_layout.setVisibility(View.GONE);
                    }


                    /*更新IP后的web重启*/
                    if (Const.UPDATE_IP == true) {
                        int returndate = DeviceSetFrament.updateSetting(AppData.getAppData().getUpdatedeviceip(), mContext);
                        if (returndate == 3) {
                            mHandler.postDelayed(() -> openHttpServer(), 3000);
                        }
                        Const.UPDATE_IP = false;
                    }

                    if (SPUtil.getBoolean(Const.KEY_ISOPEN_ONE, Const.OPEN_ONE_VS_ONE)) {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(Const.MSG_READ_CARD, ""), 100);
                    }
                    break;

                case Const.MSG_FACE://开启一比n处理
                    if (SPUtil.getBoolean(Const.KEY_ISOPEN_N, Const.OPEN_ONE_VS_N)) {
                        FaceInfoss info = (FaceInfoss) msg.obj;
                        openOneVsMoreThread(info);
                    }
                    break;
                case Const.MSG_READ_CARD:
                    mHandler.removeMessages(Const.MSG_READ_CARD);
                    startIdCardThread();
                    break;
                case Const.READ_CARD_INFO:
                    mHandler.removeMessages(Const.COMPER_FINIASH);
                    mHandler.removeMessages(Const.READ_CARD_INFO);
                    mHandler.removeMessages(Const.COMPER_END);
                    oneVsMoreView.setVisibility(View.GONE);
                    home_layout.setVisibility(View.GONE);
                    pro_xml.setVisibility(View.VISIBLE);
                    IdentityInfo identityInfo = (IdentityInfo) msg.obj;
                    toComperFace1(identityInfo);
                    break;
                case Const.READ_CARD://收到读卡器的信息
                    mHandler.removeMessages(Const.COMPER_FINIASH);
                    mHandler.removeMessages(Const.READ_CARD);
                    mHandler.removeMessages(Const.COMPER_END);
                    oneVsMoreView.setVisibility(View.GONE);
                    home_layout.setVisibility(View.GONE);
                    pro_xml.setVisibility(View.VISIBLE);
                    IDCardInfo Idinfo = (IDCardInfo) msg.obj;
                    toComperFace(Idinfo);
                    break;
                case Const.COMPER_END://1:n比对显示
                    showAlert();
                    break;
                case Const.COMPER_FINIASH://身份证比对完显示
                    mHandler.removeMessages(Const.COMPER_FINIASH);
                    mHandler.removeMessages(Const.COMPER_END);
                    oneVsMoreView.setVisibility(View.GONE);
                    // pro_xml.setVisibility(View.GONE);
                    int count2 = (Integer) msg.obj;

                    if (count2 > 0) {
                        Message msg3 = obtainMessage();
                        msg3.what = Const.COMPER_FINIASH;
                        msg3.obj = count2 - 1;
                        sendMessageDelayed(msg3, 1000);
                    }
                    if (count2 == 4) {
                        pro_xml.setVisibility(View.GONE);
                        showAlertDialog();
                        Message msg3 = obtainMessage();
                        msg3.what = Const.COMPER_FINIASH;
                        msg3.obj = count2 - 1;
                        sendMessageDelayed(msg3, 1000);
                    }
                    if (count2 == 0) {
                        isOpenOneVsMore = true;
                    }
                    break;
                case Const.TEST_INFRA_RED://红外处理
                    int count1 = (Integer) msg.obj;
                    if (count1 > 0) {
                        Message msg3 = obtainMessage();
                        msg3.what = Const.TEST_INFRA_RED;
                        msg3.obj = count1 - 1;
                        sendMessageDelayed(msg3, 1000);
                    }
                    if (count1 == 0) {
                        if (Const.IS_SYSTEM_STAND_BY) {
                            home_layout.setVisibility(View.GONE);
                        }
                        stratThread();
                        Infra_red = true;
                        bStop = false;

                        if (uithread == null) {
                            uithread = new UIThread();
                            uithread.start();
                        }
                        /*待机显示*/
                        if(faceDetectTask != null) {
                            if(faceDetectTask.faceflag == false) {
                                timingnum++;
                                if (timingnum >= 180 && Const.IS_SYSTEM_STAND_BY) {
                                    home_layout.setVisibility(View.VISIBLE);
                                    Const.IS_SYSTEM_STAND_BY = false;
                                }
                            } else {
                                home_layout.setVisibility(View.GONE);
                                timingnum = 0;
                                Const.IS_SYSTEM_STAND_BY = true;
                            }
                        }
                    }
                    break;
                case Const.FLAG_SHOW_LOG://待机处理
                    int count4 = (Integer) msg.obj;
                    oneVsMoreView.setVisibility(View.GONE);
                    promptshow_xml.setVisibility(View.GONE);
                    alert.setVisibility(View.GONE);
                    pro_xml.setVisibility(View.GONE);
                    Infra_red = false;
                    if (count4 > 0) {
                        Message msgb = obtainMessage();
                        msgb.what = Const.FLAG_SHOW_LOG;
                        msgb.obj = count4 - 1;
                        sendMessageDelayed(msgb, 1000);
                    }
                    if (count4 == 0) {
                        home_layout.setVisibility(View.VISIBLE);
                        mCameraSurfView.releaseCamera();
                    }
                    break;
                case Const.SOCKET_LOGIN: /*socket设备登陆*/
                    boolean isSuccess = (boolean) msg.obj;
                    if (isSuccess) {
                        Toast.makeText(mContext, "socket登录成功", Toast.LENGTH_SHORT).show();
                        LogToFile.i("MainActivity", "socket登录成功");
                        socket_status.setBackgroundResource(R.drawable.socket_true);
                        //开启心跳
                        if (heartBeatThread != null) {
                            heartBeatThread.HeartBeatThread_flag = false;
                            heartBeatThread = null;
                        }
                        heartBeatThread = new HeartBeatThread(socketThread);
                        heartBeatThread.start();
                    } else {
                        socket_status.setBackgroundResource(R.drawable.socket_false);
                        LogToFile.i("MainActivity", "socket登录失败");
                        Toast.makeText(mContext, "socket登录失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Const.SOCKET_TIMEOUT:/*socket连接超时*/
                    socket_status.setBackgroundResource(R.drawable.socket_false);
                    String prompt = (String) msg.obj;
                    LogToFile.i("MainActivity", prompt);
                    Toast.makeText(mContext, prompt, Toast.LENGTH_SHORT).show();
                    break;
                case Const.SOCKET_DIDCONNECT:/*socket断开连接*/
                    socket_status.setBackgroundResource(R.drawable.socket_false);
                    closeSocket();
                    break;
                case Const.SOCKRT_SENDIMAGE:/*VMS批量导入操作*/
                    batchImport();
                    break;
                case 100:/*VMS批量导入结束操作---一个线程*/
                    int success0 = (int) msg.obj;
                    bacthOk0 = success0;
                    Log.e("lichaoo", "100:" + bacthOk0 + Const.VMS_ERROR_TEMPLATE + "=" + mSum);
                    if (bacthOk0 + Const.VMS_ERROR_TEMPLATE >= mSum) {
                        Const.VMS_TEMPLATE = Const.VMS_TEMPLATE + 20;
                        Const.VMS_BATCH_IMPORT_TEMPLATE = false;
                    }
                    break;
                case 101:/*VMS批量导入结束操作---三个线程*/
                    int success1 = (int) msg.obj;
                    bacthOk1 = success1;
//                    Log.e("lichaoo", "101:" + bacthOk1 + bacthOk2 + bacthOk3 + "=" + mSum);
                    if (bacthOk1 + bacthOk2 + bacthOk3 == mSum) {
                        Const.VMS_TEMPLATE = Const.VMS_TEMPLATE + 20;
                        Const.VMS_BATCH_IMPORT_TEMPLATE = false;
                    }
                    break;
                case 102:/*VMS批量导入结束操作*/
                    int success2 = (int) msg.obj;
                    bacthOk2 = success2;
//                    Log.e("lichaoo", "102:" + bacthOk1 + bacthOk2 + bacthOk3 + "=" + mSum);
                    if (bacthOk1 + bacthOk2 + bacthOk3 == mSum) {
                        Const.VMS_TEMPLATE = Const.VMS_TEMPLATE + 20;
                        Const.VMS_BATCH_IMPORT_TEMPLATE = false;
                    }
                    break;
                case 103:/*VMS批量导入结束操作*/
                    int success3 = (int) msg.obj;
                    bacthOk3 = success3;
//                    Log.e("lichaoo", "103:" + bacthOk1 + bacthOk2 + bacthOk3 + "=" + mSum);
                    if (bacthOk1 + bacthOk2 + bacthOk3 == mSum) {
                        Const.VMS_TEMPLATE = Const.VMS_TEMPLATE + 20;
                        Const.VMS_BATCH_IMPORT_TEMPLATE = false;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * ACTIVITY周期
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_main);
        // 全屏代码
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideBottomUIMenu();
        initView();
        mContext = this;

        application = (MyApplication) getApplication();
        application.init();
        application.addActivity(this);

        intentService = new Intent(mContext, ProximityService.class);
        startService(intentService);

        openNetStatusReceiver();
        openSocket();

        //监听U盘热插拔模块启动
        udiskPluggedin();

        if (mUDPServerThread == null) {
            mUDPServerThread = new UDPServerThread();
            mUDPServerThread.start();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ShellUtils.execCommand("echo 3 > /sys/class/telpoio/power_status", false);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!SPUtil.getBoolean(Const.KEY_ISOPEN_ONE, Const.OPEN_ONE_VS_ONE)) {
            if (mAsyncTask != null) {
                mAsyncTask.setTaskIsRuning(false);
                mAsyncTask = null;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBottomUIMenu();
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        //USB身份证读卡
        startIDCardReader();
        startService(intentService);

        if (uithread == null) {
            uithread = new UIThread();
            uithread.start();
        }

        if (mMyRedThread == null) {
            mMyRedThread = new MyRedThread();  //红外
            mMyRedThread.start();
        }

        if (!SPUtil.getBoolean(Const.KEY_ISOPEN_ONE, Const.OPEN_ONE_VS_ONE)) {
            if (mAsyncTask != null) {
                mAsyncTask.setTaskIsRuning(false);
                mAsyncTask = null;
            }
        }
        mMyRedThread.startredThread();

        isOpenOneVsMore = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //关闭相机线程
        Infra_red = false;
        mCameraSurfView.releaseCamera();
        //关闭红外
        mMyRedThread.closeredThread();
        stopService(intentService);

        if (mMyRedThread != null) {
            mMyRedThread.interrupt();
            mMyRedThread = null;
        }

        //关闭人脸框线程
        if (faceDetectTask != null) {
            faceDetectTask.setRuning(false);
            faceDetectTask.cancel(false);
            faceDetectTask = null;
        }
        //关闭串口身份证读取
        if (mAsyncTask != null) {
            mAsyncTask.setTaskIsRuning(false);
            mAsyncTask = null;
        }
        //关闭未播报完语音
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.release();
                mPlayer = null;
            }
        }
        isOpenOneVsMore = false;
        bStop = true;
        try {
            idCardReader.close(0);
        } catch (IDCardReaderException e) {
            Log.i(TAG, "关闭失败");
        }
        IDCardReaderFactory.destroy(idCardReader);
        unregisterReceiver(mUsbReceiver);
    }

    /**
     * 初始化视图控件
     */
    private void initView() {
        mCameraSurfView = (MyCameraSuf) findViewById(R.id.myCameraView);
        imageStack = mCameraSurfView.getImgStack();
        home_layout = (RelativeLayout) findViewById(R.id.home_layout);//待机界面

        // 提示框
        promptshow_xml = findViewById(R.id.promptshow_xml);
        loadprompt = (TextView) promptshow_xml.findViewById(R.id.loadprompt);

        //1:N
        oneVsMoreView = findViewById(R.id.onevsmore);
        //  oneVsMore_face = oneVsMoreView.findViewById(R.id.onevsmore_face);
        oneVsMore_temper = oneVsMoreView.findViewById(R.id.onevsmore_temper);
        oneVsMore_userName = oneVsMoreView.findViewById(R.id.onevsmore_userName);
        oneVsMore_userID = oneVsMoreView.findViewById(R.id.onevsmore_userID);
        oneVsMore_userType = oneVsMoreView.findViewById(R.id.onevsmore_userType);

        //1:1
        alert = findViewById(R.id.alert_xml);
        faceBmp_view = (ImageView) alert.findViewById(R.id.comperFacebm);
        cardBmp_view = (ImageView) alert.findViewById(R.id.comperCardbm);
        idcard_Bmp = (ImageView) alert.findViewById(R.id.cardImage);
        card_name = (TextView) alert.findViewById(R.id.name_1);
        name = (TextView) alert.findViewById(R.id.userName);
        card_sex = (TextView) alert.findViewById(R.id.sex);
        card_nation = (TextView) alert.findViewById(R.id.nation);
        year = (TextView) alert.findViewById(R.id.year);
        day = (TextView) alert.findViewById(R.id.day);
        month = (TextView) alert.findViewById(R.id.month);
        addr = (TextView) alert.findViewById(R.id.addr);
        cardNumber = (TextView) alert.findViewById(R.id.cardNumber);
        isSuccessComper = (ImageView) alert.findViewById(R.id.isSuccessComper);

        //刷卡标记
        pro_xml = findViewById(R.id.pro);

        socket_status = findViewById(R.id.socket_status);
        showHttpUrl = findViewById(R.id.showHttpUrl);

        home_set = findViewById(R.id.home_set);
        home_set.setOnClickListener(view -> {
            showConfirmPsdDialog();
            isOpenOneVsMore = false;
        });
    }

    /**
     * 监听U盘热插拔
     */
    private void udiskPluggedin() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        filter.addAction("android.hardware.action.USB_DISCONNECTED");
        filter.addAction("android.hardware.action.USB_CONNECTED");

        filter.addAction("android.intent.action.UMS_CONNECTED");
        filter.addAction("android.intent.action.UMS_DISCONNECTED");
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addDataScheme("file");
        UdiskReceiver mReceiver = new UdiskReceiver();
        registerReceiver(mReceiver, filter);
    }

    /**
     * 开启身份证读取线程
     */
    private void startIdCardThread() {
        if (mAsyncTask != null) {
            mAsyncTask.setTaskIsRuning(false);
            mAsyncTask = null;
        }
        mAsyncTask = new GetIDInfoTask();
        mAsyncTask.setTaskIsRuning(true);
        mAsyncTask.execute();
    }

    /**
     * 开启画人脸框线程
     */
    private void stratThread() {
        if (faceDetectTask != null) {
            faceDetectTask.setRuning(false);
            faceDetectTask = null;
        }
        faceDetectTask = new FaceFramTask(mHandler, mCameraSurfView);
        faceDetectTask.setRuning(true);
        faceDetectTask.execute();
    }


    /**
     * 开启一个1：N的线程
     */
    private void openOneVsMoreThread(FaceInfoss info) {
        if (!oneVsMoreThreadStauts && isOpenOneVsMore && Infra_red) {
            oneVsMoreThreadStauts = true;
            OneVsMoreThread thread = new OneVsMoreThread(info);
            thread.start();
        }
    }

    /**
     * 身份证读取
     */
    private void toComperFace(final IDCardInfo idCardInfo) {
        if (idCardInfo.getPhotolength() > 0) {
            byte[] buf = new byte[WLTService.imgLength];
            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                final Bitmap cardBmp = IDPhotoHelper.Bgr2Bitmap(buf);
                if (cardBmp != null) {
                    synchronized (this) {
                        new Thread(() -> {
                            faceComperFrame(cardBmp);
                            AppData.getAppData().setName(idCardInfo.getName());
                            AppData.getAppData().setSex(idCardInfo.getSex());
                            AppData.getAppData().setNation(idCardInfo.getNation());
                            AppData.getAppData().setBirthday(idCardInfo.getBirth());
                            AppData.getAppData().setAddress(idCardInfo.getAddress());
                            AppData.getAppData().setCardNo(idCardInfo.getId());
                            AppData.getAppData().setCardBmp(cardBmp);
                            Message msg = new Message();
                            msg.obj = 5;
                            msg.what = Const.COMPER_FINIASH;
                            mHandler.sendMessage(msg);
                        }).start();
                    }
                } else {
                    Log.i(TAG, "读卡器解码得到的图片为空");
                }
            } else {
                Log.i(TAG, "图片解码 error");
                Toast.makeText(mContext, "身份证图片解码失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.i(TAG, "图片数据长度为0");
            Toast.makeText(mContext, "图片数据长度为0" + idCardInfo.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 串口读卡器
     *
     * @param identityInfo
     */
    private void toComperFace1(final IdentityInfo identityInfo) {
        if (cardBitmap != null) {
            synchronized (this) {
                new Thread(() -> {
                    faceComperFrame(cardBitmap);
                    AppData.getAppData().setName(identityInfo.getName().replace(" ", ""));
                    AppData.getAppData().setSex(identityInfo.getSex().substring(0, 1));
                    AppData.getAppData().setNation(identityInfo.getNation());
                    AppData.getAppData().setBirthday(identityInfo.getBorn());
                    AppData.getAppData().setAddress(identityInfo.getAddress());
                    AppData.getAppData().setCardNo(identityInfo.getNo());
                    AppData.getAppData().setCardBmp(cardBitmap);
                    Message msg = new Message();
                    msg.obj = 5;
                    msg.what = Const.COMPER_FINIASH;
                    mHandler.sendMessage(msg);
                }).start();
            }
        } else {
            Log.i(TAG, "读卡器解码得到的图片为空");
        }
    }

    /*1：1比对操作*/
    public void faceComperFrame(Bitmap bmp) {
        //提取人脸
        List<FaceInfo> result = new ArrayList<FaceInfo>();
        List<LivenessInfo> livenessInfoList = new ArrayList<>();
        byte[] des = CameraHelp.rotateCamera(imageStack.pullImageInfo().getData(), 640, 480, 90);

        MyApplication.mFaceLibCore.FaceDetection(des, 480, 640, result);
        if (result.size() == 0) {
            return;
        }
        Boolean live = true;
        if (SPUtil.getBoolean(Const.KEY_ISOPENLIVE, Const.OPEN_LIVE)) {
            live = MyApplication.mFaceLibCore.Livingthing(des, 480, 640, result, livenessInfoList);
        }
        if (!live) {
            return;
        }
        AppData.getAppData().setOneFaceBmp(CameraHelp.getXFaceImgByInfraredJpg(result.get(0).getRect().left, result.get(0).getRect().top, result.get(0).getRect().right, result.get(0).getRect().bottom, CameraHelp.getBitMap(des)));
        // AFR_FSDKFace face = new AFR_FSDKFace();
        FaceFeature faceFeature = new FaceFeature();
        int ret = MyApplication.mFaceLibCore.FaceFeatureExtract(des, 480, 640, result.get(0), faceFeature);
        if (ret != 0) {
            return;
        }

        //提取身份证
        bmp = CameraHelp.alignBitmapForNv21(bmp);//裁剪
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        byte[] cardDes = CameraHelp.bitmapToNv21(bmp, w, h);//转nv21
        List<FaceInfo> result_card = new ArrayList<FaceInfo>();
        MyApplication.mFaceLibCore.FaceDetection(cardDes, w, h, result_card);

        if (result_card.size() == 0) {
            return;
        }
        FaceFeature card = new FaceFeature();
        ret = MyApplication.mFaceLibCore.FaceFeatureExtract(cardDes, w, h, result_card.get(0), card);
        if (ret != 0) {
            return;
        }

        FaceSimilar score = new FaceSimilar();
        while (true) {
            MyApplication.mFaceLibCore.FacePairMatching(faceFeature, card, score);
            if (score.getScore() == 0) {
                break;
            } else {
                AppData.getAppData().setoneCompareScore(score.getScore());
                break;
            }
        }
    }


    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.e(TAG, "拔出usb了");
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.e(TAG, "设备的ProductId值为：" + device.getProductId());
                    Log.e(TAG, "设备的VendorId值为：" + device.getVendorId());
                    if (device.getProductId() == PID && device.getVendorId() == VID) {
                        bStop = true;
                        try {
                            idCardReader.close(0);
                        } catch (IDCardReaderException e) {
                            Log.i(TAG, "关闭失败");
                        }
                        IDCardReaderFactory.destroy(idCardReader);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.e(TAG, "插入usb了");
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device.getProductId() == PID && device.getVendorId() == VID) {
                    // 读卡器
                    startIDCardReader();
                }
            }
        }
    };

    /**
     * 读卡器 初始化
     */
    private void startIDCardReader() {
        LogHelper.setLevel(Log.ASSERT);
        Map idrparams = new HashMap();
        idrparams.put(ParameterHelper.PARAM_KEY_VID, VID);
        idrparams.put(ParameterHelper.PARAM_KEY_PID, PID);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.USB, idrparams);
        readCard();
    }


    private void readCard() {
        try {
            idCardReader.open(0);
            bStop = false;
            Log.i(TAG, "设备连接成功");
            new Thread(() -> {
                while (!bStop) {
                    long begin = System.currentTimeMillis();
                    IDCardInfo idCardInfo = new IDCardInfo();
                    boolean ret = false;
                    try {
                        idCardReader.findCard(0);
                        idCardReader.selectCard(0);
                    } catch (IDCardReaderException e) {
                        continue;
                    }
                    if (ReaderCardFlag == true) {
                        try {
                            ret = idCardReader.readCard(0, 0, idCardInfo);
                        } catch (IDCardReaderException e) {
                            Log.i(TAG, "读卡失败，错误信息：" + e.getMessage());
                        }
                        if (ret) {
                            Const.ONE_VS_MORE_TIMEOUT_NUM = 0;
                            isOpenOneVsMore = false;
                            ReaderCardFlag = false;
                            final long nTickUsed = (System.currentTimeMillis() - begin);
                            Log.i(TAG, "success>>>" + nTickUsed + ",name:" + idCardInfo.getName() + "," + idCardInfo.getValidityTime() + "，" + idCardInfo.getDepart());
                            Message msg = new Message();
                            msg.what = Const.READ_CARD;
                            msg.obj = idCardInfo;
                            mHandler.sendMessage(msg);
                        }
                    }
                }

            }).start();

        } catch (IDCardReaderException e) {
            Log.i(TAG, "连接设备失败");
            Log.i(TAG, "开始读卡失败，错误码：" + e.getErrorCode() + "\n错误信息："
                    + e.getMessage() + "\n内部代码="
                    + e.getInternalErrorCode());
            //Toast.makeText(mContext, "连接读卡器失败:" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    @SuppressLint("NewApi")
    protected void hideBottomUIMenu() {
        // 隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     * 提示显示框
     */
    private void ShowPromptMessage(String showmessage, int audionum) {
        if (audionum == 1) {
//            playMusic(R.raw.burlcard);
            playMusic(R.raw.please_register_face);
        }
        if (audionum == 3) {
            playMusic(R.raw.blacklist);
        }
        loadprompt.setText(showmessage);
        promptshow_xml.setVisibility(View.VISIBLE);
        if (audionum != 2) {
            mHandler.postDelayed(() -> promptshow_xml.setVisibility(View.GONE), 1500);
        }
    }

    /**
     * 1vsn显示对比后成功是否窗口
     */
    private void showAlert() {
        if ((isOpenOneVsMore != false) || (Const.DELETETEMPLATE == false)) {
            if (AppData.getAppData().getCompareScore() <= SPUtil.getFloat(Const.KEY_ONEVSMORESCORE, Const.ONEVSMORE_SCORE) && Const.ONE_VS_MORE_TIMEOUT_NUM >= Const.ONE_VS_MORE_TIMEOUT_MAXNUM) {
                if (promptshow_xml.getVisibility() != View.VISIBLE) {
                    Const.ONE_VS_MORE_TIMEOUT_NUM = 0;
                    ShowPromptMessage("请联系管理员或注册人脸", 1);
                }
            } else if (AppData.getAppData().getCompareScore() > SPUtil.getFloat(Const.KEY_ONEVSMORESCORE, Const.ONEVSMORE_SCORE) && AppData.getAppData().getNFaceBmp() != null) {
                //防止连续两次识别

                String sdCardDir = null;
                Const.ONE_VS_MORE_TIMEOUT_NUM = 0;
                String snapImageID = IDUtils.genImageName();
                //  oneVsMore_face.setImageBitmap(AppData.getAppData().getNFaceBmp());
                FileUtils.saveFile(AppData.getAppData().getNFaceBmp(), snapImageID, TestDate.DGetSysTime() + "_Face");
                User user = MyApplication.faceProvider.getUserByUserId(AppData.getAppData().getUser().getId());
                AppData.getAppData().setUser(user);
                if (user.getTemplateImageID() != null) {
                    sdCardDir = Environment.getExternalStorageDirectory() + "/FaceAndroid/FaceTemplate/" + user.getTemplateImageID() + ".jpg";
                }
                try {
                    if (sdCardDir != null) {
                        Bitmap bmp = BitmapFactory.decodeFile(sdCardDir);
                        AppData.getAppData().setCardBmp(bmp);
                        oneVsMore_temper.setImageBitmap(bmp);
                    }
                } catch (Exception e) {
                    oneVsMore_temper.setImageResource(R.mipmap.ic_launcher);
                }
                oneVsMore_userName.setText(user.getName());
                oneVsMore_userType.setText(user.getType());
                oneVsMore_userID.setText(user.getWordNo());
                com.runvision.core.LogToFile.e("1:N", "1:N成功: 姓名：" + user.getName() + ",分数：" + AppData.getAppData().getCompareScore());
                user.setTime(DateTimeUtils.getTime());
                Record record = new Record(AppData.getAppData().getCompareScore() + "", "成功", Environment.getExternalStorageDirectory() + "/FaceAndroid/" + TestDate.DGetSysTime() + "_Face" + "/" + snapImageID, "1:N");
                user.setRecord(record);
                MyApplication.faceProvider.addRecord(user);

                if (user.getType().equals("黑名单")) {
                    ShowPromptMessage("黑名单", 3);
                } else {
                    GPIOHelper.openDoor(true);
                    PosUtil.setRelayPower(1);//开闸
//                    PosUtil.getWg34Status(13701163);
//                    Log.i(TAG, "WG:" + PosUtil.getWg26Status(23821899));
//                    showToast("Wg34:" + PosUtil.getWg34Status(13701163));
                    mHandler.postDelayed(() -> {
                        GPIOHelper.openDoor(false);
                        PosUtil.setRelayPower(0);//关闸
                    }, SPUtil.getInt(Const.KEY_OPENDOOR, Const.CLOSE_DOOR_TIME) * 1000);
                    oneVsMoreView.setVisibility(View.VISIBLE);
                    playMusic(R.raw.success);
                    mHandler.postDelayed(() -> oneVsMoreView.setVisibility(View.GONE), 1000);
                }
                //发送到VMS
                if (socketThread != null) {
                    SendData.sendComperMsgInfo(socketThread, true, Const.TYPE_ONEVSMORE);
                } else {
                    AppData.getAppData().clean();
                }

            } else if (AppData.getAppData().getCompareScore() != 0) {
                Const.ONE_VS_MORE_TIMEOUT_NUM++;
            }

        }
    }

    /**
     * 1vs1显示对比后成功是否窗口
     */
    private void showAlertDialog() {
        String str = "";
        cardBmp_view.setImageBitmap(AppData.getAppData().getCardBmp());
        idcard_Bmp.setImageBitmap(AppData.getAppData().getCardBmp());
        card_name.setText(AppData.getAppData().getName());
        card_sex.setText(AppData.getAppData().getSex());
        name.setText(AppData.getAppData().getName());
        year.setText(AppData.getAppData().getBirthday().substring(0, 4));
        month.setText(AppData.getAppData().getBirthday().substring(5, 7));
        day.setText(AppData.getAppData().getBirthday().substring(8, 10));
        addr.setText(AppData.getAppData().getAddress());
        cardNumber.setText(AppData.getAppData().getCardNo().substring(0, 4)
                + "************"
                + AppData.getAppData().getCardNo().substring(16, 18));
        card_nation.setText(AppData.getAppData().getNation());
        faceBmp_view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if ((AppData.getAppData().getoneCompareScore() == 0) || (AppData.getAppData().getoneCompareScore() < SPUtil.getFloat(Const.KEY_CARDSCORE, Const.ONEVSONE_SCORE) && AppData.getAppData().getOneFaceBmp() != null)) {
            str = "核验失败，请联系管理员";
            isSuccessComper.setImageResource(R.mipmap.icon_sb);
            if (AppData.getAppData().getOneFaceBmp() == null) {
                faceBmp_view.setImageResource(R.mipmap.tx);
                faceBmp_view.setScaleType(ImageView.ScaleType.FIT_XY);
            } else {
                faceBmp_view.setImageBitmap(AppData.getAppData().getOneFaceBmp());
                //保存抓拍图片
                String snapImageID = IDUtils.genImageName();
                FileUtils.saveFile(AppData.getAppData().getOneFaceBmp(), snapImageID, TestDate.DGetSysTime() + "_Face");
                //保存身份证图片
                String cardImageID = snapImageID + "_card";
                FileUtils.saveFile(AppData.getAppData().getCardBmp(), cardImageID, TestDate.DGetSysTime() + "_Card");

                Record record = new Record(AppData.getAppData().getoneCompareScore() + "", str, Environment.getExternalStorageDirectory() + "/FaceAndroid/" + TestDate.DGetSysTime() + "_Face" + "/" + snapImageID, "人证");
                User user = new User(AppData.getAppData().getName(), "无", AppData.getAppData().getSex(), 0, "无", AppData.getAppData().getCardNo(), Environment.getExternalStorageDirectory() + "/FaceAndroid/" + TestDate.DGetSysTime() + "_Card" + "/" + cardImageID, DateTimeUtils.getTime());
                user.setRecord(record);
                MyApplication.faceProvider.addRecord(user);

            }
            oneVsMoreView.setVisibility(View.GONE);
            alert.setVisibility(View.VISIBLE);
            playMusic(R.raw.error);
        } else if (AppData.getAppData().getOneFaceBmp() != null && AppData.getAppData().getoneCompareScore() >= SPUtil.getFloat(Const.KEY_CARDSCORE, Const.ONEVSONE_SCORE)) {
            str = "核验成功，请通行";
            playMusic(R.raw.success);
            isSuccessComper.setImageResource(R.mipmap.icon_tg);
            faceBmp_view.setImageBitmap(AppData.getAppData().getOneFaceBmp());
            GPIOHelper.openDoor(true);
            PosUtil.setRelayPower(1);//开闸

            mHandler.postDelayed(() -> {
                GPIOHelper.openDoor(false);
                PosUtil.setRelayPower(0);//关闸
            }, SPUtil.getInt(Const.KEY_OPENDOOR, Const.CLOSE_DOOR_TIME) * 1000);

            //保存抓拍图片
            String snapImageID = IDUtils.genImageName();
            if (AppData.getAppData().getOneFaceBmp() != null) {
                FileUtils.saveFile(AppData.getAppData().getOneFaceBmp(), snapImageID, TestDate.DGetSysTime() + "_Face");
            }
            //保存身份证图片
            String cardImageID = snapImageID + "_card";
            if (AppData.getAppData().getCardBmp() != null) {
                FileUtils.saveFile(AppData.getAppData().getCardBmp(), cardImageID, TestDate.DGetSysTime() + "_Card");
            }

            Record record = new Record(AppData.getAppData().getoneCompareScore() + "", str, Environment.getExternalStorageDirectory() + "/FaceAndroid/" + TestDate.DGetSysTime() + "_Face" + "/" + snapImageID, "人证");
            User user = new User(AppData.getAppData().getName(), "无", AppData.getAppData().getSex(), 0, "无", AppData.getAppData().getCardNo(), Environment.getExternalStorageDirectory() + "/FaceAndroid/" + TestDate.DGetSysTime() + "_Card" + "/" + cardImageID, DateTimeUtils.getTime());
            user.setRecord(record);
            MyApplication.faceProvider.addRecord(user);

            mHandler.postDelayed(() -> {
                GPIOHelper.openDoor(false);
                PosUtil.setRelayPower(0);//关闸
            }, 1000);
            oneVsMoreView.setVisibility(View.GONE);
            alert.setVisibility(View.VISIBLE);

        } else {
            oneVsMoreView.setVisibility(View.GONE);
            alert.setVisibility(View.GONE);
        }

        if (AppData.getAppData().getoneCompareScore() < SPUtil.getFloat(Const.KEY_CARDSCORE, Const.ONEVSONE_SCORE) && AppData.getAppData().getOneFaceBmp() != null) {
            //    Log.i("Gavin","人证失败："+socketThread.toString());
            if (socketThread != null) {
                SendData.sendComperMsgInfo(socketThread, false, Const.TYPE_CARD);
            } else {
                AppData.getAppData().clean();
            }
        }
        if (AppData.getAppData().getoneCompareScore() >= SPUtil.getFloat(Const.KEY_CARDSCORE, Const.ONEVSONE_SCORE) && AppData.getAppData().getOneFaceBmp() != null) {
//            Log.i("Gavin","人证成功："+socketThread.toString());
            if (socketThread != null) {
                SendData.sendComperMsgInfo(socketThread, true, Const.TYPE_CARD);
            } else {
                AppData.getAppData().clean();
            }
        }

        AppData.getAppData().setoneCompareScore(0);
        ReaderCardFlag = true;
        mHandler.postDelayed(() -> {
            oneVsMoreView.setVisibility(View.GONE);
            alert.setVisibility(View.GONE);
        }, 2000);
    }


    /**
     * 播放语音
     */
    public void playMusic(int musicID) {
        if (!SPUtil.getBoolean(Const.KEY_ISOPENMUSIC, Const.OPEN_MUSIC)) {
            return;
        }
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.release();
            }
        }
        mPlayer = MediaPlayer.create(mContext, musicID);
        mPlayer.start();
    }

    /**
     * 1：N比对操作线程
     */
    class OneVsMoreThread extends Thread {
        private FaceInfoss info;
        FaceFeature face;
        FaceSimilar score;
        User user;

        public OneVsMoreThread(FaceInfoss info) {
            this.info = info;
        }

        @Override
        public void run() {
            if (isOpenOneVsMore != false) {
                if (face == null) {
                    face = new FaceFeature();
                }
                int ret = MyApplication.mFaceLibCore.FaceFeatureExtract(info.getDes(), 480, 640, info.getFace(), face);
                if (ret == 0) {
                    float fenshu = SPUtil.getFloat(Const.KEY_ONEVSMORESCORE, Const.ONEVSMORE_SCORE);
                    if (score == null) {
                        score = new FaceSimilar();
                    }
                    if (MyApplication.mList.size() > 0) {
                        for (Map.Entry<String, byte[]> entry : MyApplication.mList.entrySet()) {
                            if ((isOpenOneVsMore == false) || (Const.BATCH_IMPORT_TEMPLATE == true) || (Const.DELETETEMPLATE == true)) {
                                continue;
                            }
                            String fileName = (String) entry.getKey();
                            byte[] mTemplate = (byte[]) entry.getValue();
                            FaceFeature face3 = new FaceFeature(mTemplate);
                            MyApplication.mFaceLibCore.FacePairMatching(face3, face, score);
                            if (score.getScore() >= fenshu) {
                                if (user == null) {
                                    user = new User();
                                }
                                if (MyApplication.faceProvider.quaryUserTableRowCount("select count(id) from tUser") != 0) {
                                    if ((MyApplication.faceProvider.getUserByUserpath(fileName)) != null) {
                                        user.setId(MyApplication.faceProvider.getUserByUserpath(fileName).getId());
                                        AppData.getAppData().setUser(user);
                                    } else {

                                    }
                                }
                                byte[] bitmap_byte = info.getDes();
                                //抓拍动态图片保存
                                AppData.getAppData().SetNFaceBmp(CameraHelp.getYFaceImgByInfraredJpg(info.getFace().getRect().left, info.getFace().getRect().top, info.getFace().getRect().right, info.getFace().getRect().bottom, CameraHelp.getBitMap(bitmap_byte)));
                                fenshu = score.getScore();
                                continue;
                            }
                        }
                        AppData.getAppData().setCompareScore(fenshu);
                    }
                } else {
                    AppData.getAppData().setCompareScore(0);
                }
                if (isOpenOneVsMore != false) {
                    // Log.i("Gavin", "发送消息:");
                    Message msg = new Message();
                    msg.what = Const.COMPER_END;
                    mHandler.sendMessage(msg);
                }

                if (MyApplication.mList.size() < 1000) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    oneVsMoreThreadStauts = false;
                } else {
                    oneVsMoreThreadStauts = false;
                }
            }
        }
    }

    /**
     * 红外线程
     */
    private class MyRedThread extends Thread {
        public boolean redflag = false;
        private TimeCompareUtil timecompare;

        @Override
        public void run() {
            super.run();
            while (true) {
                //G69A
                //int status = GPIOHelper.readStatus();
                //G701  G702
                //int status = PosUtil.getPriximitySensorStatus();
                int status = 1;
                if (redflag == true) {
                    try {
                        Thread.sleep(1500);
                        if (status == 1) {
                            mCameraSurfView.openCamera();
                            Message msg4 = new Message();
                            msg4.what = Const.TEST_INFRA_RED;
                            msg4.obj = 1;
                            mHandler.sendMessage(msg4);
                            logshowflag = 0;
                        }
                        if (status == 0) {
                            logshowflag++;
                            if (logshowflag == ((SPUtil.getInt(Const.KEY_BACKHOME, Const.CLOSE_HOME_TIMEOUT)) / 1.5)) {
                                logshowflag = 0;
                                timecompare = new TimeCompareUtil();
                                if (timecompare.TimeCompare(SPUtil.getString(Const.STARTIME, Const.startime), SPUtil.getString(Const.ENDTIME, Const.endtime), timecompare.getSystemTime())) {
                                    Message msg4 = new Message();
                                    msg4.what = Const.FLAG_SHOW_LOG;
                                    msg4.obj = 2;
                                    mHandler.sendMessage(msg4);
                                } else {
                                    logshowflag = 0;
                                }
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        }

        public void closeredThread() {
            this.redflag = false;
        }

        public void startredThread() {
            this.redflag = true;
        }
    }

    /**
     * 更新UI标志线程
     */
    private class UIThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(250);
                    Message msg = new Message();
                    msg.what = Const.UPDATE_UI;
                    mHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * udp服务端
     */
    private class UDPServerThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(1000);
                    startUdpServer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 开启HTTP服务时显示IP
     *
     * @param ip
     */
    public void httpStrat(String ip) {
        if (!TextUtils.isEmpty(ip)) {
            showHttpUrl.setText(ip + ":8088");
        } else {
            showHttpUrl.setText("");
        }
    }

    /**
     * HTTP服务开启异常时
     *
     * @param msg
     */
    public void httpError(String msg) {
        showHttpUrl.setText(CameraHelp.getIpAddress() + ":8088");
    }

    /**
     * 关闭服务
     */
    public void httpStop() {
        showHttpUrl.setText("The HTTP Server is stopped");
    }


    /**
     * 注册网络监听广播
     */
    private void openNetStatusReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new NetWorkStateReceiver();
        receiver.setmINetStatusListener(this);
        registerReceiver(receiver, filter);
    }

    /**
     * 网络状态改变  接口回调的数据     *
     *
     * @param state
     */
    @Override
    public void getNetState(int state) {
        if (state == 0) {
            System.out.println("conn");
            if (CameraHelp.getIpAddress().equals("")) {
                DeviceSetFrament.updateSetting("192.168.1.2", mContext);
            }
            openSocket();
            openHttpServer();
        } else {
            System.out.println("dis conn");
            closeSocket();
            closeHttpServer();
            showHttpUrl.setText("");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            default:
                break;
        }
    }

    /**
     * 打开socket连接
     */
    private void openSocket() {
        boolean conn = ConversionHelp.isNetworkConnected(mContext);
        receiver.setIs_conn(conn);
        if (!conn) {
            Toast.makeText(mContext, "没有网络,不开启socket连接", Toast.LENGTH_SHORT).show();
            com.runvision.core.LogToFile.i("MainActivity", "没有网络,不开启socket连接");
            return;
        }
        openHttpServer();

        if (!SPUtil.getString(Const.KEY_VMSIP, "").equals("") && SPUtil.getInt(Const.KEY_VMSPROT, 0) != 0 && !SPUtil.getString(Const.KEY_VMSUSERNAME, "").equals("") && !SPUtil.getString(Const.KEY_VMSPASSWORD, "").equals("")) {
            //开启socket线程
            socketReconnect(SPUtil.getString(Const.KEY_VMSIP, ""), SPUtil.getInt(Const.KEY_VMSPROT, 0));
        }
    }


    /**
     * socket重连接
     *
     * @param ip
     * @param port
     */
    private void socketReconnect(String ip, int port) {
        if (socketThread == null) {
            socketThread = new SocketThread(ip, port, mHandler);
        } else {
            socketThread.close();
            if (heartBeatThread != null) {
                heartBeatThread.HeartBeatThread_flag = false;
                heartBeatThread = null;
            }
            socketThread = new SocketThread(ip, port, mHandler);
        }
        socketThread.start();
    }

    /**
     * 结束socket
     *
     * @param
     */
    public void closeSocket() {
        if (heartBeatThread != null) {
            //取消心跳
            heartBeatThread.HeartBeatThread_flag = false;
            heartBeatThread = null;
        }
        //结束socket
        if (socketThread != null) {
            socketThread.close();
            socketThread = null;
        }

    }

    //上传的所有数据长度大小
    private int mSum = 0;
    //切割后的数据
    private List<File> dataList1 = null;
    private List<File> dataList2 = null;
    private List<File> dataList3 = null;
    //三个线程消息传递对应的标志为
    private int[] loadFlag = {100, 101, 102, 103};
    private int bacthOk0, bacthOk1, bacthOk2, bacthOk3 = 0;
    private int parts = 0;

    private List<File> getImagePathFile() {
        String strPath = Environment.getExternalStorageDirectory() + "/SocketImage/";
        File file = new File(strPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File[] mListFile = file.listFiles();
        if (mListFile.length == 0) {
            //Toast.makeText(mContext, "SocketImage文件夹下面没有图片文件", Toast.LENGTH_SHORT).show();
            return null;
        }
        List<File> mImportFile = new ArrayList<>();
        for (File file1 : mListFile) {
            if (checkIsImageFile(file1.getName())) {
                mImportFile.add(file1);
            }
        }
        //得到图片文件
        if ((mSum = mImportFile.size()) == 0) {
            //Toast.makeText(mContext, "image文件夹下面没有图片文件", Toast.LENGTH_SHORT).show();
            return null;
        }
        return mImportFile;
    }

    /**
     * 批量导入模板
     */
    private void batchImport() {
        List<File> mImportFile = getImagePathFile();
        if (mImportFile == null) {
            return;
        }
        bacthOk0 = 0;
        bacthOk1 = 0;
        bacthOk2 = 0;
        bacthOk3 = 0;
        Const.VMS_BATCH_IMPORT_TEMPLATE = true;
//        Log.e("lichaoo", "batchImport1:" + bacthOk0 + "=" + mSum);
//        Log.e("lichaoo", "batchImport3:" + bacthOk1 + bacthOk2 + bacthOk3  + "=" + mSum);
        System.out.println("一共：" + mSum);
        //将文件数据分成三个集合
        cuttingList(mImportFile);
        if (parts == 1) {
            Log.i("lichaoo", "batchImport one");
            BatchImport impory = new BatchImport(socketThread,dataList1, mHandler, loadFlag[0]);
            Thread thread = new Thread(impory);
            thread.start();
        } else if (parts == 3) {
            Log.i("lichaoo", "batchImport three");
            BatchImport impory1 = new BatchImport(socketThread,dataList1, mHandler, loadFlag[1]);
            Thread thread1 = new Thread(impory1);
            thread1.start();

            BatchImport impory2 = new BatchImport(socketThread,dataList2, mHandler, loadFlag[2]);
            Thread thread2 = new Thread(impory2);
            thread2.start();

            BatchImport impory3 = new BatchImport(socketThread,dataList3, mHandler, loadFlag[3]);
            Thread thread3 = new Thread(impory3);
            thread3.start();
        }
    }


    /**
     * 检查扩展名，得到图片格式的文件
     *
     * @param fName 文件名
     * @return
     */
    private boolean checkIsImageFile(String fName) {
        boolean isImageFile = false;
        // 获取扩展名
        String FileEnd = fName.substring(fName.lastIndexOf(".") + 1,
                fName.length()).toLowerCase();
        if (FileEnd.equals("jpg") || FileEnd.equals("png") || FileEnd.equals("gif")
                || FileEnd.equals("jpeg") || FileEnd.equals("bmp")) {
            isImageFile = true;
        } else {
            isImageFile = false;
        }
        return isImageFile;
    }

    private void cuttingList(List<File> list) {
        //我们数据之分三批
        int part = 3;
        int dataList = list.size();
        int minBatchImprot = 10;
        int pointsDataLimit = dataList % part == 0 ? dataList / part : (dataList / part) + 1;
        if (dataList > minBatchImprot) {
            parts = 3;
            System.out.println("开启三个线程");
            dataList1 = list.subList(0, pointsDataLimit);
            dataList2 = list.subList(pointsDataLimit, pointsDataLimit * 2);
            if (!list.isEmpty()) {
                dataList3 = list.subList(pointsDataLimit * 2, list.size());
            }
        } else {
            parts = 1;
            //只开启一个线程
            System.out.println("只开启一个线程");
            dataList1 = list;
        }
    }

    /**
     * 打开HTTP服务器
     */
    public void openHttpServer() {
        //开启HTTP服务
        if (serverManager != null) {
            closeHttpServer();
        }
        serverManager = new ServerManager(this);
        serverManager.register();
        serverManager.startService();
    }

    public void closeHttpServer() {
        if (serverManager != null) {
            serverManager.unRegister();
            serverManager.stopService();
            serverManager = null;
        }
    }


    public void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    public void onBackPressed() {
        cancelToast();
        super.onBackPressed();
    }


    public void rebootSU() {
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        OutputStreamWriter osw = null;
        StringBuilder sbstdOut = new StringBuilder();
        StringBuilder sbstdErr = new StringBuilder();

        String command = "/system/bin/reboot";

        try { // Run Script
            proc = runtime.exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command);
            osw.flush();
            osw.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (proc != null)
                proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sbstdOut.append(new BufferedReader(new InputStreamReader(proc.getInputStream())));
        sbstdErr.append(new BufferedReader(new InputStreamReader(proc.getErrorStream())));
        if (proc.exitValue() != 0) {

        }
    }

    //获取当前系统时间
    private Date currentTime = null;//currentTime就是系统当前时间
    //定义时间的格式
    private DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Date strbeginDate = null;//起始时间
    private Date strendDate = null;//结束时间
    private boolean range = false;

    public Boolean TimeCompare(String strbeginTime, String strendTime, String currentTime1) {
        try {
            strbeginDate = fmt.parse(strbeginTime);//将时间转化成相同格式的Date类型
            strendDate = fmt.parse(strendTime);
            currentTime = fmt.parse(currentTime1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if ((currentTime.getTime() - strbeginDate.getTime()) > 0 && (strendDate.getTime() - currentTime.getTime()) > 0) {//使用.getTime方法把时间转化成毫秒数,然后进行比较
            range = true;
            //  ToastUtil.MyToast(UnlockActivity.this, "当前时间在范围内");
        } else {
            range = false;
            //  ToastUtil.MyToast(UnlockActivity.this, "您的操作时间已到期,请重新申请操作时间");
        }
        return range;
    }

    public void startUdpServer() {
        new Thread() {
            @Override
            public void run() {
                try {
                    StringBuffer sb = new StringBuffer();
                    DatagramSocket ds = new DatagramSocket(5555);
                    byte[] buf = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(buf, 1024);
                    ds.receive(dp);
                    sb.append(ConversionHelp.bytesToHexString(dp.getData()));
                    InetAddress addr = dp.getAddress();
                    int port = dp.getPort();

                    byte[] echo = SendData.UDPDeviceMsg();

                    DatagramPacket dp2 = new DatagramPacket(echo, echo.length, addr, port);
                    ds.send(dp2);

                    ds.close();
                } catch (Exception e) {
                    Log.i("miao", "###############################################" + "Exception");
                }
            }
        }.start();
    }


    //删除访客记录
    private void DeleteVisitorRecord() {
        if (MyApplication.faceProvider.quaryUserTableRowCount("select count(id) from tUser") != 0) {
            mList = MyApplication.faceProvider.getAllUser();
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i).getType().equals("访客")) {
                    List<User> mList1 = MyApplication.faceProvider.queryUser("select * from tUser where id=" + (mList.get(i).getId()));
                    FileUtils.deleteTempter(mList1.get(0).getTemplateImageID(), Const.TEMP_DIR);
                    FileUtils.deleteFaceTempter(mList1.get(0).getTemplateImageID(), "Template");
                    MyApplication.mList.remove(mList1.get(0).getTemplateImageID());
                    MyApplication.faceProvider.deleteUserById(mList.get(i).getId());
                    //  FileUtils.deleteTempter(mList1.get(0).getRecord().getSnapImageID());
                    MyApplication.faceProvider.deleteUser(mList.get(i).getId());
                }
            }
        }
    }

    private void showConfirmPsdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        final View view = View.inflate(this, R.layout.dialog_confirm_psd, null);
        VerificationCodeInput input = view.findViewById(R.id.verificationCodeInput);
        String psd = Const.MOBILE_SAFE_PSD;
        input.setOnCompleteListener(content -> {
            if (SPUtil.getString(Const.KEY_SETTING_PASSWORD, "").equals("") && psd.equals(content)) {
                Intent intent = new Intent(mContext, RegisterActivity.class);
                startActivity(intent);
                dialog.dismiss();
            } else if (SPUtil.getString(Const.KEY_SETTING_PASSWORD, "").equals(content)) {
                Intent intent = new Intent(mContext, RegisterActivity.class);
                startActivity(intent);
                dialog.dismiss();
            } else {
                showToast("输入密码错误");
            }
        });
        dialog.setView(view);
        dialog.show();
    }

    /******************G701/G702身份证读卡******************/
    private UsbManager mUsbManager;
    private UsbDevice idcard_reader;
    private boolean hasReader = false;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private IdentityInfo info;
    private Bitmap cardBitmap;
    private byte[] image;
    private boolean hasPermission = false;

    private class GetIDInfoTask extends AsyncTask<Void, Integer, Boolean> {
        public boolean taskIsRuning = true;

        public void setTaskIsRuning(boolean taskIsRuning) {
            this.taskIsRuning = taskIsRuning;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            info = null;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            while (taskIsRuning) {
                try {
                    IdCard.open(IdCard.IDREADER_TYPE_USB, mContext);
                    info = IdCard.checkIdCard(1000);
                } catch (TelpoException e1) {
                    e1.printStackTrace();
                }

                if (info != null) {
                    if("".equals(info.getName())) {
                        return false;
                    }
                    image = info.getHead_photo();
                    if ("I".equals(info.getCard_type())) {
                        if(image.length != 1024) {
                            return false;
                        }
                    }else {
                        if(image.length == 2048 || image.length == 1024) {
                        }else {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                byte[] buf = new byte[WLTService.imgLength];
                if (1 == WLTService.wlt2Bmp(image, buf)) {
                    cardBitmap = IDPhotoHelper.Bgr2Bitmap(buf);
                }
                if (ReaderCardFlag) {
                    isOpenOneVsMore = false;
                    ReaderCardFlag = false;
                    Message msg = new Message();
                    msg.what = Const.READ_CARD_INFO;
                    msg.obj = info;
                    mHandler.sendMessage(msg);
                }
            }
        }
    }
}
