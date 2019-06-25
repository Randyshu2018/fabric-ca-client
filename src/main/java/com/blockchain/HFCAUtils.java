package com.blockchain;

import com.blockchain.dto.BaseRequest;
import com.blockchain.dto.EnrollmentRequest;
import com.blockchain.exception.MyException;
import com.blockchain.model.HyperUser;
import com.blockchain.model.Result;
import com.blockchain.utils.FabricHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shurenwei
 */
public class HFCAUtils {
    private static final Logger logger = LoggerFactory.getLogger(HFCAUtils.class);

    public static Result enroll(EnrollmentRequest enrollmentRequest){
        try {
            boolean flag = FabricHelper.enroll(enrollmentRequest);
            if(flag){
                return Result.getSuccInstance(null);
            }
            return Result.getErrorInstance(Result.FAIL);
        } catch (MyException e) {
            logger.error("HFCAUtils | enroll ", e.getMessage());
            return Result.getErrorInstance(e.getMessage());
        }
    }

    public static Result userIsExist(BaseRequest request){
        try {
            boolean flag = FabricHelper.getMember(request.getEnrollmentID(),request.getOrgName(),null).isEnrolled();
            if(flag){
                return Result.getSuccInstance(null);
            }
            return Result.getErrorInstance(Result.FAIL);
        } catch (MyException e) {
            logger.error("HFCAUtils | userIsExist ", e.getMessage());
            return Result.getErrorInstance(e.getMessage());
        }
    }

    public static void main(String []args){
        EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
        enrollmentRequest.setOrgName("Org1");
        enrollmentRequest.setEnrollmentID("randy7");
        enrollmentRequest.setEnrollmentSecret("123456");
        BaseRequest request = new BaseRequest();
        request.setEnrollmentID("randy10");
        request.setOrgName("Org1");
//        Result r = enroll(enrollmentRequest);
//        System.out.println(r.getMsg());

        try {
            HyperUser user = FabricHelper.getMemberFromMSP(enrollmentRequest.getEnrollmentID(),enrollmentRequest.getOrgName(),enrollmentRequest.getProfile());
            System.out.println(user.isEnrolled());
            System.out.println(user);
            System.out.println(userIsExist(request));

        } catch (MyException e) {
            e.printStackTrace();
        }
    }
}
