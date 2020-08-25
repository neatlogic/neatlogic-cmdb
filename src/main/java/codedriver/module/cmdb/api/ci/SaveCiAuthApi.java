package codedriver.module.cmdb.api.ci;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiAuthMapper;
import codedriver.module.cmdb.dto.ci.CiAuthVo;
import codedriver.module.cmdb.exception.ci.CiAuthInvalidException;

@Service
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class SaveCiAuthApi extends ApiComponentBase {

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
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY");
        if (!hasAuth) {

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
