package com.fgsqw.ddns.util;

import kotlin.Pair;
import net.sf.json.JSONObject;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * 爬取openwrt 获取外网IP
 */
public class OpenWrtPubIP implements GetIP {

    private static final Logger logger = Logger.getLogger(OpenWrtPubIP.class);
    String WrtIP = PropertiesUtil.getProperty("route.managerIP");
    String cookie = "d6a42c3d466e46d2d1ba67089ac20543";

    public String getWrtCookie() throws IOException {
        String url = "http://" + WrtIP + "/cgi-bin/luci/";
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder().build();

        FormBody.Builder builder = new FormBody.Builder()
                .add("luci_username", PropertiesUtil.getProperty("route.username"))
                .add("luci_password", PropertiesUtil.getProperty("route.password"));

        RequestBody formBody = builder.build();

        Request request = new Request.Builder()
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Origin", "http://" + WrtIP)
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .addHeader("Cache-Control", "max-age=0")
                .addHeader("Connection", "keep-alive")
                .addHeader("Host", WrtIP)
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .url(url)
                .post(formBody).build(); // 请求
        Response execute = okHttpClient.newCall(request).execute();
        Headers headers = execute.priorResponse().headers();

        for (Pair<? extends String, ? extends String> header : headers) {
            if (header.component1().equals("Set-Cookie")) {
                String cookie = header.component2();
                cookie = cookie.substring(cookie.indexOf("=") + 1, cookie.lastIndexOf(";"));
                logger.info("cookie: " + cookie);
                return cookie;
            }
        }
        logger.warn("获取cookie失败");
        return null;
    }

    public String getIP(int flag) throws IOException {
        String url = "http://" + WrtIP + "/cgi-bin/luci/admin/status/overview?status=1";
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder().build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", "sysauth=" + cookie)
                .get().build(); // 请求

        Response execute = okHttpClient.newCall(request).execute();
        String string = execute.body().string();
        if (StringUtils.isEmpty(string)) {
            System.out.println("返回数据错误");
            return null;
        }

        if (string.contains("需要授权")) {
            logger.warn("cookie 获取失败");
            if (flag == 1) {
                return null;
            }
            logger.warn("cookie失效重新登录");
            cookie = getWrtCookie();
            return getIP(1);
        }

        JSONObject jsonObject = JSONObject.fromObject(string);
        JSONObject wan = jsonObject.getJSONObject("wan");
        String ipaddr = wan.getString("ipaddr");
        logger.info("pubIP = " + ipaddr);
        return ipaddr;

    }

    public static void main(String[] args) throws IOException {
        OpenWrtPubIP openWrtPubIP = new OpenWrtPubIP();
        String ip = openWrtPubIP.getIP(0);
        System.out.println("ip = " + ip);
    }

}
