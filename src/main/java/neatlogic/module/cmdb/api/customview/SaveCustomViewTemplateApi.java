/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.customview;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.CustomViewTemplateVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.framework.cmdb.exception.customview.CustomViewPrivilegeException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCustomViewTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewMapper customViewMapper;


    @Override
    public String getToken() {
        return "/cmdb/customview/template/save";
    }

    @Override
    public String getName() {
        return "保存自定义视图自定义模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "customViewId", type = ApiParamType.LONG, isRequired = true, desc = "视图id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"),
            @Param(name = "template", type = ApiParamType.STRING, isRequired = true, desc = "模板内容"),
            @Param(name = "config", type = ApiParamType.STRING, desc = "配置")})
    @Description(desc = "保存自定义视图自定义模板接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("customViewId");
        CustomViewTemplateVo customViewTemplateVo = JSONObject.toJavaObject(jsonObj, CustomViewTemplateVo.class);
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(id);
        }

        if (customViewVo.getType().equals(CustomViewType.PUBLIC.getValue())) {
            if (!AuthActionChecker.check(CUSTOMVIEW_MODIFY.class)) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.SAVE);
            }
        } else if (customViewVo.getType().equals(CustomViewType.SCENE.getValue())) {
            Long ciId = jsonObj.getLong("ciId");
            if (ciId == null) {
                throw new ParamNotExistsException("ciId");
            }
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.SAVE);
            }
        } else if (customViewVo.getType().equals(CustomViewType.PRIVATE.getValue())) {
            if (!customViewVo.getFcu().equalsIgnoreCase(UserContext.get().getUserUuid(true))) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.SAVE);
            }
        }
        customViewMapper.insertCustomViewTemplate(customViewTemplateVo);
        return null;
    }

}
