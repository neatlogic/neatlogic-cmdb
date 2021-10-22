/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.citype;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiTypeCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/citype/search";
    }

    @Override
    public String getName() {
        return "获取模型类型和模型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, desc = "是否虚拟模型，0：否，1：是"),
            @Param(name = "isAbstract", type = ApiParamType.INTEGER, desc = "是否抽象模型，0：否，1：是")})
    @Output({@Param(explode = CiTypeVo[].class)})
    @Description(desc = "获取模型类型和模型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<CiTypeVo> ciTypeList = ciMapper.searchCiTypeCi(JSONObject.toJavaObject(jsonObj, CiVo.class));
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
