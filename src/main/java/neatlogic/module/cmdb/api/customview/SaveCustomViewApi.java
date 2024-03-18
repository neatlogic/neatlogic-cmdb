/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.customview;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.cmdb.exception.customview.CustomViewCiNotFoundException;
import neatlogic.framework.cmdb.exception.customview.CustomViewNameIsExistsException;
import neatlogic.framework.cmdb.exception.customview.CustomViewPrivilegeSaveException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.customview.CustomViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCustomViewApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewMapper customViewMapper;
    @Autowired
    private CustomViewService customViewService;


    @Override
    public String getToken() {
        return "/cmdb/customview/save";
    }

    @Override
    public String getName() {
        return "nmcac.savecustomviewapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "term.cmdb.viewid"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.customview.ciid"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, maxLength = 50, desc = "common.name"),
            @Param(name = "icon", type = ApiParamType.STRING, desc = "common.icon"),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "common.isactive"),
            @Param(name = "type", type = ApiParamType.ENUM, member = CustomViewType.class, isRequired = true, desc = "common.type"),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "common.authlist", help = "term.cmdb.customview.authhelp"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "common.config")})
    @Output({@Param(name = "Return", type = ApiParamType.LONG, desc = "term.cmdb.viewid")})
    @Description(desc = "nmcac.savecustomviewapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String type = jsonObj.getString("type");
        Long ciId = null;
        if (type.equals(CustomViewType.PUBLIC.getValue())) {
            if (!AuthActionChecker.check(CUSTOMVIEW_MODIFY.class)) {
                throw new CustomViewPrivilegeSaveException();
            }
        } else if (type.equals(CustomViewType.SCENE.getValue())) {
            ciId = jsonObj.getLong("ciId");
            if (ciId == null) {
                throw new ParamNotExistsException("ciId");
            }
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CustomViewPrivilegeSaveException();
            }
        }
        Long id = jsonObj.getLong("id");
        if (id != null) {
            CustomViewVo checkView = customViewService.getCustomViewById(id);
            if (checkView == null) {
                throw new CustomViewCiNotFoundException();
            }
            if (type.equals(CustomViewType.PRIVATE.getValue())) {
                if (!checkView.getFcu().equalsIgnoreCase(UserContext.get().getUserUuid(true))) {
                    throw new CustomViewPrivilegeSaveException();
                }
            }
        }
        JSONObject config = jsonObj.getJSONObject("config");
        CustomViewVo customViewVo = JSONObject.toJavaObject(jsonObj, CustomViewVo.class);
        customViewVo.setCustomViewAuthList(null);
        if (customViewMapper.checkCustomViewNameIsExists(customViewVo) > 0) {
            throw new CustomViewNameIsExistsException(customViewVo.getName());
        }
        if (type.equals(CustomViewType.PRIVATE.getValue())) {
            //私有视图默认都是激活
            customViewVo.setIsActive(1);
        }
        customViewService.parseConfig(customViewVo);
        if (customViewVo.valid()) {
            if (id == null) {
                customViewVo.setFcu(UserContext.get().getUserUuid(true));
                customViewService.insertCustomView(customViewVo);
                if (customViewVo.getType().equals(CustomViewType.SCENE.getValue()) && ciId != null) {
                    customViewMapper.insertCiCustomView(ciId, customViewVo.getId());
                }
            } else {
                customViewVo.setLcu(UserContext.get().getUserUuid(true));
                customViewService.updateCustomView(customViewVo);
            }
            return customViewVo.getId();
        } else {
            return null;
        }
    }

}
