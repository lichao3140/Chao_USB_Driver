package com.runvision.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

/**
 *
 * Bean 对象注释的解释
 *
 * @Entity：告诉GreenDao该对象为实体，只有被@Entity注释的Bean类才能被dao类操作
 * @Id：对象的Id，使用Long类型作为EntityId，否则会报错。(autoincrement = true)表示主键会自增，如果false就会使用旧值
 * @Property：可以自定义字段名，注意外键不能使用该属性
 * @NotNull：属性不能为空
 * @Transient：使用该注释的属性不会被存入数据库的字段中
 * @Unique：该属性值必须在数据库中是唯一值
 * @Generated：编译后自动生成的构造函数、方法等的注释，提示构造函数、方法等不能被修改
 */

@Entity
public class IDCard {
    //不能用int （ID 表示标识主键 且主键不能用int autoincrement = true 表示主键会自增）
    @Id(autoincrement = true)
    private Long id;

    //姓名
    @Property(nameInDb = "name")
    private String name;

    //性别
    @Property(nameInDb = "gender")
    private String gender;

    //身份证号码
    @Property(nameInDb = "id_card")
    private String id_card;

    //人证对比图片
    @Property(nameInDb = "facepic")
    private String facepic;

    //身份证图片
    @Property(nameInDb = "idcardpic")
    private String idcardpic;

    //打卡时间
    @Property(nameInDb = "sign_in")
    private String sign_in;

    //打卡SN
    @Property(nameInDb = "sn")
    private String sn;

    @Generated(hash = 782242639)
    public IDCard(Long id, String name, String gender, String id_card,
                  String facepic, String idcardpic, String sign_in, String sn) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.id_card = id_card;
        this.facepic = facepic;
        this.idcardpic = idcardpic;
        this.sign_in = sign_in;
        this.sn = sn;
    }

    @Generated(hash = 1276747893)
    public IDCard() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getId_card() {
        return id_card;
    }

    public void setId_card(String id_card) {
        this.id_card = id_card;
    }

    public String getFacepic() {
        return facepic;
    }

    public void setFacepic(String facepic) {
        this.facepic = facepic;
    }

    public String getIdcardpic() {
        return idcardpic;
    }

    public void setIdcardpic(String idcardpic) {
        this.idcardpic = idcardpic;
    }

    public String getSign_in() {
        return sign_in;
    }

    public void setSign_in(String sign_in) {
        this.sign_in = sign_in;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }
}
