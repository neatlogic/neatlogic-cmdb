/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.cmdb.api.resourcecenter.resourcetype;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.annotation.ResourceType;
import neatlogic.framework.cmdb.annotation.ResourceTypes;
import neatlogic.framework.cmdb.crossover.IResourceTypeTreeApiCrossoverService;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询资源类型树列表接口
 *
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceTypeTreeApi extends PrivateApiComponentBase implements IResourceTypeTreeApiCrossoverService {

    private static List<ResourceTypeVo> resourceTypeList = new ArrayList<>();

    static {
        Reflections reflections = new Reflections("neatlogic.framework.cmdb.dto.resourcecenter.entity", new TypeAnnotationsScanner(), new SubTypesScanner());
        Set<Class<?>> classList = reflections.getTypesAnnotatedWith(ResourceType.class, true);
        for (Class<?> c : classList) {
            ResourceType resourceType = c.getAnnotation(ResourceType.class);
            if (resourceType != null) {
                String ciName = resourceType.ciName();
                if (StringUtils.isNotBlank(ciName)) {
                    resourceTypeList.add(new ResourceTypeVo(resourceType.label(), ciName));
                }
            }
        }
        classList = reflections.getTypesAnnotatedWith(ResourceTypes.class, true);
        for (Class<?> c : classList) {
            ResourceTypes resourceTypes = c.getAnnotation(ResourceTypes.class);
            if (resourceTypes != null) {
                for (ResourceType resourceType : resourceTypes.value()) {
                    String ciName = resourceType.ciName();
                    if (StringUtils.isNotBlank(ciName)) {
                        resourceTypeList.add(new ResourceTypeVo(resourceType.label(), ciName));
                    }
                }
            }
        }
    }

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourcetype/tree";
    }

    @Override
    public String getName() {
        return "查询资源类型树列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "名称模糊匹配")
    })
    @Output({
            @Param(explode = ResourceTypeVo[].class, desc = "资源类型树列表")
    })
    @Description(desc = "查询资源类型树列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String keyword = jsonObj.getString("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase();
        }
        List<ResourceTypeVo> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(resourceTypeList)) {
            List<CiVo> ciVoList = new ArrayList<>();
            for (ResourceTypeVo type : resourceTypeList) {
                CiVo ciVo = ciMapper.getCiByName(type.getName());
                if (ciVo == null) {
                    throw new CiNotFoundException(type.getName());
                }
                ciVoList.add(ciVo);
            }
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            for (CiVo ciVo : ciVoList) {
                List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                int size = ciList.size();
                List<ResourceTypeVo> resourceTypeVoList = new ArrayList<>(size);
                Map<Long, ResourceTypeVo> resourceTypeMap = new HashMap<>(size);
                for (CiVo ci : ciList) {
                    ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ci.getId(), ci.getParentCiId(), ci.getLabel(), ci.getName());
                    resourceTypeMap.put(resourceTypeVo.getId(), resourceTypeVo);
                    resourceTypeVoList.add(resourceTypeVo);
                }
                if (StringUtils.isNotBlank(keyword)) {
                    // 建立父子关系
                    for (ResourceTypeVo resourceType : resourceTypeVoList) {
                        if (resourceType.getParentId() != null) {
                            ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                            if (parentResourceType != null) {
                                resourceType.setParent(parentResourceType);
                                parentResourceType.addChild(resourceType);
                            }
                        }
                    }
                    // 判断节点名称是否与关键字keyword匹配，如果匹配就将该节点及其父子节点的isKeywordMatch字段值设置为1，否则设置为0。
                    for (ResourceTypeVo resourceType : resourceTypeVoList) {
                        if (resourceType.getLabel().toLowerCase().contains(keyword)) {
                            if (resourceType.getIsKeywordMatch() == null) {
                                resourceType.setIsKeywordMatch(1);
                                resourceType.setUpwardIsKeywordMatch(1);
                                resourceType.setDownwardIsKeywordMatch(1);
                            }
                        } else {
                            if (resourceType.getIsKeywordMatch() == null) {
                                resourceType.setIsKeywordMatch(0);
                            }
                        }
                    }
                    // 将isKeywordMatch字段值为0的节点从其父级中移除。
                    Iterator<ResourceTypeVo> iterator = resourceTypeVoList.iterator();
                    while (iterator.hasNext()) {
                        ResourceTypeVo resourceType = iterator.next();
                        if (Objects.equals(resourceType.getIsKeywordMatch(), 0)) {
                            ResourceTypeVo parent = resourceType.getParent();
                            if (parent != null) {
                                parent.removeChild(resourceType);
                            }
                            iterator.remove();
                        }
                    }
                } else {
                    for (ResourceTypeVo resourceType : resourceTypeVoList) {
                        if (resourceType.getParentId() != null) {
                            ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                            if (parentResourceType != null) {
                                parentResourceType.addChild(resourceType);
                            }
                        }
                    }
                }
                for (ResourceTypeVo resourceType : resourceTypeVoList) {
                    if (resourceType.getParentId() != null) {
                        ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
                        if (parentResourceType == null) {
                            resultList.add(resourceType);
                        }
                    }
                }
            }
        }
        return resultList;
    }

    //用于巡检工具列表的显示
    public List<ResourceTypeVo> getResourceTypeList() {
        return resourceTypeList;
    }
}
