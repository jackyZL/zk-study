package com.zl.watcher;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Created by jacky on 2016/11/23.
 */
public class WatcherRegister {

    private ZooKeeper zk = null;

    public WatcherRegister(String connectString, Watcher watcher){
        try {
            zk = new ZooKeeper(connectString,10000,watcher);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void testWatcherDisable(String path) throws KeeperException, InterruptedException {

        WatcherExample1 wx1 = new WatcherExample1();
        zk.getData(path,wx1,null); //不使用默认的wathcer，而是使用WatcherExample1的实例

    }

    public static void main(String[] args) {
        WatcherExample2 wx2 = new WatcherExample2();
        // 传入默认的watcher
        WatcherRegister wr = new WatcherRegister("192.168.43.132:2181", wx2);

        try {
            wr.testWatcherDisable("/node7");

            Thread.sleep(300000);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
