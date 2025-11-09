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
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/6/22 15:57
 **/
@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceTagBatchAddApi extends PrivateApiComponentBase {

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;
    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/tag/batch/add";
    }

    @Override
    public String getName() {
        return "批量添加资源标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "资源id列表"),
            @Param(name = "tagList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签列表")
    })
    @Description(desc = "批量添加资源标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resourceIdArray = paramObj.getJSONArray("resourceIdList");
        if (CollectionUtils.isEmpty(resourceIdArray)) {
            throw new ParamNotExistsException("resourceIdList");
        }
        JSONArray tagArray = paramObj.getJSONArray("tagList");
        if (CollectionUtils.isEmpty(tagArray)) {
            throw new ParamNotExistsException("tagList");
        }

        List<Long> resourceIdList = resourceIdArray.toJavaList(Long.class);
        List<Long> existResourceIdList = resourceCenterResourceService.checkResourceIdListIsExists(resourceIdList);
        if (resourceIdList.size() > existResourceIdList.size()) {
            List<Long> notFoundIdList = ListUtils.removeAll(resourceIdList, existResourceIdList);
            if (CollectionUtils.isNotEmpty(notFoundIdList)) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Long resourceId : notFoundIdList) {
                    stringBuilder.append(resourceId);
                    stringBuilder.append("、");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                throw new ResourceNotFoundException(stringBuilder.toString());
            }
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
        for (Long resourceId : resourceIdList) {
            for (Long tagId : tagIdList) {
                resourceTagVoList.add(new ResourceTagVo(resourceId, tagId));
                if (resourceTagVoList.size() > 100) {
                    resourceTagMapper.insertIgnoreResourceTag(resourceTagVoList);
                    resourceTagVoList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            resourceTagMapper.insertIgnoreResourceTag(resourceTagVoList);
        }
        return null;
    }
}
