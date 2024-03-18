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

package neatlogic.module.cmdb.api.globalattr;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrItemIsInUsedException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrNameIsExistsException;
import neatlogic.framework.cmdb.exception.globalattr.GlobalAttrNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveGlobalAttrApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public String getName() {
        return "nmcag.saveglobalattrapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NAME, desc = "common.uniquename", isRequired = true, xss = true),
            @Param(name = "label", type = ApiParamType.STRING, desc = "common.name", isRequired = true, xss = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive", defaultValue = "0"),
            @Param(name = "isMultiple", type = ApiParamType.INTEGER, desc = "common.ismultiple", defaultValue = "0"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description", xss = true),
            @Param(name = "itemList", type = ApiParamType.JSONARRAY, desc = "nmcag.saveglobalattrapi.input.param.desc")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "nmcaa.getattrapi.input.param.desc.id")})
    @Description(desc = "nmcag.saveglobalattrapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        GlobalAttrVo globalAttrVo = JSONObject.toJavaObject(paramObj, GlobalAttrVo.class);
        Long id = paramObj.getLong("id");
        if (globalAttrMapper.checkGlobalAttrNameIsUsed(globalAttrVo) > 0) {
            throw new GlobalAttrNameIsExistsException(globalAttrVo.getName());
        }
        if (id == null) {
            globalAttrMapper.insertGlobalAttr(globalAttrVo);
            if (CollectionUtils.isNotEmpty(globalAttrVo.getItemList())) {
                int sort = 1;
                for (GlobalAttrItemVo globalAttrItemVo : globalAttrVo.getItemList()) {
                    globalAttrItemVo.setAttrId(globalAttrVo.getId());
                    globalAttrItemVo.setSort(sort);
                    sort += 1;
                    globalAttrMapper.insertGlobalAttrItem(globalAttrItemVo);
                }
            }
        } else {
            GlobalAttrVo oldGlobalAttrVo = globalAttrMapper.getGlobalAttrById(globalAttrVo.getId());
            if (oldGlobalAttrVo == null) {
                throw new GlobalAttrNotFoundException(globalAttrVo.getId());
            }
            globalAttrMapper.updateGlobalAttr(globalAttrVo);
            if (CollectionUtils.isNotEmpty(globalAttrVo.getItemList())) {
                List<GlobalAttrItemVo> updateList = new ArrayList<>();
                List<GlobalAttrItemVo> insertList = new ArrayList<>();
                int sort = 1;
                for (GlobalAttrItemVo globalAttrItemVo : globalAttrVo.getItemList()) {
                    globalAttrItemVo.setSort(sort);
                    if (globalAttrItemVo.getAttrId() == null) {
                        globalAttrItemVo.setAttrId(globalAttrVo.getId());
                        if (StringUtils.isNotBlank(globalAttrItemVo.getValue())) {
                            insertList.add(globalAttrItemVo);
                            sort += 1;
                        }
                    } else {
                        if (StringUtils.isNotBlank(globalAttrItemVo.getValue())) {
                            updateList.add(globalAttrItemVo);
                            sort += 1;
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(insertList)) {
                    for (GlobalAttrItemVo item : insertList) {
                        globalAttrMapper.insertGlobalAttrItem(item);
                    }
                }
                if (CollectionUtils.isNotEmpty(updateList)) {
                    for (GlobalAttrItemVo item : updateList) {
                        globalAttrMapper.updateGlobalAttrItem(item);
                    }
                }
                if (CollectionUtils.isNotEmpty(oldGlobalAttrVo.getItemList())) {
                    oldGlobalAttrVo.getItemList().removeAll(globalAttrVo.getItemList());
                    if (CollectionUtils.isNotEmpty(oldGlobalAttrVo.getItemList())) {
                        for (GlobalAttrItemVo item : oldGlobalAttrVo.getItemList()) {
                            if (globalAttrMapper.checkGlobalAttrItemIsUsed(item.getId()) > 0) {
                                throw new GlobalAttrItemIsInUsedException(item);
                            }
                            globalAttrMapper.deleteGlobalAttrItemById(item.getId());
                        }
                    }
                }
            } else {
                if (CollectionUtils.isNotEmpty(oldGlobalAttrVo.getItemList())) {
                    for (GlobalAttrItemVo item : oldGlobalAttrVo.getItemList()) {
                        if (globalAttrMapper.checkGlobalAttrItemIsUsed(item.getId()) > 0) {
                            throw new GlobalAttrItemIsInUsedException(item);
                        }
                        globalAttrMapper.deleteGlobalAttrItemById(item.getId());
                    }
                }
            }
        }
        return globalAttrVo.getId();
    }

    @Override
    public String getToken() {
        return "/cmdb/globalattr/save";
    }


}
