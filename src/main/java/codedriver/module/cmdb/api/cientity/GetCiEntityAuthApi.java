/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
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
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, rule = "cientityinsert,cientitydelete,cimanage,transactionmanage,cientityrecover,passwordview", desc = "需要判断的权限列表")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项权限信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityBaseInfoById(jsonObj.getLong("ciEntityId"));
        //如果需要获取恢复权限，配置项已经被删除，需要改用模型id获取权限
        Long ciId = (ciEntityVo != null ? ciEntityVo.getCiId() : jsonObj.getLong("ciId"));
        Map<String, Boolean> authMap = new HashMap<>();
        if (ciId != null) {
            JSONArray authList = jsonObj.getJSONArray("authList");
            if (CollectionUtils.isNotEmpty(authList)) {
                for (int i = 0; i < authList.size(); i++) {
                    String authString = authList.getString(i);
                    CiAuthType auth = CiAuthType.get(authString);
                    if (auth != null) {
                        authMap.put(authString, CiAuthChecker.chain().checkAuth(ciId, auth).check());
                    }
                }
            } else {
                for (CiAuthType auth : CiAuthType.values()) {
                    authMap.put(auth.getValue(), CiAuthChecker.chain().checkAuth(ciId, auth).check());
                }
            }
        }
        return authMap;
    }


}
