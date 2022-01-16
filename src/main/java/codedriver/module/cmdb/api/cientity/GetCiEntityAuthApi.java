/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityAuthApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;


    @Override
    public String getToken() {
        return "/cmdb/cientity/auth/get";
    }

    @Override
    public String getName() {
        return "获取配置项权限信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, rule = "cientityinsert,cientitydelete,cimanage,transactionmanage,cientityrecover,passwordview", desc = "需要判断的权限列表")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项权限信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityBaseInfoById(jsonObj.getLong("ciEntityId"));
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(jsonObj.getLong("ciEntityId"));
        }
        JSONArray authList = jsonObj.getJSONArray("authList");
        Map<String, Boolean> authMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(authList)) {
            for (int i = 0; i < authList.size(); i++) {
                CiAuthType auth = CiAuthType.get(authList.getString(i));
                if (auth != null) {
                    authMap.put(authList.getString(i), CiAuthChecker.chain().checkAuth(ciEntityVo.getCiId(), ciEntityVo.getId(), auth).check());
                }
            }
        } else {
            for (CiAuthType auth : CiAuthType.values()) {
                authMap.put(auth.getValue(), CiAuthChecker.chain().checkAuth(ciEntityVo.getCiId(), ciEntityVo.getId(), auth).check());
            }
        }
        return authMap;
    }


}
