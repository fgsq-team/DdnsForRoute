package com.fgsqw.ddns.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class IKuaiPubIP implements GetIP {

    private static final Logger logger = Logger.getLogger(MiPubIP.class);
    private static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");
    String managerIP = PropertiesUtil.getProperty("route.managerIP");
    String cookie = "d6a42c3d466e46d2d1ba67089ac20543";


    @Override
    public Map<String,String> getIP(int flag) throws IOException {
        String managerIP = "192.168.33.1";

        String url = "http://" + managerIP + "/Action/call";


        JSONObject json = new JSONObject();
        json.put("action", "show");
        json.put("func_name", "monitor_iface");

        JSONObject param = new JSONObject();
        param.put("TYPE", "iface_check,iface_stream");
        json.put("param", param);


        RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));

        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder().build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", "sess_key=" + cookie)
                .post(requestBody).build(); // 请求

        Response execute = okHttpClient.newCall(request).execute();

        String string = execute.body().string();
        if (StringUtils.isEmpty(string)) {
            logger.error("返回数据错误");
            return null;
        }

        JSONObject rel = JSONObject.fromObject(string);
        int result = rel.getInt("Result");
        if (result == 30000) {
            logger.info("cookie 检验成功");
            JSONObject info = rel.getJSONObject("Data");
            JSONArray iface_check = info.getJSONArray("iface_check");
            Map<String,String> pubIps = new HashMap<>();
            for (int i = 0; i < iface_check.size(); i++) {
                JSONObject data = iface_check.getJSONObject(i);
                // adsl1 是自己设置的拨号名称 此处逻辑可以改成自己想要的
//              if (data != null && data.getString("interface").startsWith("adsl")) {
                pubIps.put(data.getString("interface"),data.getString("ip_addr"));
//              }
            }
            return pubIps;
        } else {
            logger.error(" ErrMsg " + rel.getString("ErrMsg"));
            // 账号或密码错误
            if (result == 10014) {
                logger.warn("cookie 失效");
            }

            if (flag == 1) {
                return null;
            }
            logger.warn("cookie 失效重新获取cookie");
            cookie = getCookie();
            return getIP(1);
        }
    }


    // 获取登录cookie
    public String getCookie() throws IOException {

        String userName = PropertiesUtil.getProperty("route.username");
        String password = PropertiesUtil.getProperty("route.password");

        String salt_password = "salt_11" + password;

        String url = "http://" + managerIP + "/Action/login";
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .followRedirects(false)
                .build();

        JSONObject json = new JSONObject();
        json.put("pass", new String(Base64.getEncoder().encode(salt_password.getBytes())));
        json.put("remember_password", "");
        json.put("passwd", utils.md5(password));
        json.put("username", userName);

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
                .post(RequestBody.create(JSON, String.valueOf(json))).build(); // 请求

        Response execute = okHttpClient.newCall(request).execute();

        String string = execute.body().string();
        JSONObject rel = JSONObject.fromObject(string);
        int anInt = rel.getInt("Result");
        if (anInt == 10000) {
            Headers headers = execute.headers();
            String setCookie = headers.get("Set-Cookie");
            if (!StringUtils.isEmpty(setCookie)) {
                return setCookie.substring(setCookie.indexOf("=") + 1, setCookie.indexOf(";"));
            }
        } else {
            logger.error(" ErrMsg " + rel.getString("ErrMsg"));
        }
        return null;
    }


    public static void main(String[] args) throws IOException {
        new IKuaiPubIP().getIP(0);
//        logger.info("pubIP = " + cookie);
    }


}
