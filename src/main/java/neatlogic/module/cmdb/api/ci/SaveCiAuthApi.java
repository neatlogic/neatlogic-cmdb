/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.ci.CiAuthInvalidException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiAuthApi extends PrivateApiComponentBase {

    @Autowired
    private CiAuthMapper ciAuthMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/auth/save";
    }

    @Override
    public String getName() {
        return "保存模型授权";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY,
                    desc = "授权列表，为空代表清空所有授权，范例：{authType:'user',authUuid:'xxxx',action:'cimaange'}")})
    @Description(desc = "保存模型授权接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        boolean hasAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
        if (!hasAuth) {
            throw new CiAuthException();
        }

        ciAuthMapper.deleteCiAuthByCiId(ciId);
        JSONArray authList = jsonObj.getJSONArray("authList");
        if (!CollectionUtils.isEmpty(authList)) {
            for (int i = 0; i < authList.size(); i++) {
                JSONObject authObj = authList.getJSONObject(i);
                CiAuthVo ciAuthVo = JSONObject.toJavaObject(authObj, CiAuthVo.class);
                ciAuthVo.setCiId(ciId);
                if (StringUtils.isBlank(ciAuthVo.getAction()) || StringUtils.isBlank(ciAuthVo.getAuthType())
                        || StringUtils.isBlank(ciAuthVo.getAuthUuid())) {
                    throw new CiAuthInvalidException();
                }
                ciAuthMapper.insertCiAuth(ciAuthVo);
            }
        }
        return null;
    }

}
