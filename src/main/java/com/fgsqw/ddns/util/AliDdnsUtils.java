package com.fgsqw.ddns.util;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云解析工具
 *
 * @author xiang
 */
public class AliDdnsUtils {
    private static Logger logger = Logger.getLogger(AliDdnsUtils.class);

    private static IAcsClient client = null;

    private static String regionId;
    private static String accessKeyId;
    private static String accessKeySecret;


    static {
        regionId = "cn-hangzhou"; // 必填固定值，必须为“cn-hanghou”
        accessKeyId = PropertiesUtil.getProperty("AccessKeyID").trim(); // your accessKey
        accessKeySecret = PropertiesUtil.getProperty("AccessKeySecret").trim();// your accessSecret
        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        client = new DefaultAcsClient(profile);
    }

    /**
     * 重新加载客户端
     */
    public static void reload() {
        if (!accessKeyId.equals(PropertiesUtil.getProperty("AccessKeyID")) || !accessKeySecret.equals(PropertiesUtil.getProperty("AccessKeySecret"))) {
            regionId = "cn-hangzhou"; // 必填固定值，必须为“cn-hanghou”
            accessKeyId = PropertiesUtil.getProperty("AccessKeyID"); // your accessKey
            accessKeySecret = PropertiesUtil.getProperty("AccessKeySecret");// your accessSecret
            IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            client = new DefaultAcsClient(profile);
            logger.info("更新了AccessKey");
        }
    }


    /**
     * 获取客户端数据
     *
     * @return
     */
    public static IAcsClient getClient() {
        reload();
        return client;
    }

    /**
     * 添加解析记录
     *
     * @param subDomain 子域名
     * @param value
     * @return
     */
    public static boolean addDomainRecord(String subDomain, String value) {
        IAcsClient client = AliDdnsUtils.getClient();//获取客户端
        AddDomainRecordRequest request = new AddDomainRecordRequest();
        request.setDomainName(PropertiesUtil.getProperty("DomainName"));//设置主域名
        request.setRR(subDomain);//设置 子域名
        request.setType("A");//设置type 解析的是个IPV4
        request.setTTL(60L);
        request.setValue(value);//设置 解析的地址
        try {
            AddDomainRecordResponse response = client.getAcsResponse(request);
            return true;
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            logger.error("ErrCode:" + e.getErrCode());
            logger.error("ErrMsg:" + e.getErrMsg());
            logger.error("RequestId:" + e.getRequestId());
        }
        return false;
    }

    /**
     * 删除解析记录
     *
     * @param recordId
     * @return
     */
    public static boolean delDomainRecord(String recordId) {
        IAcsClient client = AliDdnsUtils.getClient();//获取客户端
        DeleteDomainRecordRequest request = new DeleteDomainRecordRequest();
        request.setRecordId(recordId);
        try {
            DeleteDomainRecordResponse response = client.getAcsResponse(request);
            return true;
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            logger.error("ErrCode:" + e.getErrCode());
            logger.error("ErrMsg:" + e.getErrMsg());
            logger.error("RequestId:" + e.getRequestId());
        }
        return false;
    }

    public static boolean editDomainRecord(String recordId, String rR, String value) {
        IAcsClient client = AliDdnsUtils.getClient();//获取客户端
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setRecordId(recordId);//
        // 设置 主机记录 也就是 子域名
        request.setRR(rR);
        // 类型 A  将一个域名解析到一个ipv4地址
        request.setType("A");
        // 记录值  也就是IP
        request.setValue(value);
        request.setTTL(60L);
        try {
            client.getAcsResponse(request);
            return true;
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            logger.error("ErrCode:" + e.getErrCode());
            logger.error("ErrMsg:" + e.getErrMsg());
            logger.error("RequestId:" + e.getRequestId());
        }
        return false;
    }

    /**
     * 获取子域名的记录列表
     */
    public static List<DescribeSubDomainRecordsResponse.Record> getDescribeSubDomainRecords(String subDomainName) {
        List<DescribeSubDomainRecordsResponse.Record> records = new ArrayList<>();
        // 获取客户端
        IAcsClient client = AliDdnsUtils.getClient();
        // 构造一个获取子域名的记录列表请求
        DescribeSubDomainRecordsRequest request = new DescribeSubDomainRecordsRequest();
        // 如 "a.b"+"."+"xxx.com"
        request.setSubDomain(subDomainName + "." + PropertiesUtil.getProperty("DomainName"));
        try {
            DescribeSubDomainRecordsResponse response = client.getAcsResponse(request);
            records.addAll(response.getDomainRecords());
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            logger.error("ErrCode:" + e.getErrCode());
            logger.error("ErrMsg:" + e.getErrMsg());
            logger.error("RequestId:" + e.getRequestId());
            return null;
        }
        return records;
    }

    /**
     * 获取解析记录list
     */
    public static List<DescribeDomainRecordsResponse.Record> getDescribeDomainRecords() {
        List<DescribeDomainRecordsResponse.Record> records = new ArrayList<>();
        // 获取客户端
        IAcsClient client = AliDdnsUtils.getClient();
        // 构造一个获取解析记录列表请求
        DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
        // 获取主域名
        request.setDomainName(PropertiesUtil.getProperty("DomainName"));
        // 设置请求的每页的条数 10条
        request.setPageSize(10l);
        try {
            DescribeDomainRecordsResponse response = client.getAcsResponse(request);
            records.addAll(response.getDomainRecords());
            // 如果还有下一页
            while (response.getPageNumber() * response.getPageSize() < response.getTotalCount()) {
                // 设置页数加一
                request.setPageNumber(response.getPageNumber() + 1);
                // 再次请求
                response = client.getAcsResponse(request);
                // 添加到list里面
                records.addAll(response.getDomainRecords());
            }
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            logger.error("ErrCode:" + e.getErrCode());
            logger.error("ErrMsg:" + e.getErrMsg());
            logger.error("RequestId:" + e.getRequestId());
        }
        return records;
    }
}
