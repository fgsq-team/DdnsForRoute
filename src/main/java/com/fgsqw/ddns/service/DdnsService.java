package com.fgsqw.ddns.service;

import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsResponse;
import com.fgsqw.ddns.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;


public class DdnsService {
    private static Logger logger = Logger.getLogger(DdnsService.class);
    GetIP getIP;
    Map<String, String> IPS = new HashMap<>();

    public DdnsService() {
        int routeType = Integer.parseInt(PropertiesUtil.getProperty("route.type"));
        if (routeType == 1) {     // 爬取小米
            logger.info("开始爬取小米路由");
            getIP = new MiPubIP();
        } else if (routeType == 2) {   // 爬取openWrt
            logger.info("开始爬取openWrt");
            getIP = new OpenWrtPubIP();
        } else if (routeType == 3) {
            logger.info("开始爬取爱快");
            getIP = new IKuaiPubIP();
        }
    }

    public void run() {
        try {
            service();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("出现错误: ", e);
        } finally {
            int checkTime = Integer.parseInt(PropertiesUtil.getProperty("intervalTime"));
            try {
                Thread.sleep(checkTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            run();
        }
    }

    /**
     * 检测域名解析
     *
     * @throws Exception cuo
     */
    private void service() throws Exception {

        logger.info("正在爬取主机公网IP地址...");
        Map<String, String> maPubIPS = getIP.getIP(0);

        if (maPubIPS == null || maPubIPS.size() <= 0) {
            logger.warn("获取公网IP失败");
            return;
        }

        logger.info("pubIPS = " + maPubIPS);

        logger.info("开始检测域名解析...");
        String[] chiledDomainNames = PropertiesUtil.getProperty("ChiledDomainNames").split(",");
        for (String chiledDomainName : chiledDomainNames) {
            String[] split = chiledDomainName.split(":");
            String interfaceName = split[0];
            String subDomainName = split[1];
            String IP = IPS.get(chiledDomainName);
            String pubIP = maPubIPS.get(interfaceName);
            boolean change = false;
            if (!StringUtils.isEmpty(IP)) {
                if (!StringUtils.isEmpty(pubIP)) {
                    if (!IP.equals(pubIP)) {
                        change = true;
                    }
                }
            } else {
                change = true;
            }

            if (change) {
                List<DescribeSubDomainRecordsResponse.Record> subDomainRecordsList = AliDdnsUtils.getDescribeSubDomainRecords(subDomainName);//获取子域名信息
                if (subDomainRecordsList == null) {
                    continue;
                }
                if (subDomainRecordsList.size() == 0) {//没有这个域名
                    //添加
                    logger.info("添加解析记录：" + subDomainName + ":" + pubIP);
                    boolean isSuccess = AliDdnsUtils.addDomainRecord(subDomainName, pubIP);
                    if (isSuccess) {
                        IPS.put(chiledDomainName, pubIP);
                    }
                } else {//有
                    //判断解析值是否符合当前ip
                    DescribeSubDomainRecordsResponse.Record record = subDomainRecordsList.get(0);
                    if (!record.getValue().equals(pubIP) || !record.getType().equals("A") || !record.getRR().equals(subDomainName)) {//不相等,修改
                        // 外网ip和解析的ip不一致
                        logger.info("修改解析记录：" + subDomainName + ":" + pubIP);
                        boolean isSuccess = AliDdnsUtils.editDomainRecord(record.getRecordId(), subDomainName, pubIP);
                        if (isSuccess) {
                            IPS.put(chiledDomainName, pubIP);
                        }
                    } else {
                        // 外网ip和解析的ip一致
                        IPS.put(chiledDomainName, pubIP);
                    }
                }

            }
        }

        logger.info("检测完毕...");
    }

}
