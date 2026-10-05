# 0008. 混合双驱动客户端生命周期自愈与边界防御加固

## 背景与上下文

在对 `mcp-server-rocketmq` 的控制面与数据面双驱动进行深度全景质量审查时，发现生产与极端网络场景下存在以下潜在韧性隐患：
1. **重连雪崩与并发误杀**：旧版在捕获网络超时后无条件粗暴调用 `mqAdminExt.shutdown()` 重启客户端。若多个请求并发执行，正在执行 RPC 的正常线程会被强行掐断，且在秒级抖动下易引发反复重启的重连风暴；
2. **查空消息误触发重连**：当通过 ID 检索不存在的消息时，服务端返回 206 NOT_FOUND，旧版将其混同于网络异常抛出并触发了底层客户端重启；
3. **Remoting 驱动 ACL 鉴权遗漏**：`RocketmqProperties` 声明了 AK/SK，且 gRPC 客户端挂载了认证，但 `DefaultMQAdminExt` 未挂载 `AclClientRPCHook`，对接鉴权集群时管控接口必然拒绝；
4. **gRPC 客户端单例缺乏断连自愈**：`DefaultMessagingClientService` 持有的 Producer 引用一旦损坏无法自我修复；
5. **边界防御与锁竞争**：缺少 4MB 消息 Payload 防爆保护，Topic 创建在无活跃 Master 时存在假成功，且 `ensureStarted()` 全方法互斥锁制约了并发吞吐。

## 架构决策

我们实施了以下高可用生命周期加固与防御增强决策：

1. **Admin 运维客户端韧性加固**：
   - **重连冷却防抖**：引入 5000ms 冷却时间窗口（`RECONNECT_COOLDOWN_MS = 5000L`），短时间内并发或反复的重连请求自动合并跳过，杜绝惊群风暴；
   - **精确异常语义分离**：在 `doQueryMessageById` 中精细化捕获并区分通信级异常与“消息不存在”业务异常，查空消息统一返回 null 转化为语义清晰的业务拦截，不再误触发网络自愈；
   - **Remoting 协议 ACL 认证补齐**：`createMQAdminExt()` 检测到配置了 AK/SK 时，自动实例化并挂载 `AclClientRPCHook` 与 `SessionCredentials`。
2. **gRPC 消息客户端断连自愈机制**：
   - 增加线程安全的 `reconnect()` 重置方法；在 `sendMessage()` 捕获到底层致命异常时，安全关闭损坏的 Producer 并置空引用，由下一次调用自动触发延迟重建与新通道握手。
3. **输入契约与防爆边界前置防御**：
   - **4MB 消息体防爆**：在 Tool 与 Service 接入层增加 4MB（`MAX_MESSAGE_BODY_BYTES = 4194304`）前置体积强校验，超限直接拦截抛出 `IllegalArgumentException`，防范 OOM；
   - **Topic 创建可用节点强校验**：`createTopic` 前置检查解析出的 Master Broker 集合，若为空集合直接抛出 `IllegalStateException`，杜绝 0 节点更新的假成功；
   - **Message Key 检索区间与数量收敛**：校验 `beginTimestamp <= endTimestamp`，并将 `maxNum` 强制约束在 `[1, 64]` 合法区间内。
4. **锁粒度优化与停机安全**：
   - `ensureStarted()` 升级为 DCL（双重检查锁定）无锁快路径，稳态请求无锁直通，释放虚拟线程并发性能；
   - `destroy()` 标记为 `synchronized`，杜绝优雅停机与重连流程产生竞态。

## 影响与权衡

- **积极影响**：
  - 彻底根除了秒级网络抖动下的惊群重连风暴与并发误杀；
  - 补齐了企业级 ACL 鉴权集群接入能力；
  - 提升了长时间运行容器进程的高可用与自愈能力；
  - 全量 138 项单元测试与集成测试全部绿灯通过。
- **妥协与代价**：
  - 冷却窗口内若连续发生网络故障，第 2 次重试需等待下次调用或冷却时间过期方能触发新一轮连接尝试。
