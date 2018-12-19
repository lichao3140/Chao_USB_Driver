package com.runvision.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

@Entity
public class Cours {

    /**
     * coursename : 法律、法规及道路交通信号|机动车基本知识
     * subject : 1
     * coursecode : 01,02
     * classcode : 91620866542145536
     * targetlen : 45
     */

    @Id(autoincrement = true)
    private Long id;

    /**
     * 课程名称
     */
    @Property(nameInDb = "coursename")
    private String coursename;

    /**
     * 科目
     */
    @Property(nameInDb = "subject")
    private String subject;

    /**
     * 课程编码
     */
    @Property(nameInDb = "coursecode")
    private String coursecode;

    /**
     * 课堂编码
     */
    @Property(nameInDb = "classcode")
    private String classcode;

    /**
     * 目标学习时长
     */
    @Property(nameInDb = "targetlen")
    private String targetlen;

    @Generated(hash = 642847660)
    public Cours(Long id, String coursename, String subject, String coursecode,
                 String classcode, String targetlen) {
        this.id = id;
        this.coursename = coursename;
        this.subject = subject;
        this.coursecode = coursecode;
        this.classcode = classcode;
        this.targetlen = targetlen;
    }

    @Generated(hash = 659831132)
    public Cours() {
    }

    public String getCoursename() {
        return coursename;
    }

    public void setCoursename(String coursename) {
        this.coursename = coursename;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCoursecode() {
        return coursecode;
    }

    public void setCoursecode(String coursecode) {
        this.coursecode = coursecode;
    }

    public String getClasscode() {
        return classcode;
    }

    public void setClasscode(String classcode) {
        this.classcode = classcode;
    }

    public String getTargetlen() {
        return targetlen;
    }

    public void setTargetlen(String targetlen) {
        this.targetlen = targetlen;
    }

    @Override
    public String toString() {
        return "Cours{" +
                "coursename='" + coursename + '\'' +
                ", subject='" + subject + '\'' +
                ", coursecode='" + coursecode + '\'' +
                ", classcode='" + classcode + '\'' +
                ", targetlen='" + targetlen + '\'' +
                '}';
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
