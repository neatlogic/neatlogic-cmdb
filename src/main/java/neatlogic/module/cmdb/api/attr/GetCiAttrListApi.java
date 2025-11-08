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

package neatlogic.module.cmdb.api.attr;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiAttrListApi extends PrivateApiComponentBase {

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/listattr";
    }

    @Override
    public String getName() {
        return "nmcaa.getciattrlistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "term.cmdb.ciname"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "common.displaytype"),
            @Param(name = "allowEdit", type = ApiParamType.INTEGER, rule = "1,0", desc = "term.cmdb.allowedit"),
            @Param(name = "isRequired", type = ApiParamType.INTEGER, rule = "1,0", desc = "common.isrequired"),
            @Param(name = "isSimple", type = ApiParamType.BOOLEAN, rule = "true,false", desc = "term.cmdb.issimpleattribute"),
            @Param(name = "isNeedTargetCi", type = ApiParamType.BOOLEAN, rule = "true,false", desc = "是否有目标模型"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue"),
            @Param(name = "needAlias", type = ApiParamType.INTEGER, desc = "term.cmdb.needalias", rule = "0,1"),
            @Param(name = "mergeAlias", type = ApiParamType.INTEGER, defaultValue = "1", desc = "term.cmdb.mergealias", rule = "0,1")
    })
    @Output({
            @Param(name = "Return", explode = AttrVo[].class)
    })
    @Description(desc = "nmcaa.getciattrlistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        int needAlias = jsonObj.getIntValue("needAlias");
        int mergeAlias = jsonObj.getInteger("mergeAlias");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            return attrMapper.getAttrByIdList(idList);
        }
        Long ciId = jsonObj.getLong("ciId");
        if (ciId == null) {
            String ciName = jsonObj.getString("ciName");
            if (StringUtils.isBlank(ciName)) {
                throw new ParamNotExistsException("ciId", "ciName");
            }
            CiVo ciVo = ciMapper.getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
            ciId = ciVo.getId();
        }
        String showType = jsonObj.getString("showType");
        Boolean isSimple = jsonObj.getBoolean("isSimple");
        Boolean isNeedTargetCi = jsonObj.getBoolean("isNeedTargetCi");
        Integer allowEdit = jsonObj.getInteger("allowEdit");
        Integer isRequired = jsonObj.getInteger("isRequired");
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        List<CiViewVo> ciViewList;
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> attrSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().equals("attr")) {
                    attrSet.add(ciView.getItemId());
                }
            }
            attrList.removeIf(attr -> !attrSet.contains(attr.getId()));
        } else {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        }
        if (needAlias == 1 && CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo ciView : ciViewList) {
                if (StringUtils.isNotBlank(ciView.getAlias()) && ciView.getType().equals("attr")) {
                    Optional<AttrVo> op = attrList.stream().filter(d -> d.getId().equals(ciView.getItemId())).findFirst();
                    if (mergeAlias == 1) {
                        op.ifPresent(attrVo -> attrVo.setLabel(ciView.getAlias()));
                    } else {
                        op.ifPresent(attrVo -> attrVo.setAlias(ciView.getAlias()));
                    }
                }
            }
        }
        if (allowEdit != null) {
            attrList.removeIf(attr -> (allowEdit.equals(1) && (attr.getAllowEdit() != null && attr.getAllowEdit().equals(0)))
                    || (allowEdit.equals(0) && (attr.getAllowEdit() == null || attr.getAllowEdit().equals(1))));
        }
        if (isSimple != null) {
            attrList.removeIf(attr -> AttrValueHandlerFactory.getHandler(attr.getType()).isSimple() != isSimple);
        }
        if (isNeedTargetCi != null) {
            attrList.removeIf(attr -> AttrValueHandlerFactory.getHandler(attr.getType()).isNeedTargetCi() != isNeedTargetCi);
        }
        if (isRequired != null && isRequired.equals(1)) {
            //唯一规则和必填等价
            attrList.removeIf(attr -> (attr.getIsRequired() != null && attr.getIsRequired().equals(0) && attr.getIsCiUnique().equals(0)));
        }
        return attrList;
    }
}
