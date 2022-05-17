package com.lding.controller;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.List;

@RestController
public class TestController {
    @Resource
    private ZooKeeper zooKeeper;

    @GetMapping("/createNode")
    public String createNode(String path, String data, String type) throws Exception {
        return this.zooKeeper.create(path, // 节点路径
                data.getBytes(), // 节点内容
                ZooDefs.Ids.OPEN_ACL_UNSAFE, // zookeeper权限（这里直接【完全开放】
                CreateMode.valueOf(type) // 指定节点的模式（临时、持久化等）
        );
    }

    @GetMapping("/getNode")
    public String getNode(String path) throws Exception {
        String result = "获取数据失败";
        // 1. 查看指定节点是否存在（参数watch为false代表不观察
        Stat stat = this.zooKeeper.exists(path, false);
        if (stat != null) {
            // 2. 同步获取数据
            result = "获取数据成功: " + new String(this.zooKeeper.getData(path, false, stat));
        }
        return result;
    }

    @GetMapping("/getNodeAsync")
    public String getNodeAsync(String path) throws Exception {
        String result = "异步获取数据失败";
        // 1. 查看指定节点是否存在（参数watch为false代表不观察
        Stat status = this.zooKeeper.exists(path, false);
        if (status != null) {
            result = "异步获取数据成功";
            // 2. 异步获取数据
            // 回调函数参数的含义为: int rc, String path, Object ctx, byte[] data, Stat stat
            this.zooKeeper.getData(path, false, (rc, pt, ctx, data, stat) -> {
                System.out.println("rc=" + rc + "; pt=" + pt + "; ctx=" + ctx);
                System.out.println(new String(data));
            }, "测试数据ctx");
        }
        return result;
    }

    @GetMapping("/getChildren")
    public List<String> getChildren(String path) throws Exception {
        return this.zooKeeper.getChildren(path, // 当前节点的路径
                false // 不设置watch
        );
    }

    @GetMapping("/updateNode")
    public String updateNode(String path, String data) throws Exception {
        Stat status = this.zooKeeper.exists(path, false);
        if (status != null) {
            this.zooKeeper.setData(path, // 指定路径
                    data.getBytes(), // 新内容
                    status.getVersion() // 版本号（CAS乐观锁原理
            );
        }
        return "更新成功";
    }

    @GetMapping("/deleteNode")
    public String deleteNode(String path) throws Exception {
        Stat status = this.zooKeeper.exists(path, false);
        if (status != null) {
            this.zooKeeper.delete(path, // 指定路径
                    status.getVersion() // 版本号（CAS乐观锁原理
            );
        }
        return "删除成功";
    }

    @GetMapping("/defaultWatch")
    public String defaultWatch(String path) throws Exception {
        Stat stat = this.zooKeeper.exists(path, false);
        if (stat != null) {
            // 数据改变事件（一般为get/set方法）并且只会监听一次
            this.zooKeeper.getData(path, watchedEvent -> { //
                System.out.println("事件类型: " + watchedEvent.getType());
                System.out.println("数据发送改变!");
            }, stat);
        }
        return "success";
    }

    @GetMapping("/watch")
    public String watch(String path) throws Exception {
        Stat stat = this.zooKeeper.exists(path, false);
        if (stat != null) {
            // 数据改变事件 手写无限监控版
            this.zooKeeper.getData(path, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    System.out.println("事件类型: " + watchedEvent.getType());
                    System.out.println("数据发送改变!");
                    try {
                        // 监听到后再次注册监听，形成Watcher函数的递归，达到无限监控
                        byte[] data = zooKeeper.getData(path, this, stat);
                        System.out.println("更新后的数据为: " + new String(data));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, stat);
        }
        return "success";
    }

    @GetMapping("/addWatch")
    public String addWatch(String path) throws Exception {
        Stat stat = this.zooKeeper.exists(path, false);
        // addWatch绑定永久事件（包括数据变化和子节点改变
        zooKeeper.addWatch(path, watchedEvent -> {
            System.out.println("event: " + watchedEvent.getType());
            try {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
                    // 重新获取数据
                    byte[] data = new byte[0];
                    data = zooKeeper.getData(path, false, stat);
                    System.out.println(new String(data));
                } else if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    // 重新获取子节点列表
                    List<String> children = zooKeeper.getChildren(path, false);
                    for (String s : children) {
                        System.out.println(s);
                    }
                } else {
                    System.out.println("预想外的event!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, AddWatchMode.PERSISTENT); // 简单起见，指定类型为持久性
        return "success";
    }
}
