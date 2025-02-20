/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.resourcecenter.datasource.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ApplicationListDisplayVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.Ordered;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class DefaultResourceCenterDataSourceImpl implements IResourceCenterDataSource {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private ResourceMapper resourceMapper;

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public Ordered getOrdered() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public JSONArray getAppResourceList(Long appSystemId, Long appModuleId, Long envId, List<Long> resourceTypeIdList, Integer currentPage, Integer pageSize) {
        JSONArray tableList = new JSONArray();
        ApplicationListDisplayVo applicationListDisplay = resourceEntityMapper.getApplicationListDisplay();
        if (applicationListDisplay != null) {
            JSONObject config = applicationListDisplay.getConfig();
            if (MapUtils.isNotEmpty(config)) {
                JSONArray tableArray = config.getJSONArray("tableList");
                if (CollectionUtils.isNotEmpty(tableArray)) {
                    List<String> ciNameList = new ArrayList<>();
                    Map<String, JSONArray> ciName2TheadListMap = new HashMap<>();
                    for (int i = 0; i < tableArray.size(); i++) {
                        JSONObject tableObj = tableArray.getJSONObject(i);
                        if (MapUtils.isNotEmpty(tableObj)) {
                            String ciName = tableObj.getString("ciName");
                            JSONArray theadList = tableObj.getJSONArray("theadList");
                            ciName2TheadListMap.put(ciName, theadList);
                            ciNameList.add(ciName);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(ciNameList)) {
                        List<CiVo> resourceCiVoList = ciMapper.getCiListByNameList(ciNameList);
                        if (CollectionUtils.isNotEmpty(resourceTypeIdList)) {
                            ResourceSearchVo searchVo = new ResourceSearchVo();
                            searchVo.setAppSystemIdList(Collections.singletonList(appSystemId));
                            if (appModuleId != null) {
                                searchVo.setAppModuleIdList(Collections.singletonList(appModuleId));
                            }
                            if (envId != null) {
                                searchVo.setEnvIdList(Collections.singletonList(envId));
                            }
                            searchVo.setCurrentPage(currentPage);
                            searchVo.setPageSize(pageSize);
                            List<CiVo> ciList = ciMapper.getAllCi(resourceTypeIdList);
                            for (CiVo ciVo : ciList) {
                                String resourceTypeName = getResourceTypeName(resourceCiVoList, ciVo);
                                if (StringUtils.isNotBlank(resourceTypeName)) {
                                    ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ciVo.getId(), ciVo.getParentCiId(), ciVo.getLabel(), ciVo.getName());
                                    searchVo.setTypeIdList(Collections.singletonList(ciVo.getId()));
                                    resourceCenterResourceService.assembleResourceSearchVo(searchVo, false);
                                    List<ResourceVo> resourceList = getResourceList(searchVo);
                                    if (CollectionUtils.isNotEmpty(resourceList)) {
                                        JSONObject tableObj = TableResultUtil.getResult(ciName2TheadListMap.get(resourceTypeName), resourceList, searchVo);
                                        tableObj.put("type", resourceTypeVo);
                                        tableList.add(tableObj);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return tableList;
    }

    @Override
    public List<ResourceVo> getResourceList(ResourceSearchVo searchVo) {
        List<ResourceVo> resourceList = new ArrayList<>();
        List<Long> idList = resourceMapper.getResourceIdList(searchVo);
        if (CollectionUtils.isNotEmpty(idList)) {
            resourceList = resourceMapper.getResourceListByIdList(idList);
            if (Objects.equals(searchVo.getRowNum(), 0)) {
                int rowNum = 0;
                if (noFilterCondition(searchVo)) {
                    rowNum = resourceMapper.getAllResourceCount(searchVo);
                } else {
                    rowNum = resourceMapper.getResourceCount(searchVo);
                }
                searchVo.setRowNum(rowNum);
            }
        }
        return resourceList;
    }

    /**
     * 判断是否有过滤条件
     * @param searchVo
     * @return
     */
    private boolean noFilterCondition(ResourceSearchVo searchVo) {
        if (StringUtils.isNotBlank(searchVo.getKeyword())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getBatchSearchList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getStateIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getVendorIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getEnvIdList())) {
            return false;
        }
        if (searchVo.getExistNoEnv()) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getAppSystemIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getAppModuleIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getDefaultValue())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getInspectStatusList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getProtocolIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(searchVo.getInspectJobPhaseNodeStatusList())) {
            return false;
        }
        return true;
    }

    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo) {
        for (CiVo ciVo : resourceCiVoList) {
            if (ciVo.getLft() <= resourceCiVo.getLft() && ciVo.getRht() >= resourceCiVo.getRht()) {
                return ciVo.getName();
            }
        }
        return null;
    }
}
