package com.fgsqw.ddns.util;

import net.sf.json.JSONObject;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 爬取openwrt 获取外网IP
 */
public class OpenWrtPubIP implements GetIP {

    private static final Logger logger = Logger.getLogger(OpenWrtPubIP.class);
    String WrtIP = PropertiesUtil.getProperty("route.managerIP");

    String cookie = "d6a42c3d466e46d2d1ba67089ac20543";

    public String getWrtCookie() throws IOException {
        logger.warn("WrtIP = " + WrtIP);
        String url = "http://" + WrtIP + "/cgi-bin/luci/";

        URL temp = new URL(url);

        String host = temp.getHost();
        String path = temp.getPath();

        int port = temp.getPort() == -1 ? 80 : temp.getPort();

        Socket socket = new Socket();
        SocketAddress address = new InetSocketAddress(host, port);
        socket.connect(address, 5000);

        String data = "luci_username=" + PropertiesUtil.getProperty("route.username")
                + "&luci_password=" + PropertiesUtil.getProperty("route.password");

        String requestString = "POST " + path + " HTTP/1.1\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "Content-Length: " + data.length() + "\r\n" +
                "\r\n" +
                data;

        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        os.write(requestString.getBytes());
        os.flush();

        BufferedInputStream streamReader = new BufferedInputStream(is);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, StandardCharsets.UTF_8));
        String line = null;
        while (true) {
            line = bufferedReader.readLine();
            if (line == null || line.equals("")) {
                logger.error("获取cookie失败");
                try {
                    os.close();
                    is.close();
                    socket.close();
                } catch (Exception ignored) {
                }
                return null;
            }
            if (line.contains("Set-Cookie")) {
                cookie = line.substring(line.indexOf("=") + 1, line.lastIndexOf(";"));
                logger.info("cookie: " + cookie);
                try {
                    os.close();
                    is.close();
                    socket.close();
                } catch (Exception ignored) {
                }
                return cookie;
            }

        }
    }

    public Map<String, String> getIP(int flag) throws IOException {
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
            logger.error("返回数据错误");
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

        Map<String, String> pubIP = new HashMap<>();
        pubIP.put("*", ipaddr);
        return pubIP;

    }

    public static void main(String[] args) throws IOException {
        OpenWrtPubIP openWrtPubIP = new OpenWrtPubIP();
        String ip = openWrtPubIP.getIP(0).get(0);
        System.out.println("ip = " + ip);
//        System.out.println("cookie = " + openWrtPubIP.getWrtCookie());

    }

}
