package com.fgsqw.ddns.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * 通过小米路由获取外网ip
 */
public class MiPubIP implements GetIP {
    private static final Logger logger = Logger.getLogger(MiPubIP.class);
    String managerIP = PropertiesUtil.getProperty("route.managerIP");
    String cookie = "d6a42c3d466e46d2d1ba67089ac20543";

    // 获取登录cookie
    public String getCookie() throws IOException {
        String url = "http://" + managerIP + "/cgi-bin/luci/api/xqsystem/login";
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .followRedirects(false)
                .build();

        MIWIFIkey miwifIkey = new MIWIFIkey();

        FormBody.Builder builder = new FormBody.Builder()
                .add("username", PropertiesUtil.getProperty("route.username"))
                .add("password", miwifIkey.oldPwd(PropertiesUtil.getProperty("route.password")))
                .add("nonce", miwifIkey.getNonce());

        RequestBody formBody = builder.build();

        Request request = new Request.Builder()
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Origin", "http://" + managerIP)
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .addHeader("Cache-Control", "max-age=0")
                .addHeader("Connection", "keep-alive")
                .addHeader("Host", managerIP)
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .url(url)
                .post(formBody).build(); // 请求

        Response execute = okHttpClient.newCall(request).execute();

        String string = execute.body().string();
        if (StringUtils.isEmpty(string)) return null;

        JSONObject jsonObject = JSONObject.fromObject(string);
        String token = jsonObject.getString("token");

        logger.info("token: " + token);

        return token;
    }


    // 爬取IP
    public String getIP(int flag) throws IOException {

        String url = "http://" + managerIP + "/cgi-bin/luci/;stok=" + cookie + "/api/xqnetwork/wan_info";
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder().build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", "sysauth=" + cookie)
                .get().build(); // 请求

        Response execute = okHttpClient.newCall(request).execute();

        String string = execute.body().string();
        if (StringUtils.isEmpty(string)) {
            logger.info("返回数据错误");
            return null;
        }
        if (string.contains("Invalid token")) {
            logger.info("cookie 获取失败");
            if (flag == 1) {
                return null;
            }

            logger.info("cookie 失效重新登录");
            cookie = getCookie();
            return getIP(1);
        }

        JSONObject jsonObject = JSONObject.fromObject(string);
        JSONObject info = jsonObject.getJSONObject("info");
        JSONArray ipv4 = info.getJSONArray("ipv4");
        JSONObject ipv4JSONObject = ipv4.getJSONObject(0);
        String ip = ipv4JSONObject.getString("ip");

        logger.info("pubIP = " + ip);
        return ip;

    }


    public static void main(String[] args) throws IOException {
        GetIP miPubIP = new MiPubIP();
        String ip = miPubIP.getIP(0);
        System.out.println("ip = " + ip);
    }


}
