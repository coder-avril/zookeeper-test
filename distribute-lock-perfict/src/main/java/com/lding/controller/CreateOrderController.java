package com.lding.controller;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@RestController
public class CreateOrderController {
    // 发送http请求
    private RestTemplate template = new RestTemplate();
    // 定义分布式锁的path和节点
    private static final String PATH = "/locks";
    private static final String ORDER_KEY = "/orderId";

    @Resource
    private ZooKeeper zooKeeper;

    @GetMapping("/orderId")
    public String createOrder() throws Exception {
        String keyNode = PATH + ORDER_KEY;
        // 1. 创建一个临时顺序节点
        String currentFullPath = zooKeeper.create(keyNode, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        String currentPath = currentFullPath. // 单独获得子节点名称
                substring(currentFullPath.lastIndexOf("/") + 1);
        // 2. 获取ID
        if (tryLock(currentPath)) { // 尝试获取锁
            String id = template.getForObject("http://localhost:5000/id", String.class);
            System.out.println("成功获取id: " + id + " ,并执行完业务!");
            unLock(currentPath); // 释放锁
        } else {
            waitLock(currentPath); // 阻塞等待
        }
        return "success";
    }

    /**
     * 尝试获取锁
     * @param currentPath 子节点名称（不带路径）
     * @return 是否成功获得锁
     */
    private boolean tryLock(String currentPath) {
        try {
            // 1. 获取所有的子节点列表
            List<String> children = zooKeeper.getChildren(PATH, false);
            Collections.sort(children);
            // 2. 判断当前的currentPath是否是最小的节点
            if (StringUtils.pathEquals(currentPath, children.get(0))) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }


    /**
     * 释放锁
     * @param currentPath 子节点名称（不带路径）
     */
    private void unLock(String currentPath) {
        try {
            Stat status = zooKeeper.exists(PATH + "/" + currentPath, false);
            if (status != null) {
                zooKeeper.delete(PATH + "/" + currentPath, status.getVersion());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 阻塞状态 - 等待锁
     */
    private void waitLock(String currentPath) {
        try {
            // 1. 获取所有的子节点列表
            List<String> children = zooKeeper.getChildren(PATH, false);
            Collections.sort(children);
            // 2. 获取当前节点的位置，找到前一个节点（也就是目前正得到锁，导致当前节点等待的原因）
            int index = children.indexOf(currentPath);
            if (index > 0) {
                String preNode = children.get(index - 1);
                // 3. 对前一个节点进行删除事件绑定
                zooKeeper.getData(PATH + "/" + preNode, watchedEvent -> {
                    if (watchedEvent.getType() ==
                            Watcher.Event.EventType.NodeDeleted) {
                        // 调用业务方法
                        String id = template.getForObject("http://localhost:5000/id", String.class);
                        System.out.println("成功获取id: " + id + " ,并执行完业务!");
                        // 释放锁
                        unLock(currentPath);
                    }
                }, new Stat());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
