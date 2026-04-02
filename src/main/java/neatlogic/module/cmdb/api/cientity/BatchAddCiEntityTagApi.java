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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.cientity.CiEntityTagVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.cmdb.exception.tag.CmdbTagNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.tag.CmdbTagMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class BatchAddCiEntityTagApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;
    @Resource
    private CmdbTagMapper cmdbTagMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/tag/batch/add";
    }

    @Override
    public String getName() {
        return "批量添加配置项标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签id列表")
    })
    @Description(desc = "批量添加配置项标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray ciEntityIdArray = paramObj.getJSONArray("ciEntityIdList");
        JSONArray tagIdArray = paramObj.getJSONArray("tagIdList");

        List<Long> ciEntityIdList = ciEntityIdArray.toJavaList(Long.class);
        List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
        if (CollectionUtils.isEmpty(ciEntityList)) {
            throw new CiEntityNotFoundException(ciEntityIdList.get(0));
        }
        Map<Long, CiEntityVo> ciEntityMap = ciEntityList.stream().collect(Collectors.toMap(CiEntityVo::getId, Function.identity()));
        if (ciEntityList.size() != ciEntityIdList.size()) {
            List<Long> existIdList = ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList());
            List<Long> notFoundIdList = ListUtils.removeAll(ciEntityIdList, existIdList);
            throw new CiEntityNotFoundException(notFoundIdList.get(0));
        }
        for (Long ciEntityId : ciEntityIdList) {
            CiEntityVo ciEntityVo = ciEntityMap.get(ciEntityId);
            if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityId, GroupType.MAINTAIN).check()) {
                throw new CiEntityAuthException(ciEntityId, ciEntityVo.getName(), TransactionActionType.UPDATE.getText());
            }
        }

        List<Long> tagIdList = tagIdArray.toJavaList(Long.class);
        List<TagVo> tagList = cmdbTagMapper.getTagListByIdList(tagIdList);
        if (CollectionUtils.isEmpty(tagList)) {
            throw new CmdbTagNotFoundException(tagIdList.get(0));
        }
        if (tagList.size() != tagIdList.size()) {
            List<Long> existTagIdList = tagList.stream().map(TagVo::getId).collect(Collectors.toList());
            List<Long> notFoundTagIdList = ListUtils.removeAll(tagIdList, existTagIdList);
            throw new CmdbTagNotFoundException(notFoundTagIdList);
        }

        List<CiEntityTagVo> ciEntityTagList = new ArrayList<>();
        for (Long ciEntityId : ciEntityIdList) {
            for (Long tagId : tagIdList) {
                ciEntityTagList.add(new CiEntityTagVo(ciEntityId, tagId));
                if (ciEntityTagList.size() > 100) {
                    ciEntityMapper.insertIgnoreCiEntityTag(ciEntityTagList);
                    ciEntityTagList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(ciEntityTagList)) {
            ciEntityMapper.insertIgnoreCiEntityTag(ciEntityTagList);
        }
        return null;
    }
}
