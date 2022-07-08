/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.dto.BasePageVo;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.List;
import java.util.function.BiConsumer;

public interface ResourceCenterCommonGenerateSqlService {

    /**
     * 根据查询条件组装查询资源总个数的PlainSelect对象
     * @param paramObj
     * @return
     */
    PlainSelect getResourceCountPlainSelect(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList);

    /**
     * 根据查询条件组装查询资源总个数的sql语句
     * @param paramObj
     * @return
     */
    String getResourceCountSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList);

    /**
     * 根据查询条件组装查询当前页id列表的sql语句
     * @param paramObj
     * @param mainResourceId
     * @return
     */
    String getResourceIdListSql(String mainResourceId, List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList, int startNum, int pageSize);

    /**
     * 根据查询条件组装查询当前页id列表的sql语句
     * @param plainSelect
     * @return
     */
    String getResourceIdListSql(PlainSelect plainSelect);

    /**
     * 根据查询条件组装查询当前页id列表的sql语句
     * @param plainSelect
     * @param startNum
     * @param pageSize
     * @return
     */
    String getResourceIdListSql(PlainSelect plainSelect, int startNum, int pageSize);

    String getResourceIdSql(List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList);


    String getResourceListSql(PlainSelect plainSelect, List<ResourceInfo> unavailableResourceInfoList, List<ResourceInfo> theadList);

    /**
     * 根据默认查询的列，生成对应的sql语句
     * @param idList
     * @param unavailableResourceInfoList
     * @return
     */
    String getResourceListByIdListSql(List<Long> idList, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 根据需要查询的列，生成对应的sql语句
     * @param theadList
     * @param idList
     * @param unavailableResourceInfoList
     * @return
     */
    String getResourceListByIdListSql(List<ResourceInfo> theadList, List<Long> idList, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 根据默认查询的列，生成对应的sql语句
     * @param idList
     * @param unavailableResourceInfoList
     * @param mainResourceId
     * @return
     */
    String getResourceListByIdListSql(List<Long> idList, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId);

    /**
     * 根据需要查询的列，生成对应的sql语句
     * @param theadList
     * @param idList
     * @param unavailableResourceInfoList
     * @param mainResourceId
     * @return
     */
    String getResourceListByIdListSql(List<ResourceInfo> theadList, List<Long> idList, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId);

    /**
     * 根据需要查询的列和查询条件，生成对应的sql语句执行，返回ResourceVo列表
     * @param biConsumerList
     * @param basePageVo
     * @param unavailableResourceInfoList
     * @param mainResourceId
     * @param theadList
     * @return
     */
    List<ResourceVo> getResourceList(List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList, BasePageVo basePageVo, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId, List<ResourceInfo> theadList);

    /**
     * 根据默认查询的列和查询条件，生成对应的sql语句执行，返回ResourceVo列表
     * @param biConsumerList
     * @param basePageVo
     * @param unavailableResourceInfoList
     * @return
     */
    List<ResourceVo> getResourceList(List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList, BasePageVo basePageVo, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 获取数据初始化配置信息中的视图列表信息
     * @return
     */
    List<ResourceEntityVo> getResourceEntityList();

    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByCommonCondition(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList);

    /**
     * 查询个数
     * @param sql
     * @return
     */
    int getCount(String sql);

    /**
     * 查询id列表
     * @param sql
     * @return
     */
    List<Long> getIdList(String sql);

    /**
     * 查询id
     * @param sql
     * @return
     */
    Long getId(String sql);

    /**
     * 查询资源列表
     * @param sql
     * @return
     */
    List<ResourceVo> getResourceList(String sql);
}
