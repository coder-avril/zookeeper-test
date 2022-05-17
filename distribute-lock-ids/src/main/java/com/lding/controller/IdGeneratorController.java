package com.lding.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class IdGeneratorController {
    private int count = 0;

    @GetMapping("/id")
    public String getId() {
        String id = null;
        try {
            SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
            // 生成id [2022-05-17-0]
            id = sd.format(new Date()) + "-" + count++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
}
