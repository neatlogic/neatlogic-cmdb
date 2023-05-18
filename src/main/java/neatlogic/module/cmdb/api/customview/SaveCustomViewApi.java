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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.CUSTOMVIEW_MODIFY;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.enums.customview.CustomViewType;
import neatlogic.framework.cmdb.enums.customview.RelType;
import neatlogic.framework.cmdb.exception.customview.CustomViewCiNotFoundException;
import neatlogic.framework.cmdb.exception.customview.CustomViewEmptyException;
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
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return "保存自定义视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id，不提供代表新增"), @Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id，保存场景视图时需要提供"), @Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, maxLength = 50, desc = "名称"), @Param(name = "icon", type = ApiParamType.STRING, desc = "图标"), @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活"), @Param(name = "type", type = ApiParamType.ENUM, member = CustomViewType.class, isRequired = true, desc = "类型"), @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "授权列表", help = "仅对公共视图生效"), @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "拓扑图配置")})
    @Output({@Param(name = "Return", type = ApiParamType.LONG, desc = "视图id")})
    @Description(desc = "保存自定义视图接口")
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
