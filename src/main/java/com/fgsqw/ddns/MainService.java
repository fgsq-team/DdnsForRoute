package com.fgsqw.ddns;

import com.fgsqw.ddns.service.DdnsService;


public class MainService {
    public static void main(final String[] args) {
        DdnsService ddnsService = new DdnsService();
        ddnsService.run();
    }

}
