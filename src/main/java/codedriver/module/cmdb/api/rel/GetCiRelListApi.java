package codedriver.module.cmdb.api.rel;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.service.ci.CiAuthChecker;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiRelListApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/{ciId}/listrel";
    }

    @Override
    public String getName() {
        return "获取模型关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要则根据用户权限返回合适的操作列")})
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "获取模型关系列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        List<RelVo> relList = relMapper.getRelByCiId(ciId);
        if (needAction) {
            boolean hasManageAuth = AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY");
            for (RelVo relVo : relList) {
                if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                    if (hasManageAuth) {
                        relVo.setToAllowInsert(true);
                    } else {
                        relVo.setToAllowInsert(CiAuthChecker.hasCiEntityInsertPrivilege(relVo.getToCiId()));
                    }
                } else {
                    if (hasManageAuth) {
                        relVo.setFromAllowInsert(true);
                    } else {
                        relVo.setFromAllowInsert(CiAuthChecker.hasCiEntityInsertPrivilege(relVo.getFromCiId()));
                    }
                }
            }
        }
        return relList;
    }
}
