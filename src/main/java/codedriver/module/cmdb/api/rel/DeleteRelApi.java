/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.InputFrom;
import codedriver.framework.cmdb.exception.ci.CiAuthException;
import codedriver.framework.cmdb.exception.rel.RelNotFoundException;
import codedriver.framework.cmdb.threadlocal.InputFromContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.rel.RelService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteRelApi extends PrivateApiComponentBase {

    @Autowired
    private RelService relService;

    @Autowired
    private RelMapper relMapper;

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/delete";
    }

    @Override
    public String getName() {
        return "删除模型关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "关系id")})
    @Description(desc = "删除模型关系接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        InputFromContext.init(InputFrom.PAGE);
        Long relId = jsonObj.getLong("id");
        RelVo relVo = relMapper.getRelById(relId);
        if (relVo == null) {
            throw new RelNotFoundException(relId);
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(relVo.getFromCiId()).check()) {
            CiVo ciVo = ciMapper.getCiById(relVo.getFromCiId());
            throw new CiAuthException(ciVo.getLabel());
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(relVo.getToCiId()).check()) {
            CiVo ciVo = ciMapper.getCiById(relVo.getToCiId());
            throw new CiAuthException(ciVo.getLabel());
        }
        relService.deleteRel(relVo);
        return null;
    }

}
