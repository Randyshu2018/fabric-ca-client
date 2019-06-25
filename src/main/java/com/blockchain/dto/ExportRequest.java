package com.blockchain.dto;

import javax.validation.constraints.NotNull;


/**
 * @author shurenwei
 */
public class ExportRequest {
    /**
     * 登记ID
     */
    @NotNull
    private String enrollmentID;
    /**
     * 组织名称
     */
    @NotNull
    private String orgName;
    /**
     * 用于颁发证书的签名配置文件的名称
     */
    private String profile;

    public String getEnrollmentID() {
        return enrollmentID;
    }

    public void setEnrollmentID(String enrollmentID) {
        this.enrollmentID = enrollmentID;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
