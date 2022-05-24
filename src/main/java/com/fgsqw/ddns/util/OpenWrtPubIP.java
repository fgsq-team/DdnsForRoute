package com.fgsqw.ddns.util;

import kotlin.Pair;
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

    public static String sendRequest(String urlParam, String requestType) {

        HttpURLConnection con = null;

        BufferedReader buffer = null;
        StringBuffer resultBuffer = null;

        try {
            URL url = new URL(urlParam);
            //得到连接对象
            con = (HttpURLConnection) url.openConnection();
            //设置请求类型
            con.setRequestMethod(requestType);

            //设置请求需要返回的数据类型和字符集类型
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("User-Agent", "");
            //允许写出
            con.setDoOutput(true);
            //允许读入
            con.setDoInput(true);
            //不使用缓存
            con.setUseCaches(false);

            con.addRequestProperty("luci_username", "root");
            con.addRequestProperty("luci_password", "password");

            //得到响应码
            int responseCode = con.getResponseCode();

//            if (responseCode == HttpURLConnection.HTTP_OK) {
            //得到响应流
            InputStream inputStream = con.getInputStream();
            //将响应流转换成字符串
            resultBuffer = new StringBuffer();
            String line;
            buffer = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while ((line = buffer.readLine()) != null) {
                resultBuffer.append(line);
            }
            return resultBuffer.toString();
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

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
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, "utf-8"));
        String line = null;
        while (true) {
            line = bufferedReader.readLine();
            if (line == null || line.equals("")) {
                logger.info("获取cookie失败");
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
//        System.out.println("cookie = " + openWrtPubIP.getWrtCookie());

    }

}
