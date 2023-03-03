/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.customview;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.cmdb.exception.customview.CustomViewCiNotFoundException;
import neatlogic.framework.cmdb.exception.customview.CustomViewPrivilegeException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteCustomViewTemplateApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewMapper customViewMapper;

    @Override
    public String getName() {
        return "删除自定义视图自定义模板";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "customViewId", type = ApiParamType.LONG, desc = "视图id", isRequired = true)})
    @Output({@Param(explode = CustomViewVo.class)})
    @Description(desc = "删除自定义视图自定义模板接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("customViewId");
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
        if (customViewVo == null) {
            throw new CustomViewCiNotFoundException();
        }
        if (customViewVo.getType().equals(CustomViewType.PUBLIC.getValue())) {
            if (!AuthActionChecker.check(CUSTOMVIEW_MODIFY.class)) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.DELETE);
            }
        } else if (customViewVo.getType().equals(CustomViewType.SCENE.getValue())) {
            Long ciId = paramObj.getLong("ciId");
            if (ciId == null) {
                throw new ParamNotExistsException("ciId");
            }
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.DELETE);
            }
        } else if (customViewVo.getType().equals(CustomViewType.PRIVATE.getValue())) {
            if (!customViewVo.getFcu().equalsIgnoreCase(UserContext.get().getUserUuid(true))) {
                throw new CustomViewPrivilegeException(CustomViewPrivilegeException.Action.DELETE);
            }
        }
        customViewMapper.deleteCustomViewTemplateById(id);
        return null;
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/template/delete";
    }
}
