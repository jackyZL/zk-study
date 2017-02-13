package com.zl.watcher;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by jacky on 2016/11/22.
 */
public class WatcherExample1 implements Watcher {

    private String zkpath = "192.168.43.132:2181";

    private CountDownLatch connectedSemaphore = new CountDownLatch(1);


    @Override
    public void process(WatchedEvent watchedEvent) {


        System.out.println("watcher=" + this.getClass().getName());

        // 获取监听的节点路径
        System.out.println("path=" + watchedEvent.getPath());

        // 事件类型
        System.out.println("eventType=" + watchedEvent.getType().name());


        // 在收到消息之后，再次注册
        WatcherExample1 wx1 = new WatcherExample1();
        try {
            ZooKeeper zk = new ZooKeeper(wx1.getZkpath(), 10000, wx1);
            zk.getData(watchedEvent.getPath(), wx1, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }

    }

    public String getZkpath() {
        return zkpath;
    }

    public void setZkpath(String zkpath) {
        this.zkpath = zkpath;
    }

    public static void main(String[] args) {

        WatcherExample1 wx = new WatcherExample1();

        try {
            // Zookeeper构造函数，传入默认的Wathcer
            ZooKeeper zk = new ZooKeeper(wx.getZkpath(), 10000, wx);

            // 使用默认的watcher -> DefaultWathcer , 也可以这样：zk.getChildren("/node7", true);
            List<String> children = zk.getChildren("/node7", wx);

            // 使用watcher模式
            //zk.getChildren("/node7", true);

            // 休眠300秒，等待watcher被调用
            Thread.sleep(300000);

            //System.out.println(children);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
