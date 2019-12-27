package com.runvision.bean;

/**
 * 考勤终端注册
 */
public class Device {

    private String usercode;//用户编号
    private String hmaccode;//数字签名
    private String inscode;//培训机构编号
    private int termtype;//计时终端类型
    private String vender;//生产厂家
    private String model;//终端型号
    private String gps;//GPS坐标
    private String imei;//imei地址

    public String getUsercode() {
        return usercode;
    }

    public void setUsercode(String usercode) {
        this.usercode = usercode;
    }

    public String getHmaccode() {
        return hmaccode;
    }

    public void setHmaccode(String hmaccode) {
        this.hmaccode = hmaccode;
    }

    public String getInscode() {
        return inscode;
    }

    public void setInscode(String inscode) {
        this.inscode = inscode;
    }

    public int getTermtype() {
        return termtype;
    }

    public void setTermtype(int termtype) {
        this.termtype = termtype;
    }

    public String getVender() {
        return vender;
    }

    public void setVender(String vender) {
        this.vender = vender;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public Device(String usercode, String hmaccode, String inscode, int termtype, String vender, String model, String gps, String imei) {
        this.usercode = usercode;
        this.hmaccode = hmaccode;
        this.inscode = inscode;
        this.termtype = termtype;
        this.vender = vender;
        this.model = model;
        this.gps = gps;
        this.imei = imei;
    }

    public Device(String inscode, int termtype, String vender, String model, String gps, String imei) {
        this.inscode = inscode;
        this.termtype = termtype;
        this.vender = vender;
        this.model = model;
        this.gps = gps;
        this.imei = imei;
    }

    @Override
    public String toString() {
        return "Device{" +
                "usercode='" + usercode + '\'' +
                ", hmaccode='" + hmaccode + '\'' +
                ", inscode='" + inscode + '\'' +
                ", termtype=" + termtype +
                ", vender='" + vender + '\'' +
                ", model='" + model + '\'' +
                ", gps='" + gps + '\'' +
                ", imei='" + imei + '\'' +
                '}';
    }
}
