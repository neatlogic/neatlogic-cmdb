/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.service.resourcecenter.resource;

import neatlogic.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppEnvVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityConfigVo;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
public interface IResourceCenterResourceService extends IResourceCenterResourceCrossoverService {

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj);

    /**
     * 处理批量搜索关键字
     * @param resourceSearchVo
     */
    void handleBatchSearchList(ResourceSearchVo resourceSearchVo);

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
     * 补充资产的账号信息
     *
     * @param idList         资产id列表
     * @param resourceVoList 资产列表
     */
    void addResourceAccount(List<Long> idList, List<ResourceVo> resourceVoList);

    /**
     * 补充资产的标签信息
     *
     * @param idList         资产id列表
     * @param resourceVoList 资产列表
     */
    void addResourceTag(List<Long> idList, List<ResourceVo> resourceVoList);


    /**
     * 获取对应模块的应用清单列表
     * 其中清单列表有 系统 存储设备 网络设备 应用实例 应用实例集群 DB实例 DB实例集群 访问入口
     *
     * @param searchVo resourceSearchVo
     * @return tableList
     */
    JSONArray getAppModuleResourceList(ResourceSearchVo searchVo);

    /**
     * 获取对应的资产类型名称
     *
     * @param resourceCiVoList 模型列表
     * @param resourceCiVo     模型
     * @return 模型名称
     */
    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagAndAccountInformation(List<ResourceVo> resourceList);

    /**
     * 获取模块列表
     *
     * @param searchVo resourceSearchVo
     * @return 模块列表
     */
    List<ResourceVo> getAppModuleList(ResourceSearchVo searchVo);


    /**
     * 获取应用巡检批量巡检时的环境列表（环境会包含模块列表，模块还会包含模型列表）
     *
     * @param searchVo resourceSearchVo
     * @return 应用巡检批量巡检时的环境列表
     */
    Collection<AppEnvVo> getAppEnvList(ResourceSearchVo searchVo);

    /**
     * 重建资源中心视图
     * @return
     */
    List<ResourceEntityVo> rebuildResourceEntity();

    /**
     * 构建单个视图
     * @param viewName
     * @param originalConfig
     * @return
     */
    String buildResourceView(String viewName, ResourceEntityConfigVo originalConfig);
}
