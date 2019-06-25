package com.blockchain.dto;

import javax.validation.constraints.NotNull;

/**
 * @author shurenwei
 */
public class BaseRequest {
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

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getEnrollmentID() {
        return enrollmentID;
    }

    public void setEnrollmentID(String enrollmentID) {
        this.enrollmentID = enrollmentID;
    }
}
