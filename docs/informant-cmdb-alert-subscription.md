# Informant 服务告警 Kafka 订阅

## 启用步骤

1. 按正常数据库升级流程执行 `src/main/resources/neatlogic/resources/cmdb/changelog/2026-11-03/neatlogic_tenant.sql`，再部署本次 framework、cmdb-base、cmdb、informant 和前端变更。
2. 确认 Kafka 连接可用，现有 `informant` 主题已启用；主题的注册组筛选同时作用于 Alert、CMDB 等全部订阅。
3. 在消息订阅管理中手动创建一条订阅：消息类型选择 Kafka，主题选择 `informant`，处理器选择“Informant服务告警同步CMDB”（处理器标识 `CMDB_INFORMANT_ALERT_SUBSCRIBE`，实现类 `neatlogic.module.cmdb.mq.subscribe.InformantAlertSubscribe`），使用独立订阅名称并启用，无需额外配置。
4. 保留已有 Alert 订阅。每条订阅以“租户 UUID＋订阅 ID”建立独立消费组，同一条订阅的多个部署实例共用进度；创建不同订阅记录即可让同一消息分别被消费。
5. 可在 CMDB 告警级别管理中新建 `informant` 类型、唯一标识为 `Warn` 或 `Critical` 的级别。名称忽略大小写匹配，未配置时使用活动告警级别 0（不明级别）。

本次没有修改现有租户数据库、创建真实业务订阅或启用业务主题。迁移必须先于对应查询和消费代码上线。

## 消息与处理规则

保留原 `title`、`state`、`ip`、`hostname`、`incident_id`、`informant_id`、`obj_id`、`obj_path`、`category` 等字段，附加：

| 字段 | 含义 |
| --- | --- |
| `source` | 固定 `informant` |
| `alert_uuid` | 对 `["informant", agent_id, obj_id, category, obj_path]` 的 JSON 数组计算 MD5，空身份字段按空字符串处理，结果为 32 位小写字符 |
| `tenant_uuid` | 发布事件所属租户 |
| `cientity_id` | 源端从 `informant_service` 读取的关联快照；空值在 JSON 中可能省略，消费时与 NULL 等同处理 |

CMDB 只处理当前租户且类别为 `services` 的新版消息。旧协议、无效 UUID、格式错误记录原因后跳过。异常且有配置项关联时按 UUID 幂等新增或更新；关联为空时清理该 UUID 原有告警；`Healthy` 按来源及 UUID 精确删除，不依赖当前关联。数字级别 0 不代表恢复。

服务状态使用本次本地事件，远程详情只补充说明；无 Runner、无 hints 或远程查询失败都不阻止服务状态发布。非服务事件保留原推送门槛。UUID 不包含事件编号、状态和配置项 ID，因此重复事件、级别变化和恢复保持相同身份。

旧单条、批量告警接口和巡检清理仅操作无来源、无 UUID 的历史记录，避免干扰订阅告警。查询接口返回 `alertUuid`、`source`；0 级别返回统一多语言名称和中性色。

## 投递边界

Kafka key 使用 `alert_uuid`，同一告警落入同一分区。订阅成功处理、CMDB 数据库事务提交后才确认消费进度；临时异常每 5 秒持续重试，失败分区的后续消息等待该消息成功，其他独立订阅不受影响。永久配置错误需要维护人员修复，格式错误由 CMDB 处理器明确跳过。

保留既有主题启停、注册组过滤和新订阅首次消费位置规则；新订阅不自动回放历史消息。源端仍采用提交后异步发送和日志容错，没有数据库与 Kafka 原子提交、源端失败补偿、全量核对或存量回填。原有源端并发和提交后异步调度不保证严格事件时间顺序，Kafka 只保证同分区实际写入顺序。服务删除、关联修改不额外产生消息，需要后续服务状态事件才能重新校正；本期不向系统、模块穿透。

## 验证

2026-11-03 统一 reactor 构建通过，以下 45 项针对性测试全部成功，真实 Kafka 测试未跳过。

- framework：订阅配置隔离、手动确认、固定重试、兼容带 key 发送；真实 TopicBase 提交后发送和回滚不发送。
- Informant：真实事件保存入口、稳定 UUID、无 Runner/hints 的服务状态和恢复发布、组筛选及 key。
- CMDB：真实 MyBatis XML、Spring 事务与隔离 H2，执行实际迁移 SQL，覆盖重复更新、恢复、空关联、0 级别、秒/毫秒时间戳、租户及历史记录兼容。
- 真实隔离 Kafka：两个独立订阅完整消费，第三订阅加入，失败订阅未提前确认且不阻塞其他订阅，恢复后继续处理。
- 跨模块联调：同一消息进入真实 AlertSubscribe 和 CMDB 订阅；Alert 侧在原始告警队列边界捕获并使用真实 InformantAdapter 检查旧字段，CMDB 侧真实事务和 SQL 落入隔离测试库。未运行 Alert 的实际告警规则和正式数据库。
- 前端告警级别类型改为组件直接读取后端枚举，静态 ESLint 检查通过；没有新增页面样式。

可重复运行的针对性测试从 `neatlogic-build-root` 执行：

```sh
mvn -Pdevelop,commercial -pl ../neatlogic-cmdb,../neatlogic-informant -am \
  -Dtest=KafkaSubscriptionTest,MqSendResultTest,KafkaMultiSubscriptionIntegrationTest,TopicBaseTransactionTest,InformantIncidentPublishTest,InformantIncidentTopicTest,InformantAlertSubscribeTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dkafka.integration.bootstrap=127.0.0.1:19092 test
```

`kafka.integration.bootstrap` 应指向可创建、删除隔离测试主题的代理；未指定时真实 Kafka 测试自动跳过。本工作区完整语言资源扫描有既有的聚合副本和其他模块 owner 目录冲突，因此本次把 framework、cmdb、informant 的 6 个语言文件复制到临时规范目录，再通过同一个运行时校验器验证；没有修改无关模块或跳过本次语言资源检查。

## 修改文件


### neatlogic-framework

- `src/main/java/neatlogic/framework/mq/core/IMqHandler.java`
- `src/main/java/neatlogic/framework/mq/core/TopicBase.java`
- `src/main/java/neatlogic/module/framework/mq/mqhandler/KafkaHandler.java`
- `src/test/java/neatlogic/framework/mq/core/TopicBaseTransactionTest.java`
- `src/test/java/neatlogic/module/framework/mq/mqhandler/KafkaMultiSubscriptionIntegrationTest.java`
- `src/test/java/neatlogic/module/framework/mq/mqhandler/KafkaSubscriptionTest.java`
- `src/test/java/neatlogic/module/framework/mq/mqhandler/MqSendResultTest.java`

### neatlogic-informant

- `src/main/java/neatlogic/module/informant/api/incident/PushInformantIncidentApi.java`
- `src/main/java/neatlogic/module/informant/dao/mapper/InformantMapper.java`
- `src/main/java/neatlogic/module/informant/dao/mapper/InformantMapper.xml`
- `src/main/java/neatlogic/module/informant/mq/InformantIncidentTopic.java`
- `src/main/java/neatlogic/module/informant/service/InformantIncidentPublishService.java`
- `src/main/java/neatlogic/module/informant/service/impl/InformantIncidentPublishServiceImpl.java`
- `src/test/java/neatlogic/module/informant/api/incident/InformantIncidentPublishTest.java`
- `src/test/java/neatlogic/module/informant/mq/InformantIncidentTopicTest.java`

### neatlogic-cmdb-base

- `src/main/java/neatlogic/framework/cmdb/dto/cientity/CiEntityAlertVo.java`
- `src/main/java/neatlogic/framework/cmdb/enums/alertlevel/AlertLevelType.java`

### neatlogic-cmdb

- `docs/informant-cmdb-alert-subscription.md`
- `pom.xml`
- `src/main/java/neatlogic/module/cmdb/api/cientity/SaveAlertLevelApi.java`
- `src/main/java/neatlogic/module/cmdb/dao/mapper/cientity/AlertLevelMapper.xml`
- `src/main/java/neatlogic/module/cmdb/dao/mapper/cientity/CiEntityAlertMapper.java`
- `src/main/java/neatlogic/module/cmdb/dao/mapper/cientity/CiEntityAlertMapper.xml`
- `src/main/java/neatlogic/module/cmdb/mq/subscribe/InformantAlertSubscribe.java`
- `src/main/java/neatlogic/module/cmdb/service/cientity/InformantAlertService.java`
- `src/main/java/neatlogic/module/cmdb/service/cientity/InformantAlertServiceImpl.java`
- `src/main/resources/neatlogic/resources/cmdb/changelog/2026-11-03/neatlogic_tenant.sql`
- `src/main/resources/neatlogic/resources/cmdb/i18n/language_en.json`
- `src/main/resources/neatlogic/resources/cmdb/i18n/language_zh.json`
- `src/main/resources/neatlogic/resources/cmdb/sqldefine/tables/cmdb_cientity_alert-en.json`
- `src/main/resources/neatlogic/resources/cmdb/sqldefine/tables/cmdb_cientity_alert.json`
- `src/test/java/neatlogic/module/cmdb/mq/subscribe/InformantAlertSubscribeTest.java`

### neatlogic-web

- `src/views/pages/cmdb/alertlevel/alertlevel-edit-dialog.vue`
