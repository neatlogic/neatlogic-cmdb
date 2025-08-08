/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.service.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountComponentVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;

import java.util.List;
import java.util.Map;


/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
public interface IResourceCenterResourceService extends IResourceCenterResourceCrossoverService {

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj);

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj,boolean isIncludeSon);

    void assembleResourceSearchVo(ResourceSearchVo searchVo, boolean isIncludeSon);

    /**
     * 处理批量搜索关键字
     * @param resourceSearchVo
     */
    void handleBatchSearchList(ResourceSearchVo resourceSearchVo);

    /**
     * 设置ipFieldAttrId字段和nameFieldAttrId字段
     * @param resourceSearchVo
     */
    void setIpFieldAttrIdAndNameFieldAttrId(ResourceSearchVo resourceSearchVo);

    /**
     * 设置isIpFieldSort字段和isNameFieldSort字段
     * @param resourceSearchVo
     */
    void setIsIpFieldSortAndIsNameFieldSort(ResourceSearchVo resourceSearchVo);

    /**
     * 查询所有后代模型的id列表
     *
     * @param idList
     * @return
     */
    List<Long> getDownwardCiIdListByCiIdList(List<Long> idList);

    /**
     * 获取资产的账号信息
     *
     * @param idList
     */
    Map<Long, List<AccountVo>> getResourceAccountByResourceIdList(List<Long> idList);

    /**
     * 获取资产的标签信息
     *
     * @param idList 资产id列表
     * @return map<资产id ， 标签列表>
     */
    Map<Long, List<TagVo>> getResourceTagByResourceIdList(List<Long> idList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagAndAccountInformation(List<ResourceVo> resourceList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagInformation(List<ResourceVo> resourceList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addAccountInformation(List<ResourceVo> resourceList);

    /**
     * 构建单个视图
     * @param resourceEntityVo
     * @return
     */
    String buildResourceView(ResourceEntityVo resourceEntityVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceIdList(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetResourceIdListSql(ResourceSearchVo searchVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceCount(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetResourceCountSql(ResourceSearchVo searchVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceListByIdList(java.util.List)}
     * @param idList
     * @param selectFieldNameList
     * @return
     */
    String buildGetResourceListSql(List<Long> idList, List<String> selectFieldNameList);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceListByIdList(java.util.List)}
     * @param idList
     * @return
     */
    String buildGetResourceListSql(List<Long> idList);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceCountByNameKeyword(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetResourceCountByNameKeywordSql(ResourceSearchVo searchVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceCountByIpKeyword(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetResourceCountByIpKeywordSql(ResourceSearchVo searchVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getAuthResourceList(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetAuthResourceListSql(ResourceSearchVo searchVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceListByIpAndPortAndNameWithFilter(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetResourceListByIpAndPortAndNameWithFilterSql(ResourceSearchVo searchVo);

    /**
     * 生成SQL等效于{@link neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper#getResourceTypeIdListByAuth(neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo)}
     * @param searchVo
     * @return
     */
    String buildGetResourceTypeIdListByAuthSql(ResourceSearchVo searchVo);

    String buildGetResourceIdByIpAndPortAndNameSql(ResourceSearchVo searchVo);
    String buildGetResourceIdListByIpAndPortAndNameSql(ResourceSearchVo searchVo);
    String buildGetResourceListByIpAndPortAndNameSql(ResourceSearchVo searchVo);
    String buildGetResourceByIdListSql(List<Long> idList);
    String buildGetResourceByIdSql(Long id, List<String> selectFieldNameList);
    String buildGetResourceByIdSql(Long id);
    String buildGetResourceIdByResourceIdSql(Long id);
    String buildCheckResourceIdListIsExistsSql(List<Long> idList);
//    String buildGetResourceListByResourceVoListSql();
    String buildGetResourceIdListByAppSystemIdAndModuleIdAndEnvIdSql(ResourceVo resourceVo);
    String buildGetResourceListByTypeIdListAndIpListSql(List<Long> typeIdList, List<String> ipList);
    String buildGetResourceByIpAndPortAndNameAndTypeNameSql(String ip, Integer port, String name, String typeName);
    String buildGetResourceByIpAndPortSql(String ip, Integer port);
    String buildSearchAccountComponentSql(AccountComponentVo accountComponentVo);
    String buildSearchAccountComponentCountSql(AccountComponentVo accountComponentVo);
    String buildGetAppEnvListByAppSystemIdAndAppModuleIdSql(Long appSystemId, Long appModuleId);
    String buildGetAppEnvCountMapByAppSystemIdGroupByAppModuleIdSql(Long appSystemId);
//    String buildGetResourceCountByDynamicConditionSql();
//    String buildGetResourceIdListByDynamicConditionSql();

    // InspectMapper
//    String buildgetInspectResourceListByIdListSql();
//    String buildgetInspectResourceCountSql();
//    String buildgetInspectResourceCountByIpKeywordSql();
//    String buildgetInspectResourceCountByNameKeywordSql();
//    String buildgetInspectResourceIdListSql();
//    String buildgetInspectAutoexecJobNodeResourceCountSql();
//    String buildgetInspectAutoexecJobNodeResourceCountByIpKeywordSql();
//    String buildgetInspectAutoexecJobNodeResourceCountByNameKeywordSql();
//    String buildgetInspectAutoexecJobNodeResourceIdListSql();
//    String buildgetInspectResourceListByIdListAndJobIdSql();
    // InspectConfigFileMapper
//    String buildgetInspectResourceCountSql();
//    String buildgetInspectResourceIdListSql();
//    String buildgetInspectResourceListByIdListSql();
//    String buildgetInspectConfigFilePathCountSql();
//    String buildgetInspectConfigFilePathIdListSql();
//    String buildgetInspectConfigFilePathListSql();
//    String buildgetInspectConfigFilePathListByJobIdSql();

}
