/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.enums.customview.CustomViewType;
import codedriver.framework.cmdb.exception.customview.CustomViewCiNotFoundException;
import codedriver.framework.cmdb.exception.customview.CustomViewPrivilegeException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.customview.CustomViewService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeleteCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewService customViewService;

    @Resource
    private CustomViewMapper customViewMapper;

    @Override
    public String getName() {
        return "删除自定义视图";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id", isRequired = true)})
    @Output({@Param(explode = CustomViewVo.class)})
    @Description(desc = "删除自定义视图接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        CustomViewVo customViewVo = customViewService.getCustomViewById(id);
        if (customViewVo == null) {
            throw new CustomViewCiNotFoundException();
        }
        if (customViewVo.getType().equals(CustomViewType.PUBLIC.getValue())) {
            if (!AuthActionChecker.check("CUSTOMVIEW_MODIFY")) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.DELETE);
            }
        } else if (customViewVo.getType().equals(CustomViewType.PRIVATE.getValue())) {
            if (!customViewVo.getFcu().equalsIgnoreCase(UserContext.get().getUserUuid(true))) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.DELETE);
            }
        } else if (customViewVo.getType().equals(CustomViewType.SCENE.getValue())) {
            Long ciId = customViewMapper.getCiIdByCustomViewId(id);
            if (ciId != null) {
                if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                    throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.DELETE);
                }
            }
        }
        customViewService.deleteCustomView(id);
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/delete";
    }
}
