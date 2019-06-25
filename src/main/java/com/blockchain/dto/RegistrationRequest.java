package com.blockchain.dto;

import javax.validation.constraints.NotNull;

/**
 * @author shurenwei
 */
public class RegistrationRequest {

    /**
     * 默认证书类型
     */
    private static String DEFAULT_USER_TYPE = "user";
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
    private String enrollmentSecret;
    /**
     * 组织从属机构
     */
    @NotNull(message = "affiliation can not be null")
    private String affiliation;

    private String type = DEFAULT_USER_TYPE;

    private String profile;

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

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
