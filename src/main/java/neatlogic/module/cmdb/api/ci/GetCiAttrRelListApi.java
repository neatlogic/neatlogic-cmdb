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
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.framework.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetCiAttrRelListApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/attrrellist";
    }

    @Override
    public String getName() {
        return "nmcac.getciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "term.cmdb.ciidlist")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "nmcac.getciattrrellistapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciIds = jsonObj.getJSONArray("ciIdList");
        List<Long> ciIdList = new ArrayList<>();
        for (int i = 0; i < ciIds.size(); i++) {
            ciIdList.add(ciIds.getLong(i));
        }
        JSONArray ciObjList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciIdList)) {
            List<CiVo> ciList = ciMapper.getCiByIdList(ciIdList);
            if (CollectionUtils.isNotEmpty(ciList)) {
                for (CiVo ciVo : ciList) {
                    JSONObject ciObj = new JSONObject();
                    ciObj.put("id", ciVo.getId());
                    ciObj.put("name", ciVo.getName());
                    ciObj.put("label", ciVo.getLabel());
                    List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                    List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciVo.getId()));
                    JSONArray attrObjList = new JSONArray();
                    for (AttrVo attrVo : attrList) {
                        JSONObject attrObj = new JSONObject();
                        attrObj.put("id", attrVo.getId());
                        attrObj.put("name", attrVo.getName());
                        attrObj.put("label", attrVo.getLabel());
                        attrObjList.add(attrObj);
                    }
                    ciObj.put("attrList", attrObjList);
                    JSONArray relObjList = new JSONArray();
                    for (RelVo relVo : relList) {
                        JSONObject relObj = new JSONObject();
                        relObj.put("id", relVo.getId());
                        if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                            relObj.put("name", relVo.getToCiName());
                            relObj.put("label", relVo.getToCiLabel());
                        } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                            relObj.put("name", relVo.getFromCiName());
                            relObj.put("label", relVo.getFromCiLabel());
                        }
                        relObjList.add(relObj);
                    }
                    ciObj.put("relList", relObjList);
                    ciObjList.add(ciObj);
                }
            }
        }
        return ciObjList;
    }
}
