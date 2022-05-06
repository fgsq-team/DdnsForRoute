package com.fgsqw.ddns.util;


import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

/**
 * 读取配置文件
 */

public class PropertiesUtil {
    public static Properties prop = new Properties();

    public static String getProperty(String key) {
        // 尝试从外部读取ddns.properties文件
        File file = new File("ddns.properties");
        InputStream is = null;
        if (file.exists()) {
            try {
                is = Files.newInputStream(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                is = PropertiesUtil.class.getClassLoader().getResourceAsStream("ddns.properties");
            }
        } else {
            is = PropertiesUtil.class.getClassLoader().getResourceAsStream("ddns.properties");
        }
        try {
            prop.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop.getProperty(key);
    }

}
