/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/get";
    }

    @Override
    public String getName() {
        return "获取模型信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作，需要的话会进行权限校验")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "获取模型信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        if (Objects.equals(ciVo.getIsVirtual(), 1) && ciVo.getFileId() != null) {
            ciVo.setFileVo(fileMapper.getFileById(ciVo.getFileId()));
        }
        boolean needAction = jsonObj.getBooleanValue("needAction");
        if (needAction) {
            Map<String, Boolean> authData = new HashMap<>();
            boolean hasCiManageAuth, hasCiEntityInsertAuth = false, hasCiEntityUpdateAuth = false, hasCiEntityTransactionAuth = false;
            hasCiManageAuth = CiAuthChecker.chain().checkCiManagePrivilege(ciId).check();
            if (hasCiManageAuth) {
                hasCiEntityInsertAuth = true;
                hasCiEntityTransactionAuth = true;
                hasCiEntityUpdateAuth = true;
            } else if (ciVo.getIsVirtual().equals(0) && ciVo.getIsAbstract().equals(0)) {
                hasCiEntityUpdateAuth = CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check();
                hasCiEntityInsertAuth = CiAuthChecker.chain().checkCiEntityInsertPrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check();
                hasCiEntityTransactionAuth = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).checkCiIsInGroup(ciId, GroupType.MAINTAIN).check();
            }

            authData.put(CiAuthType.CIMANAGE.getValue(), hasCiManageAuth);
            authData.put(CiAuthType.CIENTITYUPDATE.getValue(), hasCiEntityUpdateAuth);
            authData.put(CiAuthType.CIENTITYINSERT.getValue(), hasCiEntityInsertAuth);
            authData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), hasCiEntityTransactionAuth);
            ciVo.setAuthData(authData);
        }
        return ciVo;
    }
}
