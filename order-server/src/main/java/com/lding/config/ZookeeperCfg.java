package com.lding.config;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ZookeeperCfg {
    private static final String ZK_SERVER_ADDR = "192.168.0.10:2181,192.168.0.11:2181,192.168.0.12:2181";
    private static final int SESSION_TIMEOUT = 30_000;
    private static final String PATH = "/servers";
    // 获取到的最新秒杀服务器地址
    private static List<String> tempAddr = null;

    private ZooKeeper zooKeeper;

    /**
     * 创建一个zooKeeper连接
     * @return zooKeeper
     */
    @Bean
    public ZooKeeper zooKeeper() throws IOException {
        zooKeeper = new ZooKeeper(ZK_SERVER_ADDR, // 连接地址和端口
                SESSION_TIMEOUT, // 会话超时时间
                event -> { // 事件监听程序
                    System.out.println("event = " + event);
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        System.out.println("zooKeeper客户端连接成功");
                        try {
                            // 1. 获取对应的地址列表
                            this.setData();
                            // 2. 绑定永久的事件监听
                            zooKeeper.addWatch(PATH, watchedEvent -> {
                                try {
                                    this.setData();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }, AddWatchMode.PERSISTENT);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        return zooKeeper;
    }

    private void setData() throws Exception {
        // 得到检测节点的子节点路径
        List<String> children = zooKeeper.getChildren(PATH, false);
        tempAddr = new ArrayList<>();
        for (String child: children) {
            // 获取子节点路径的数据
            byte[] data = zooKeeper.getData(PATH + "/" + child, false, new Stat());
            tempAddr.add(new String(data));
        }
        System.out.println("获得秒杀服务的最新地址: " + tempAddr);
    }
}

