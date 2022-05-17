package com.lding.jvmLockDemo;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class OrderService implements Runnable{
    // id生成器
    private static OrderIdGenerator idGenerator = new OrderIdGenerator();
    // 用来保存生成的id
    private static Set<String> ids = new HashSet<>();
    // 归零计数器:
    // 1. 其他线程调用CountDown方法会将计数器减1（调用CountDown方法的线程不会阻塞）
    // 2. 当计数器变为0时，await 方法阻塞的线程会被唤醒，继续执行
    private static CountDownLatch countDownLatch = new CountDownLatch(50);

    public static void main(String[] args) throws InterruptedException {
        OrderService service = new OrderService();
        // 开启50个线程
        for (int i = 0; i < 50; i++) {
            new Thread(service).start();
        }
        // 等待所有线程执行完id获取后，再去执行主线程
        countDownLatch.await();
        System.out.println("获得的id总数为: " + ids.size());
        System.out.println(ids);
    }

    @Override
    public void run() {
        // 调用id的生成方法
        ids.add(idGenerator.getId());
        countDownLatch.countDown();
    }
}
