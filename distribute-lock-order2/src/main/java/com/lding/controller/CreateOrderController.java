package com.lding.controller;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;

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
    public String createOrder() {
        String keyNode = PATH + ORDER_KEY;
        // 1. 获取ID
        if (tryLock(keyNode)) { // 尝试获取锁
            String id = template.getForObject("http://localhost:5000/id", String.class);
            System.out.println("成功获取id: " + id + " ,并执行完业务!");
            unLock(keyNode); // 释放锁
        } else {
            waitLock(); // 阻塞等待
        }
        return "success";
    }

    /**
     * 尝试获取锁
     * 本质其实是写入指定的临时节点，如果无法成功写入，代表已经有别的task提前写入了，意味着获取锁失败；反之，则意味着获取成功
     * @param keyNode 分布式锁的key
     * @return 是否成功获取锁
     */
    private boolean tryLock(String keyNode) {
        try {
            zooKeeper.create(keyNode, // 指定的分布式锁的key
                    null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * 释放锁资源
     * 本质就是删除指定的节点
     * @param keyNode 分布式锁的key
     */
    private void unLock(String keyNode) {
        try {
            Stat status = zooKeeper.exists(keyNode, false);
            if (status != null) {
                zooKeeper.delete(keyNode, status.getVersion());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());;
        }
    }

    /**
     * 阻塞状态 - 等待锁
     * 本质就是监听目标节点的子节点是否发生变化
     * 特别注意，这里主要是为了关注首次的删除节点事件，之后别的task碰巧先写入造成的添加事件是不需要出发的，所以应该用一次性监听
     */
    private void waitLock() {
        try { // 尝试获取目标节点的子节点
            zooKeeper.getChildren(PATH, watchedEvent -> { // 一次性监控
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    // 一旦发现节点有变化，再次尝试生成orderId
                    this.createOrder();
                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
