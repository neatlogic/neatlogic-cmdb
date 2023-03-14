中文 / [English](README.en.md)

## 关于

neatlogic-cmdb是配置管理模块，配合neatlogic-autoexec可以实现自动发现和采集，配合neatlogic-itsm支持在流程中修改配置项，配合naetlogic-pbc支持数据自动推送到人民银行。
neatlogic-cmdb采用"万物皆CI"的设计理念，支持把外部数据直接转化成配置项，供cmdb内部消费，例如用户信息、组织架构信息等。

## 主要功能

### 自定义模型

neatlogic-cmdb通过自动生成表的方式实现自定义模型，模型之间支持继承，兼顾单个配置项检索性能和批量出报表性能。
![img.png](README_IMAGES/img.png)

- 支持引用属性、密码、附件、文本、数字、日期时间、表达式等多种属性类型。
  ![img.png](README_IMAGES/img3.png)
- 关系支持双向规则
  ![img.png](README_IMAGES/img2.png)

### 自定义视图

自定义视图让用户有重组数据的能力，用户可通过配置重新组织模型之间的关联关系，或者跳过中间模型，直达目标模型，满足不同的数据消费场景。
![img.png](README_IMAGES/img4.png)

- 可视化数据视图配置。
  ![img.png](README_IMAGES/img5.png)
- 对数据视图进行少量代码开发，可以实现更多个性化展示效果。
  ![img.png](README_IMAGES/img7.png)
- 可视化拓扑视图配置。
  ![img.png](README_IMAGES/img6.png)

### 配置项事务

模仿数据库引入事务设计，配置项修改支持二段提交，支持修改和生效分权处理，支持事务恢复，比较等功能

- 全局事务管理，方便发现最近修改的配置项。
  ![img.png](README_IMAGES/img8.png)
- 配置项修改记录完整展示。
  ![img.png](README_IMAGES/img9.png)

### 拓扑

支持分层、星形等多种自动布局方式。

- 模型拓扑
  ![img.png](README_IMAGES/img10.png)
- 配置项拓扑
  ![img.png](README_IMAGES/img11.png)
- 自定义视图拓扑（和配置项拓扑类似，但关系根据自定义视图生成）

###权限
除了传统的按模型授权以外，还支持通过规则对单个配置项进行授权。
![img.png](README_IMAGES/img12.png)

- 模型权限设置
  ![img.png](README_IMAGES/img13.png)
- 按团体授权
  ![img.png](README_IMAGES/img14.png)

### 其他功能

- 合规检查
- 全局搜索
- 资源中心（和自动化联动）

