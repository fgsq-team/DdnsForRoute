package com.fgsqw.ddns.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 调用js测试
 */
public class JsTest {


    public static void main(String[] args) throws Exception {
        testJSFile();
    }

    public static String testJSFile() throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("javascript");
        engine.eval(readJSFile());
        Invocable inv = (Invocable) engine;
        Object res = (Object) inv.invokeFunction("nonceCreat", new String[] { "5050412", "D" });
        System.out.println("res:" + res);
        return (String) res;
    }

    private static String readJSFile() throws Exception {
        StringBuffer script = new StringBuffer();
        File file = new File("C:\\Project\\Java\\AliYunDdns\\src\\main\\resources\\f.js");
        FileReader filereader = new FileReader(file);
        BufferedReader bufferreader = new BufferedReader(filereader);
        String tempString = null;
        while ((tempString = bufferreader.readLine()) != null) {
            script.append(tempString).append("\n");
        }
        bufferreader.close();
        filereader.close();
        return script.toString();
    }
}
