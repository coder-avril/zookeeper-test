package com.lding.jvmLockDemo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class OrderIdGenerator {
    private int count = 0;

    public synchronized String getId() {
        String id = null;
        try {
            TimeUnit.MILLISECONDS.sleep(50); // 模拟网络延迟
            SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
            // 生成id [2022-05-17-0]
            id = sd.format(new Date()) + "-" + count++;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return id;
    }
}
