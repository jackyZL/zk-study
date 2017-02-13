package com.zl.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class CuratorClientTest {
    private CuratorFramework client = null;

    public CuratorClientTest() {

        // 定义重试策略（默认有四种） ExponentialBackoffRetry ，RetryNTimes , RetryOneTime, RetryUntilElapsed
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // 使用CuratorFrameworkFactory工厂的静态方法创建客户端
        client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181,localhost:2182")
                .sessionTimeoutMs(10000) // 会话超时时间
                .retryPolicy(retryPolicy)
                .namespace("base") // 设置连接根目录
                .build();

        // 启动客户端
        client.start();
    }

    /**
     * 关闭连接
     */
    public void closeClient() {
        if (client != null)
            this.client.close();
    }

    /**
     * 创建节点数据
     *
     * @param path
     * @param data
     * @throws Exception
     */
    public void createNode(String path, byte[] data) throws Exception {
        client.create().creatingParentsIfNeeded() //递归创建父目录
                .withMode(CreateMode.PERSISTENT)
                .withACL(Ids.OPEN_ACL_UNSAFE)
                .forPath(path, data);
    }

    /**
     * 删除节点
     *
     * @param path
     * @param version
     * @throws Exception
     */
    public void deleteNode(String path, int version) throws Exception {
        // client.delete().guaranteed().deletingChildrenIfNeeded().withVersion(version).inBackground(new
        // DeleteCallBack()).forPath(path);
        client.delete()
                .guaranteed()  //确保节点被删除（靠重试的方式）
                .deletingChildrenIfNeeded()  //递归删除所有子节点
                .withVersion(version) // 特定版本号
                .inBackground(new DeleteCallBack()) // 异步操作，设置回调
                .forPath(path);

    }

    /**
     * 获取节点数据
     *
     * @param path
     * @throws Exception
     */
    public void readNode(String path) throws Exception {

        Stat stat = new Stat();
        byte[] data = client.getData().storingStatIn(stat).forPath(path); // 把服务器端获取的节点数据存储到stat中
        System.out.println("读取节点" + path + "的数据:" + new String(data));
        System.out.println(stat.toString());
    }

    /**
     * 更新节点数据
     *
     * @param path
     * @param data
     * @param version
     * @throws Exception
     */
    public void updateNode(String path, byte[] data, int version)
            throws Exception {
        client.setData().withVersion(version).forPath(path, data);
    }

    /**
     * 获取节点下面的子节点
     *
     * @param path
     * @throws Exception
     */
    public void getChildren(String path) throws Exception {

        // 使用usingWatcher()设置watcher ，类似于zk本身的api，也只能使用一次
        List<String> children = client.getChildren().usingWatcher(new WatcherTest()).forPath("/curator");
        for (String pth : children) {
            System.out.println("child=" + pth);
        }
    }

    /**
     * NodeCache（设置一次，多次生效）
     • ① 监听数据节点的内容变更
     • ② 监听节点的创建，即如果指定的节点不存在，则节点创建后，会触发这个监听
     * @param path
     * @throws Exception
     */
    public void addNodeDataWatcher(String path) throws Exception {
        final NodeCache nodeC = new NodeCache(client, path);
        nodeC.start(true);

        nodeC.getListenable().addListener(new NodeCacheListener() {
            public void nodeChanged() throws Exception {
                String data = new String(nodeC.getCurrentData().getData());
                System.out.println("path=" + nodeC.getCurrentData().getPath()
                        + ":data=" + data);
            }
        });
    }

    /**
     * PathChildrenCache
     • ① 监听指定节点的子节点变化情况
     • ② 包括：新增子节点  子节点数据变更 和子节点删除
     * @param path
     * @throws Exception
     */
    public void addChildWatcher(String path) throws Exception {
        final PathChildrenCache cache = new PathChildrenCache(this.client,
                path, true);

        /*
        * • BUILD_INITIAL_CACHE //同步初始化客户端的cache，及创建cache后，就从服务器端拉入对应的数据
          • NORMAL //异步初始化cache
          • POST_INITIALIZED_EVENT //异步初始化，初始化完成触发事件PathChildrenCacheEvent.Type.INITIALIZED
        * */
        cache.start(StartMode.BUILD_INITIAL_CACHE);//ppt中需要讲StartMode


        System.out.println(cache.getCurrentData().size());

        //byte childone[] = cache.getCurrentData().get(0).getData();
//		System.out.println("childone:"
//				+ cache.getCurrentData().get(0).getPath() + ";data="
//				+ new String(childone));
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                if (event.getType().equals(PathChildrenCacheEvent.Type.INITIALIZED)) {
                    System.out.println("客户端子节点cache初始化数据完成");
                    System.out.println("size=" + cache.getCurrentData().size());
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                    System.out.println("添加子节点:" + event.getData().getPath());
                    System.out.println("修改子节点数据:" + new String(event.getData().getData()));
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                    System.out.println("删除子节点:" + event.getData().getPath());
                } else if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
                    System.out.println("修改子节点数据:" + event.getData().getPath());
                    System.out.println("修改子节点数据:" + new String(event.getData().getData()));
                }
            }
        });
    }

    public static void main(String[] args) {
        CuratorClientTest ct = null;
        try {
            ct = new CuratorClientTest();
            // ct.createNode("/curator/test10/node1", "test-node1".getBytes());
            //ct.readNode("/curator/test/node1");
            //ct.getChildren("/curator");
            // ct.updateNode("/curator/test/node1", "test-node1-new".getBytes(),0);
            // ct.readNode("/curator/test/node1");
            // ct.deleteNode("/curator/test10", 0);

            ct.addNodeDataWatcher("/curator/test7");
            ct.addChildWatcher("/curator");

            // 主线程不能退出，否则设置的watcher回调不了
            Thread.sleep(300000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ct.closeClient();
        }

    }

}
