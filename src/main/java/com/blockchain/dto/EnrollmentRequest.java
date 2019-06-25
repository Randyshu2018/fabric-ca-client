package com.blockchain.dto;

import javax.validation.constraints.NotNull;

/**
 * @author shurenwei
 */
public class EnrollmentRequest {

    /**
     * 组织名称
     */
    @NotNull(message = "orgName can not be null")
    private String orgName;
    /**
     * 登记ID
     */
    @NotNull(message = "enrollmentID can not be null")
    private String enrollmentID;
    /**
     * 登记Secret
     */
    @NotNull(message = "enrollmentSecret can not be null")
    private String enrollmentSecret;
    /**
     * 证书签名请求
     */
    private String csr;
    /**
     * 用于颁发证书的签名配置文件的名称
     */
    private String profile = null;

    public String getEnrollmentID() {
        return enrollmentID;
    }

    public void setEnrollmentID(String enrollmentID) {
        this.enrollmentID = enrollmentID;
    }

    public String getEnrollmentSecret() {
        return enrollmentSecret;
    }

    public void setEnrollmentSecret(String enrollmentSecret) {
        this.enrollmentSecret = enrollmentSecret;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
