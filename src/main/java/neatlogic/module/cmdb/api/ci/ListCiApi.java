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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/list";
    }

    @Override
    public String getName() {
        return "nmcac.listciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.ciidlist"),
            @Param(name = "isRaw", type = ApiParamType.INTEGER, desc = "是否返回原始数据格式", rule = "0,1"),
            @Param(name = "excludeCiIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.excludeciid"),
            @Param(name = "isAbstract", type = ApiParamType.ENUM, rule = "0,1", desc = "term.cmdb.isabstractci"),
            @Param(name = "isVirtual", type = ApiParamType.ENUM, rule = "0,1", desc = "term.cmdb.isvirtualci"),
            @Param(name = "needChildren", type = ApiParamType.INTEGER, rule = "0,1", desc = "如果提供的id是父节点，是否返回他的所有子节点")
    })
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "nmcac.listciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        JSONArray excludeCiIdList = jsonObj.getJSONArray("excludeCiIdList");
        List<Long> ciIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                try {
                    ciIdList.add(idList.getLong(i));
                } catch (Exception ignored) {

                }
            }
        }
        List<CiVo> ciList = ciMapper.getAllCi(ciIdList);
        JSONArray jsonList = new JSONArray();
        Integer isAbstract = jsonObj.getInteger("isAbstract");
        Integer isVirtual = jsonObj.getInteger("isVirtual");
        Integer isRaw = jsonObj.getInteger("isRaw");
        Integer needChildren = jsonObj.getInteger("needChildren");
        List<CiVo> finalCiList = new ArrayList<>();
        if (needChildren != null && needChildren.equals(1)) {
            for (CiVo ciVo : ciList) {
                if (ciVo.getIsAbstract().equals(1)) {
                    List<CiVo> childCIList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    finalCiList.addAll(childCIList);
                } else {
                    finalCiList.add(ciVo);
                }
            }
        } else {
            finalCiList = ciList;
        }
        for (CiVo ciVo : finalCiList) {
            if (CollectionUtils.isNotEmpty(excludeCiIdList) && (excludeCiIdList.contains(ciVo.getId()))) {
                continue;
            }
            if (isAbstract != null && !Objects.equals(isAbstract, ciVo.getIsAbstract())) {
                continue;
            }
            if (isVirtual != null && !Objects.equals(isVirtual, ciVo.getIsVirtual())) {
                continue;
            }
            JSONObject valueObj = new JSONObject();
            if (isRaw == null || isRaw.equals(0)) {
                valueObj.put("value", ciVo.getId());
                valueObj.put("text", ciVo.getLabel() + "(" + ciVo.getName() + ")");
            } else if (isRaw.equals(1)) {
                valueObj.put("id", ciVo.getId());
                valueObj.put("name", ciVo.getName());
                valueObj.put("label", ciVo.getLabel());
            }
            jsonList.add(valueObj);
        }
        return jsonList;
    }
}
