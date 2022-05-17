package com.lding.config;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.CountDownLatch;

@Configuration
public class ZookeeperCfg {
    private static final String ZK_SERVER_ADDR = "192.168.0.10:2181,192.168.0.11:2181,192.168.0.12:2181";
    private static final int SESSION_TIMEOUT = 30_000;
    private ZooKeeper zooKeeper;

    /**
     * 创建一个zooKeeper连接
     * @return zooKeeper
     */
    @Bean
    public ZooKeeper zooKeeper() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1); // 并发等待
        zooKeeper = new ZooKeeper(ZK_SERVER_ADDR, // 连接地址和端口
                SESSION_TIMEOUT, // 会话超时时间
                event -> { // 事件监听程序
                    System.out.println("event = " + event);
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        System.out.println("zooKeeper客户端连接成功");
                        countDownLatch.countDown();
                    }
                });
        countDownLatch.await();
        return zooKeeper;
    }
}
