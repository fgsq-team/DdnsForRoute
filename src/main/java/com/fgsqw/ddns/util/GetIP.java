package com.fgsqw.ddns.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface GetIP {

     Map<String,String> getIP(int flag) throws IOException;
}
