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

package neatlogic.module.cmdb.api.citype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiTypeCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/citype/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchcitypeciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "common.typeid"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcitypeciapi.input.param.desc.typeidlist"),
            @Param(name = "ciNameList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcitypeciapi.input.param.desc.cinamelist"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, desc = "nmcac.searchcitypeciapi.input.param.desc.isvirtual"),
            @Param(name = "isAbstract", type = ApiParamType.INTEGER, desc = "nmcac.searchcitypeciapi.input.param.desc.isabstract")})
    @Output({@Param(explode = CiTypeVo[].class)})
    @Description(desc = "nmcac.searchcitypeciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo pCiVo = JSON.toJavaObject(jsonObj, CiVo.class);
        //查询ciNameList的ciTypeIdList
        JSONArray ciNameList = jsonObj.getJSONArray("ciNameList");
        if (CollectionUtils.isNotEmpty(ciNameList)) {
            List<CiVo> ciListByName = ciMapper.getCiListByNameList(ciNameList.toJavaList(String.class));
            if (CollectionUtils.isNotEmpty(ciListByName)) {
                List<Long> typeIdList = pCiVo.getTypeIdList();
                if (CollectionUtils.isNotEmpty(typeIdList)) {
                    pCiVo.getTypeIdList().addAll(ciListByName.stream().map(CiVo::getTypeId).collect(Collectors.toList()));
                } else {
                    pCiVo.setTypeIdList(ciListByName.stream().map(CiVo::getTypeId).collect(Collectors.toList()));
                }
            }
        }
        List<CiTypeVo> ciTypeList = ciMapper.searchCiTypeCi(pCiVo);
        //如果没有管理权限则需要检查每个模型的权限
        if (!AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY")) {
            for (CiTypeVo ciType : ciTypeList) {
                Iterator<CiVo> itCi = ciType.getCiList().iterator();
                while (itCi.hasNext()) {
                    CiVo ciVo = itCi.next();
                    if (CollectionUtils.isNotEmpty(ciVo.getAuthList())) {
                        if (!CiAuthChecker.hasPrivilege(ciVo.getAuthList(), CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYQUERY)) {
                            if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                                itCi.remove();
                            }
                        }
                    } else {
                        if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                            itCi.remove();
                        }
                    }
                }
            }
        }
        return ciTypeList;
    }
}
