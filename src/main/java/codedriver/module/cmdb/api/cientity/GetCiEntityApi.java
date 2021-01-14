package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.CiAuthType;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/get";
    }

    @Override
    public String getName() {
        return "获取配置项详细信息";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("serial")
    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
        @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要检查操作权限，会根据结果返回action列")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        Long id = jsonObj.getLong("id");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityById(id);
        if (ciEntityVo != null) {
            List<AttrEntityVo> attrEntityList = attrEntityMapper.getAttrEntityByCiEntityId(id);
            ciEntityVo.setAttrEntityList(attrEntityList);
            List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(id);
            ciEntityVo.setRelEntityList(relEntityList);

            boolean canEdit = AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY");
            if (needAction) {
                if (!canEdit) {
                    canEdit = CiAuthChecker.builder().hasCiManagePrivilege(ciEntityVo.getId())
                        .hasCiEntityUpdatePrivilege(ciEntityVo.getId()).isInGroup(ciEntityVo.getId(), GroupType.MATAIN)
                        .check();
                }

                if (canEdit) {
                    ciEntityVo.setAuthData(new HashMap<String, Boolean>() {
                        {
                            this.put(CiAuthType.CIENTITYUPDATE.getValue(), true);
                        }
                    });
                }
            }
            if (!canEdit) {// 没有维护权限的情况下，判断是否拥有查看权限
                boolean canView =
                    CiAuthChecker.builder().hasCiEntityQueryPrivilege(ciEntityVo.getId(), ciEntityVo.getId())
                        .isInGroup(ciEntityVo.getId(), GroupType.READONLY).check();
                if (!canView) {
                    
                }
            }
        }
        return ciEntityVo;
    }

}
