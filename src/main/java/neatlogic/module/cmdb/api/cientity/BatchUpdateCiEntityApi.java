/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchUpdateCiEntityApi extends PrivateApiComponentBase {
    //static Logger logger = LoggerFactory.getLogger(BatchSaveCiEntityApi.class);

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchupdate";
    }

    @Override
    public String getName() {
        return "nmcac.batchupdatecientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ciEntityIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "nmcac.batchupdatecientityapi.input.param.desc.cientityidlist"),
            @Param(name = "attrEntityData", type = ApiParamType.JSONOBJECT, desc = "nmcac.batchupdatecientityapi.input.param.desc.attrentitydata"),
            @Param(name = "globalAttrEntityData", type = ApiParamType.JSONOBJECT, desc = "nmcac.batchupdatecientityapi.input.param.desc.globalattrentitydata"),
            @Param(name = "relEntityData", type = ApiParamType.JSONOBJECT, desc = "nmcac.batchupdatecientityapi.input.param.desc.relentitydata"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, desc = "term.cmdb.iscommit"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.memo")})
    @Description(desc = "nmcac.batchupdatecientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).check()) {
            throw new CiEntityAuthException(ciVo.getLabel(), TransactionActionType.UPDATE.getText());
        }

        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        if (needCommit) {
            if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).check()) {
                needCommit = false;
            }
        }

        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
        if (MapUtils.isNotEmpty(attrObj)) {
            for (String key : attrObj.keySet()) {
                JSONObject obj = attrObj.getJSONObject(key);
                JSONArray valueList = obj.getJSONArray("valueList");
                obj.remove("actualValueList");
                if (CollectionUtils.isNotEmpty(valueList)) {
                    for (int i = valueList.size() - 1; i >= 0; i--) {
                        if (valueList.get(i) instanceof JSONObject) {
                            JSONObject valueObj = valueList.getJSONObject(i);
                            Long attrCiEntityId = valueObj.getLong("id");
                            if (attrCiEntityId != null) {
                                valueList.set(i, attrCiEntityId);
                            } else {
                                valueList.remove(i);
                            }
                        }
                    }
                }
            }
        }


        JSONArray ciEntityIdList = jsonObj.getJSONArray("ciEntityIdList");
        String description = jsonObj.getString("description");
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            for (int i = 0; i < ciEntityIdList.size(); i++) {
                Long ciEntityId = ciEntityIdList.getLong(i);
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(ciEntityId);
                ciEntityTransactionVo.setCiId(ciId);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                ciEntityTransactionVo.setAllowCommit(needCommit);
                ciEntityTransactionVo.setDescription(description);
                ciEntityTransactionVo.setGlobalAttrEntityData(JSONObject.parseObject(jsonObj.getString("globalAttrEntityData")));
                ciEntityTransactionVo.setAttrEntityData(JSONObject.parseObject(jsonObj.getString("attrEntityData")));
                ciEntityTransactionVo.setRelEntityData(JSONObject.parseObject(jsonObj.getString("relEntityData")));
                ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        }
        return null;
    }

}
