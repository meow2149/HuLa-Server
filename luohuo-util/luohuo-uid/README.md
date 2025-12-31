# luohuo-uid 说明

本模块参考 [baidu/uid-generator](https://github.com/baidu/uid-generator)。由于原项目未发布正式版本，这里将源码复制进来方便使用；如有侵权请联系作者删除。

## 与原项目的差异

1. `WorkerNodeDAO` 从 `com.baidu.fsg.uid.worker.dao.WorkerNodeDAO` 移动到 `com.luohuo.basic.uid.dao.WorkerNodeDAO`
2. `DisposableWorkerIdAssigner#assignWorkerId` 的事务增加了：`(rollbackFor = Exception.class)`

## 参考资料

- 源码仓库：[baidu/uid-generator](https://github.com/baidu/uid-generator)
- 原理文章：[uid-generator 原理解析](https://www.cnblogs.com/csonezp/p/12088432.html)

## 关于 UID 比特分配的建议

### 场景 1：并发要求不高、期望长期使用

可增加 `timeBits` 位数，减少 `seqBits` 位数。示例：节点采取“用完即弃”的 `WorkerIdAssigner` 策略，重启频率为 `12 次/天`，配置：

```json
{"workerBits":23,"timeBits":31,"seqBits":9}
```

可支持 `28` 个节点以整体并发量 `14400 UID/s` 的速度持续运行约 `68` 年。

### 场景 2：节点重启频繁、期望长期使用

可增加 `workerBits` 与 `timeBits` 位数，减少 `seqBits` 位数。示例：重启频率为 `24*12 次/天`，配置：

```json
{"workerBits":27,"timeBits":30,"seqBits":6}
```

可支持 `37` 个节点以整体并发量 `2400 UID/s` 的速度持续运行约 `34` 年。
