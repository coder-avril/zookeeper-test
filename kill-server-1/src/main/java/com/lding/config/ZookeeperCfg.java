package com.lding.config;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class ZookeeperCfg {
    private static final String ZK_SERVER_ADDR = "192.168.0.10:2181,192.168.0.11:2181,192.168.0.12:2181";
    private static final int SESSION_TIMEOUT = 30_000;
    private static final String PATH = "/servers";
    private static final String SUB_PATH = "/killServer";
    // 自定义的秒杀服务IP和端口
    private static final String KILL_SERVER = "192.168.100.130:8888";

    private ZooKeeper zooKeeper;

    /**
     * 创建一个zooKeeper连接
     * @return zooKeeper
     */
    @Bean
    public ZooKeeper zooKeeper() throws IOException {
        zooKeeper = new ZooKeeper(ZK_SERVER_ADDR, // 连接地址和端口
                SESSION_TIMEOUT, // 会话超时时间
                watchedEvent -> { // 事件监听程序
                    System.out.println("event = " + watchedEvent);
                    if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        System.out.println("zooKeeper客户端连接成功");
                        try {
                            // 注册对应的信息
                            zooKeeper.create(PATH + SUB_PATH, KILL_SERVER.getBytes(),
                                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                    CreateMode.EPHEMERAL_SEQUENTIAL);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        return zooKeeper;
    }
}
