package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.CiViewVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiRelListApi extends PrivateApiComponentBase {

    @Autowired
    private RelMapper relMapper;

    @Autowired
    private CiViewMapper ciViewMapper;

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
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要则根据用户权限返回合适的操作列"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "显示类型")})
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "获取模型关系列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String showType = jsonObj.getString("showType");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        List<RelVo> relList = relMapper.getRelByCiId(ciId);
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> relSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().startsWith("rel")) {
                    relSet.add(ciView.getItemId());
                }
            }
            relList.removeIf(rel -> !relSet.contains(rel.getId()));
        }
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
