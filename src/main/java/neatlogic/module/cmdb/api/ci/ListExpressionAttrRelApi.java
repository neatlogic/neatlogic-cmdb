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

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.framework.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListExpressionAttrRelApi extends PrivateApiComponentBase {

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/{ciId}/listexpressionattrrel";
    }

    @Override
    public String getName() {
        return "nmcac.listexpressionattrrelapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid", isRequired = true)})
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "nmcac.listexpressionattrrelapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        JSONArray jsonList = new JSONArray();
        for (AttrVo attrVo : attrList) {
            if (!attrVo.getType().equals("expression")) {
                JSONObject valueObj = new JSONObject();
                valueObj.put("value", "{" + attrVo.getId() + "}");
                valueObj.put("text", attrVo.getLabel());
                jsonList.add(valueObj);
            }
        }

        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        if (CollectionUtils.isNotEmpty(relList)) {
            for (RelVo relVo : relList) {
                List<AttrVo> relAttrList;
                String relName;
                if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                    relAttrList = attrMapper.getAttrByCiId(relVo.getToCiId());
                    relName = relVo.getToLabel();
                } else {
                    relAttrList = attrMapper.getAttrByCiId(relVo.getFromCiId());
                    relName = relVo.getFromLabel();
                }
                if (CollectionUtils.isNotEmpty(relAttrList)) {
                    for (AttrVo attrVo : relAttrList) {
                        if (!attrVo.getType().equals("expression")) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("value", "{" + relVo.getId() + "." + attrVo.getId() + "." + relVo.getDirection() + "}");
                            valueObj.put("text", relName + "->" + attrVo.getLabel());
                            jsonList.add(valueObj);
                        }
                    }
                }
            }

           /* String[] signList = new String[]{":", "-", "_", "(", ")", "[", "]"};
            for (String sign : signList) {
                JSONObject valueObj = new JSONObject();
                valueObj.put("value", sign);
                valueObj.put("text", "分隔符\"" + sign + "\"");
                jsonList.add(valueObj);
            }*/
        }

        return jsonList;
    }
}
