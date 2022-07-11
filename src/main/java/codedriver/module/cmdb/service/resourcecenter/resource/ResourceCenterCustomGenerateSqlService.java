/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.List;
import java.util.function.BiConsumer;

public interface ResourceCenterCustomGenerateSqlService {

    /**
     * 资产清单中协议过滤条件
     * @param protocolIdList 协议id列表
     * @param unavailableResourceInfoList 资源中心数据初始化配置中没有定义的字段集合
     * @return
     */
    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByProtocolIdList(List<Long> protocolIdList, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 资产清单中标签过滤条件
     * @param tagIdList 标签id列表
     * @param unavailableResourceInfoList 资源中心数据初始化配置中没有定义的字段集合
     * @return
     */
    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByTagIdList(List<Long> tagIdList, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 资产清单中关键字过滤条件
     * @param keyword 关键字
     * @param unavailableResourceInfoList 资源中心数据初始化配置中没有定义的字段集合
     * @return
     */
    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByKeyword(String keyword, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 资产清单中端口过滤条件
     * @param port 端口
     * @param unavailableResourceInfoList 资源中心数据初始化配置中没有定义的字段集合
     * @return
     */
    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByPort(String port, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 资产清单中需要显示的字段列表
     * @return
     */
    List<ResourceInfo> getTheadList();

    /**
     * 资产清单中常用的过滤条件，类型、状态、环境、系统、模块、巡检状态、名称、IP地址
     * @param paramObj 数据集合
     * @param unavailableResourceInfoList 资源中心数据初始化配置中没有定义的字段集合
     * @return
     */
    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByCommonCondition(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList);

}
