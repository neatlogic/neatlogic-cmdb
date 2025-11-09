/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceTagVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/6/22 15:56
 **/
@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceTagSaveApi extends PrivateApiComponentBase {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;
    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/tag/save";
    }

    @Override
    public String getName() {
        return "保存资源标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, desc = "标签列表")
    })
    @Description(desc = "保存资源标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray tagArray = paramObj.getJSONArray("tagList");
        Long resourceId = paramObj.getLong("resourceId");
        resourceTagMapper.deleteResourceTagByResourceId(resourceId);
        if (CollectionUtils.isEmpty(tagArray)) {
            return null;
        }
        if (resourceCenterResourceService.getResourceIdByResourceId(resourceId) == null) {
            throw new ResourceNotFoundException(resourceId);
        }
        List<String> tagList = tagArray.toJavaList(String.class);
        List<TagVo> existTagList = resourceTagMapper.getTagListByTagNameList(tagList);
        List<Long> tagIdList = existTagList.stream().map(TagVo::getId).collect(Collectors.toList());
        if (tagList.size() > existTagList.size()) {
            List<String> existTagNameList = existTagList.stream().map(TagVo::getName).collect(Collectors.toList());
            tagList.removeAll(existTagNameList);
            for (String tagName : tagList) {
                TagVo tagVo = new TagVo(tagName);
                resourceTagMapper.insertTag(tagVo);
                tagIdList.add(tagVo.getId());
            }
        }
        List<ResourceTagVo> resourceTagVoList = new ArrayList<>();
        for (Long tagId : tagIdList) {
            resourceTagVoList.add(new ResourceTagVo(resourceId, tagId));
            if (resourceTagVoList.size() > 100) {
                resourceTagMapper.insertIgnoreResourceTag(resourceTagVoList);
                resourceTagVoList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            resourceTagMapper.insertIgnoreResourceTag(resourceTagVoList);
        }
        return null;
    }
}
