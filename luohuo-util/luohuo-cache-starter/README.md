# luohuo-cache-starter

将 **分布式缓存 Redis** 与 **本地缓存 Caffeine** 的常用能力做统一抽象，业务侧通常只需注入 `CacheOps` 即可在两种缓存实现间切换。

> [!NOTE]
> 抽象后会有能力边界：Redis 的一些特有结构与操作（例如 `list` / `set` / `hash`）无法完全用统一接口覆盖。

## 要用 Redis 特有方法怎么办

当配置 `luohuo.cache.type=REDIS` 后，你可以按需注入：

- **`RedisOps`**：直接使用 Redis 能力（要求环境必须依赖 Redis，否则启动可能失败）
- **`CachePlusOps`**：对 Caffeine 不支持的能力做空实现，保证在无 Redis 环境下也能启动

## 为啥要抽象

- **降低演示/小项目部署成本**：部分项目只需要缓存能力，不希望额外部署 Redis
- **提升开发体验**：开发机资源有限时，少启动一个中间件更流畅

## 如何切换缓存实现（Redis / Caffeine）

### 1. 使用 Redis

`pom.xml`：

```xml
<dependency>
  <groupId>com.luohuo.basic</groupId>
  <artifactId>luohuo-cache-starter</artifactId>
</dependency>
```

`application.yml`：

```yaml
luohuo:
  redis:
    ip: 127.0.0.1
    port: 16379
  cache:
    type: REDIS
```

### 2. 使用 Caffeine

`pom.xml`（排除 Redis 依赖）：

```xml
<dependency>
  <groupId>com.luohuo.basic</groupId>
  <artifactId>luohuo-cache-starter</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

`application.yml`：

```yaml
luohuo:
  cache:
    type: CAFFEINE
```
