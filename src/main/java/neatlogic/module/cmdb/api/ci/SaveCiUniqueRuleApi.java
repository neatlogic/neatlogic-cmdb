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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.exception.attr.AttrNotAllowBeUniqueException;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiUniqueRuleApi extends PrivateApiComponentBase {

    @Resource
    private CiService ciService;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/ciunique/save";
    }

    @Override
    public String getName() {
        return "nmcac.saveciuniqueruleapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "attrIdList", type = ApiParamType.JSONARRAY,
                    desc = "nmcaa.getattrlistapi.input.param.desc.idlist")})
    @Description(desc = "nmcac.saveciuniqueruleapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
            throw new CiAuthException();
        }
        JSONArray pAttrList = jsonObj.getJSONArray("attrIdList");
        List<Long> attrIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(pAttrList)) {
            for (int i = 0; i < pAttrList.size(); i++) {
                attrIdList.add(pAttrList.getLong(i));
            }
            List<AttrVo> attrList = attrMapper.getAttrByIdList(attrIdList);
            for (Long attrId : attrIdList) {
                if (attrList.stream().noneMatch(d -> d.getId().equals(attrId))) {
                    throw new AttrNotFoundException(attrId);
                }
            }
            for(AttrVo attr : attrList) {
                if(!attr.getAllowBeUnique()){
                    throw new AttrNotAllowBeUniqueException(attr);
                }
            }
        }
        ciService.updateCiUnique(ciId, attrIdList);
        return null;
    }

}
