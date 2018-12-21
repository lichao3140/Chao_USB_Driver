package com.runvision.bean;

import java.util.List;

public class LogOutResponse {

    /**
     * data : {"completeData":[{"completeHour":1,"subject":"1","targetHour ":10},{"completeHour":1,"subject":"4","targetHour ":10}],"devnum":"103662535869337600","id":"123","sn":"ffffffff-d384-095d-0033-c5870033c5871541123252286","stucode":"431023199003145114"}
     * errorcode : 0
     * message : 操作成功
     */
    private DataBean data;
    private String errorcode;
    private String message;

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public String getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(String errorcode) {
        this.errorcode = errorcode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class DataBean {
        /**
         * completeData : [{"completeHour":1,"subject":"1","targetHour ":10},{"completeHour":1,"subject":"4","targetHour ":10}]
         * devnum : 103662535869337600
         * id : 123
         * sn : ffffffff-d384-095d-0033-c5870033c5871541123252286
         * stucode : 431023199003145114
         */

        private String devnum;
        private String id;
        private String sn;
        private String stucode;
        private List<CompleteDataBean> completeData;

        public String getDevnum() {
            return devnum;
        }

        public void setDevnum(String devnum) {
            this.devnum = devnum;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSn() {
            return sn;
        }

        public void setSn(String sn) {
            this.sn = sn;
        }

        public String getStucode() {
            return stucode;
        }

        public void setStucode(String stucode) {
            this.stucode = stucode;
        }

        public List<CompleteDataBean> getCompleteData() {
            return completeData;
        }

        public void setCompleteData(List<CompleteDataBean> completeData) {
            this.completeData = completeData;
        }

        public static class CompleteDataBean {
            /**
             * completeHour : 1
             * subject : 1
             * targetHour  : 10
             */

            private int completeHour;
            private String subject;
            private int targetHour;

            public int getCompleteHour() {
                return completeHour;
            }

            public void setCompleteHour(int completeHour) {
                this.completeHour = completeHour;
            }

            public String getSubject() {
                return subject;
            }

            public void setSubject(String subject) {
                this.subject = subject;
            }

            public int getTargetHour() {
                return targetHour;
            }

            public void setTargetHour(int targetHour) {
                this.targetHour = targetHour;
            }
        }
    }
}
