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

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.cientity.AttrEntityNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPasswordAttrPlaintextApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getName() {
        return "nmcac.getpasswordattrplaintextapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "nmcaa.getattrapi.input.param.desc.id")})
    @Output({@Param(type = ApiParamType.STRING, desc = "nmcac.getpasswordattrplaintextapi.output.param.desc")})
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long attrId = jsonObj.getLong("attrId");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(attrVo.getCiId(), ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        if (!CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityId, GroupType.MAINTAIN).check()) {
            throw new CiAuthException();
        }

        AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrId);
        if (attrEntityVo == null) {
            throw new AttrEntityNotFoundException(attrId);
        }
        if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
            IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrEntityVo.getAttrType());
            handler.transferValueListToDisplay(attrVo, attrEntityVo.getValueList());
            return attrEntityVo.getValueList().getString(0);
        }
        return "";
    }


    @Override
    public String getToken() {
        return "/cmdb/attrentity/getplaintext";
    }
}
