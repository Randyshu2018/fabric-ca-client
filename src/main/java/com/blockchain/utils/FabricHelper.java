package com.blockchain.utils;

import com.blockchain.exception.MyException;
import com.blockchain.model.Config;
import com.blockchain.model.HyperUser;
import com.blockchain.model.Store;
import com.google.common.primitives.Bytes;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.*;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.HFCACertificateException;
import org.hyperledger.fabric_ca.sdk.exception.InfoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author shurenwei
 */
public class FabricHelper {
    private static final Logger logger = LoggerFactory.getLogger(FabricHelper.class);

    private static String PATH = System.getProperty("user.dir");

    private static String FIXTURES_PATH=PATH+"/src/main/java";

    private static Store SAMPLE_STORE ;

    private static NetworkConfig NETWORK_CONFIG;
    /**
     * 证书存放路径
     */
    private static File ARTIFACTS_STORE_PATH;
    static {
        try {
            String path = Config.getConfig().getProperty("artifacts.store.path");
            String enrollPathStr = Config.getConfig().getProperty("enrollment.store.path");
            File enrollPath = new File(enrollPathStr);
            if(!enrollPath.exists()){
                enrollPath.createNewFile();
            }
            SAMPLE_STORE = new Store(enrollPath);
            if(path==null){
                ARTIFACTS_STORE_PATH = new File(FIXTURES_PATH + "/users");
            }else{
                ARTIFACTS_STORE_PATH = new File(path);
            }
        } catch (Exception e) {
            // if not there no worries just use defaults
            logger.error("Failed to load any configuration",e.getMessage());
        }
    }

    /**
     * If authority key id is not present or if it is present and equal to subject key id,
     * then it is a root certificate
     * @param certificate certificate info
     * @return is rootCA or not
     */
    private static boolean isRootCA(X509Certificate certificate){
        byte [] authorityKeyIdentifier = certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
        byte [] subjectKeyIdentifier = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        return authorityKeyIdentifier == null || Arrays.equals(authorityKeyIdentifier,subjectKeyIdentifier);
    }

    /**
     * Store the CAChain in the CACerts folder of MSP (Membership Service Provider)
     * The root cert in the chain goes into MSP 'cacerts' directory.
     * The others (if any) go into the MSP 'intermediatecerts' directory.
     * @param mspDirStr cert store directoryy
     * @param hfcaInfo caInfo
     * @throws MyException
     */
    private static void storeCaChain(String mspDirStr, HFCAInfo hfcaInfo) throws MyException {
        Base64.Decoder b64dec = Base64.getDecoder();
        File mspDir;
        File caCertFile;
        File intermediateCaCertFile;
        CertificateFactory cf;
        List<X509Certificate> certificates;
        try {
            mspDir = createFile(mspDirStr);
            caCertFile = createFile(mspDir+"/cacerts/cert.pem");
            intermediateCaCertFile = createFile(mspDir+"/intermediatecerts/cert.pem");
            cf = CertificateFactory.getInstance("X.509");
            certificates = (List<X509Certificate>) cf.generateCertificates(new ByteArrayInputStream(b64dec.decode(hfcaInfo.getCACertificateChain().getBytes(UTF_8))));
            for (X509Certificate t : certificates) {
                File tmp;
                if (isRootCA(t)){
                    tmp = caCertFile;
                }else{
                    tmp = intermediateCaCertFile;
                }
                try (PemWriter writer = new PemWriter(new FileWriter(tmp))){
                    writer.writeObject(new PemObject("CERTIFICATE", t.getEncoded()));
                }
            }
        } catch (IOException e) {
            logger.error("FabricHelper | storeCaChain ",e.getMessage());
            throw new MyException("创建文件异常");
        } catch (CertificateException e) {
            logger.error("FabricHelper | storeCaChain ",e.getMessage());
            throw new MyException("生成证书异常");
        } catch (Exception e) {
            logger.error("FabricHelper | storeCaChain ",e.getMessage());
            throw new MyException("写入文件异常");
        }

    }

    /**
     * Store the admincerts in the admincerts folder of MSP (Membership Service Provider)
     * The admincerts goes into MSP 'admincerts' directory.
     * @param mspDirStr cert store directory
     * @param certificateResponse rr
     * @throws MyException
     */
    private static void storeAdminCerts(String mspDirStr,HFCACertificateResponse certificateResponse) throws MyException {
        File mspDir;
        try {
            mspDir = createFile(mspDirStr);
            if(certificateResponse != null){
                File adminCertsFile = createFile(mspDir+"/admincerts/cert.pem");
                byte[] adminCertsByte = new byte[0];
                for (HFCACredential c : certificateResponse.getCerts()) {
                    adminCertsByte = Bytes.concat(adminCertsByte,((HFCAX509Certificate)c).getPEM().getBytes());
                }
                writeFile(adminCertsFile,adminCertsByte);
            }
        } catch (IOException e) {
            logger.error("FabricHelper | storeAdminCerts ",e.getMessage());
            throw new MyException("创建文件异常");
        }
    }

    /**
     * 根据用户证书和CA链信息构造MSP结构
     * @param user 用户信息
     * @param hfcaInfo 证书颁发机构信息
     * @param certificateResponse 证书信息(管理员)
     * @throws MyException
     */
    public static void constructMSP(HyperUser user, HFCAInfo hfcaInfo, HFCACertificateResponse certificateResponse) throws MyException {
        File mspDir  ;
        File keyFile  ;
        File signCertsFile ;
        try {
            mspDir = createFile(ARTIFACTS_STORE_PATH + "/" + user.getName()+"/msp");
            keyFile = createFile(mspDir + "/keystore/user_ls");
            signCertsFile = createFile(mspDir + "/signcerts/cert.pem");
        } catch (IOException e) {
            logger.error("FabricHelper | constructMSP ",e.getMessage());
            throw new MyException("创建文件异常");
        }

        try (PemWriter writer = new PemWriter(new FileWriter(keyFile))) {
            writer.writeObject(new PemObject("PRIVATE KEY", user.getEnrollment().getKey().getEncoded()));
        } catch (IOException e) {
            throw new MyException("写入文件异常");
        }
        writeFile(signCertsFile, user.getEnrollment().getCert().getBytes());
        storeCaChain(mspDir.getAbsolutePath(),hfcaInfo);
        storeAdminCerts(mspDir.getAbsolutePath(),certificateResponse);
    }

    /**
     * 根据用户证书和CA链信息构造TLS结构
     * @param user 用户信息
     * @param hfcaInfo 证书颁发机构信息
     * @throws IOException
     */
    public static void constructTLS(HyperUser user,HFCAInfo hfcaInfo) throws IOException {
        File tlsDir = createFile(ARTIFACTS_STORE_PATH + "/" + user.getName()+"/tls");
        File keyFile = createFile(tlsDir + "/server.key");
        File signCertsFile = createFile(tlsDir + "/server.crt");
        File caFile = createFile(tlsDir + "/ca.crt");

        try (PemWriter writer = new PemWriter(new FileWriter(keyFile))) {
            writer.writeObject(new PemObject("PRIVATE KEY", user.getEnrollment().getKey().getEncoded()));
        }

        writeFile(signCertsFile,user.getEnrollment().getCert().getBytes());
        writeFile(caFile,hfcaInfo.getCACertificateChain().getBytes());

    }

    /**
     * 写入数据到指定文件
     * @param file 文件名
     * @param bytes 字节数组
     */
    private static void writeFile(File file,byte[] bytes) {
        try {
            OutputStream out = new FileOutputStream(file);
            out.write(bytes);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建文件
     * @param fpath 文件路径
     * @return
     * @throws IOException
     */
    private static File createFile(String fpath) throws IOException {
        File f = new File(fpath);
        if(!f.getParentFile().exists()){
            f.getParentFile().mkdirs();
        }
        if(f.isFile() && !f.exists()){
            f.createNewFile();
        }
        return f;
    }
    private static NetworkConfig getNetworkConfig() throws MyException{
        try {
            if(NETWORK_CONFIG != null){
                return NETWORK_CONFIG;
            }else{
//                NETWORK_CONFIG = NetworkConfig.fromYamlFile(new File("src/main/resources/network-config.yaml"));
//                InputStream input = getClass().getResourceAsStream()
                InputStream inputStream = NetworkConfig.class.getResourceAsStream("/network-config.yaml");
                NETWORK_CONFIG =  NetworkConfig.fromYamlStream(inputStream);
            }
        } catch (InvalidArgumentException e) {
            throw new MyException("加载yaml文件参数错误");
        } catch (NetworkConfigurationException e) {
            e.printStackTrace();
            throw new MyException("网络配置错误");
        }
        return NETWORK_CONFIG;
    }

    /**
     * 获取CA客户端实例
     * @param orgName 组织名称
     * @param profile 用于颁发证书的签名配置文件的名称
     * @param skipCA 此处是为了避免获取用户证书时因为CA导致的bug
     * @return 客户端实例
     * @throws MyException
     */
    public static HFCAClient getHFCAClient(String orgName,String profile,boolean skipCA) throws MyException{
        NetworkConfig networkConfig = getNetworkConfig();

        NetworkConfig.OrgInfo orgInfo = networkConfig.getOrganizationInfo(orgName);
        if(orgInfo == null){
            throw new MyException("Organization Info not found!");
        }
        NetworkConfig.CAInfo caInfo;
        if(profile != null){
            caInfo = orgInfo.getCertificateAuthorities().get(1);
        }else{
            caInfo = orgInfo.getCertificateAuthorities().get(0);
        }
        HFCAClient hfcaClient ;
        try {
            if(skipCA) {
                hfcaClient = HFCAClient.createNewInstance(caInfo.getUrl(), caInfo.getProperties());
            }else{
                hfcaClient = HFCAClient.createNewInstance(caInfo);
            }
            hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("FabricHelper | getHFCAClient ",e.getMessage());
            throw new MyException("获取CA客户端异常");
        }
        return hfcaClient;
    }

    /**
     * 获取CA Registrar列表
     * @param orgName 组织名称
     * @param profile 用于颁发证书的签名配置文件的名称
     * @return 初始用户列表
     * @throws MyException
     */
    public static Collection<NetworkConfig.UserInfo> getRegistrar(String orgName, String profile) throws MyException{
        NetworkConfig.OrgInfo orgInfo = getOrgInfo(orgName);
        if(orgInfo == null){
            throw new MyException("Organization Info not found!");
        }
        NetworkConfig.CAInfo caInfo;
        if(profile != null){
            caInfo = orgInfo.getCertificateAuthorities().get(1);
        }else{
            caInfo = orgInfo.getCertificateAuthorities().get(0);
        }
        return caInfo.getRegistrars();
    }

    /**
     * 获取组织信息
     * @param orgName 组织名称
     * @return 组织
     * @throws MyException
     */
    public static NetworkConfig.OrgInfo getOrgInfo(String orgName) throws MyException{
        NetworkConfig networkConfig = getNetworkConfig();

        NetworkConfig.OrgInfo orgInfo = networkConfig.getOrganizationInfo(orgName);
        if(orgInfo == null){
            throw new MyException("Organization Info not found!");
        }
        return orgInfo;
    }

    /**
     * 从sampleStore获取用户
     * @param username 用户名
     * @param orgName  组织名称
     * @param profile 用于颁发证书的签名配置文件的名称
     * @return 用户
     */
    public static HyperUser getMember(String username,String orgName, String profile) throws MyException {
        HFCAClient hfcaClient = getHFCAClient(orgName,profile,false);
        HyperUser user;
        try{
            user = getMemberFromMSP(username,orgName,profile);
        }catch (MyException e){
            user = SAMPLE_STORE.getMember(username,orgName,hfcaClient.getCAName());
        }
        return user;
    }

    /**
     * 根据指定用户证书构造用户
     * @param username 用户名
     * @param orgName  组织名称
     * @param profile 用于颁发证书的签名配置文件的名称
     * @return 用户
     * @throws MyException
     */
    public static HyperUser getMemberFromMSP(String username,String orgName,String profile) throws MyException {
        File fpath = new File(ARTIFACTS_STORE_PATH + "/"+username);
        if(!fpath.exists()){
            throw new MyException("用户"+username+"不存在");
        }
        String signcerts = ARTIFACTS_STORE_PATH+"/"+username+"/msp/signcerts/cert.pem";
        String keystore = ARTIFACTS_STORE_PATH+"/"+username+"/msp/keystore/user_ls";
        HFCAClient hfcaClient = getHFCAClient(orgName,profile,false);
        try {
            return SAMPLE_STORE.getMember(username,orgName,hfcaClient.getCAName(),getOrgInfo(orgName).getMspId(),new File(keystore),new File(signcerts));
        } catch (Exception e) {
            logger.error("FabricHelper | getMemberFromMSP ",e.getMessage());
            throw new MyException("获取用户异常");
        }
    }

    /**
     * 导出用户证书
     * @param username 用户名
     * @param orgName 组织名称
     * @param profile 用于颁发证书的签名配置文件的名称
     * @throws MyException
     */
    public static void exportUser(String username,String orgName,String profile) throws MyException {
        HyperUser user = FabricHelper.getMember(username, orgName,profile);
        HFCAClient hfcaClient = getHFCAClient(orgName,profile,false);

        HFCAInfo hfcaInfo;
        try {
            hfcaInfo = hfcaClient.info();
        } catch (Exception e) {
            logger.error("FabricHelper | exportUser",e.getMessage());
            throw new MyException("获取CA信息失败");
        }
        constructMSP(user,hfcaInfo,null);
    }

    /**
     * 登记用户
     * @param enrollmentRequest
     * @return
     * @throws MyException
     */
    public static boolean enroll(com.blockchain.dto.EnrollmentRequest enrollmentRequest) throws MyException {
        if(enrollmentRequest.getOrgName() == null || enrollmentRequest.getOrgName().isEmpty()){
            throw new MyException("orgName can not be null");
        }
        if(enrollmentRequest.getEnrollmentID() == null || enrollmentRequest.getEnrollmentID().isEmpty()){
            throw new MyException("enrollmentID can not be null");
        }
        if(enrollmentRequest.getEnrollmentSecret() == null || enrollmentRequest.getEnrollmentSecret().isEmpty()){
            throw new MyException("enrollmentSecret can not be null");
        }
        HyperUser user = getMember(enrollmentRequest.getEnrollmentID(), enrollmentRequest.getOrgName(), enrollmentRequest.getProfile());
        if (user.isEnrolled()) {
            throw new MyException("用户" + enrollmentRequest.getEnrollmentID() + "已经登记");
        }
        //创建caClient实例
        HFCAClient ca = FabricHelper.getHFCAClient(enrollmentRequest.getOrgName(), enrollmentRequest.getProfile(), true);
        HFCAInfo info;
        try {
            //获取CA信息
            info = ca.info();
            //登记新用户
            org.hyperledger.fabric_ca.sdk.EnrollmentRequest er = new org.hyperledger.fabric_ca.sdk.EnrollmentRequest();
            er.setProfile(enrollmentRequest.getProfile());
            Enrollment enrollment = ca.enroll(enrollmentRequest.getEnrollmentID(), enrollmentRequest.getEnrollmentSecret(), er);
            user.setEnrollment(enrollment);
            if (enrollment != null) {
                //创建符合MSP规范的证书或TLS证书
                if (enrollmentRequest.getProfile() != null) {
                    FabricHelper.constructTLS(user, info);
                } else {
                    //获取Admin证书
                    HFCACertificateRequest hr = ca.newHFCACertificateRequest();
                    hr.setEnrollmentID(((NetworkConfig.UserInfo) FabricHelper.getRegistrar(enrollmentRequest.getOrgName(), enrollmentRequest.getProfile()).toArray()[0]).getName());
                    HFCACertificateResponse response = ca.getHFCACertificates(user, hr);
                    FabricHelper.constructMSP(user, info, response);
                }
                return true;
            } else {
                return false;
            }
        } catch (InfoException e) {
            logger.error("FabricHelper | enrollUser ", e.getMessage());
            throw new MyException("获取CA信息异常");
        } catch (EnrollmentException e) {
            logger.error("FabricHelper | enrollUser ", e.getMessage());
            throw new MyException("用户登记异常");
        } catch (HFCACertificateException e) {
            logger.error("FabricHelper | enrollUser ", e.getMessage());
            throw new MyException("获取Admin证书异常");
        } catch (Exception e){
            logger.error("FabricHelper | enrollUser ", e.getMessage());
            throw new MyException("用户登记异常");
        }
    }

}
