package com.fgsqw.ddns.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;


public class MIWIFIkey {

    public String nonce = null;
    // 小米路由的加密key
    public String key = "a2ffa5c9be07488bbb04a3a47d3c5f6a";

    public MIWIFIkey() {
        nonce = nonceCreat();
    }

    // 小米请求时设备MAC总是需要带时间后缀 这里实现拼接
    public String nonceCreat() {
        int type = 0;
        String deviceId = "c2:97:1d:aa:b1:37";
        long time = (long) Math.floor(new Date().getTime() / 1000);
        long random = (long) Math.floor(Math.random() * 10000);
        return type + "_" + deviceId + "_" + time + "_" + random;
    }


    public String oldPwd(String pwd) {
        return DigestUtils.sha1Hex(nonce + DigestUtils.sha1Hex(pwd + key));
    }

    public String getNonce() {
        return nonce;
    }

}
