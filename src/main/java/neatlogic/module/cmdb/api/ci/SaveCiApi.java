/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.ci.CiIsAbstractedException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.ci.VirtualCiSettingFileNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiApi extends PrivateApiComponentBase {

    @Resource
    private CiService ciService;


    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiSchemaMapper ciSchemaMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/save";
    }

    @Override
    public String getName() {
        return "nmcac.saveciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "nmcac.saveciapi.input.param.desc.id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NUMBER_NAME, xss = true, isRequired = true, maxLength = 50, desc = "common.uniquename"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "common.cnname", xss = true, maxLength = 100,
                    isRequired = true),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.memo", maxLength = 500, xss = true),
            @Param(name = "icon", type = ApiParamType.STRING, isRequired = true, desc = "common.icon"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "common.typeid", isRequired = true),
            @Param(name = "catalogId", type = ApiParamType.LONG, desc = "common.catalogid"),
            @Param(name = "parentCiId", type = ApiParamType.LONG, desc = "term.cmdb.parentcientityid"),
            @Param(name = "viewXml", type = ApiParamType.STRING, desc = "common.config"),
            //@Param(name = "fileId", type = ApiParamType.LONG, desc = "term.cmdb.virtualcifileid"),
            @Param(name = "expiredDay", type = ApiParamType.INTEGER, desc = "common.expireddays"),
            @Param(name = "isAbstract", type = ApiParamType.INTEGER, defaultValue = "0", desc = "term.cmdb.isabstractci"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, defaultValue = "0", desc = "term.cmdb.isvirtualci"),
            @Param(name = "isPrivate", type = ApiParamType.INTEGER, defaultValue = "0", desc = "term.cmdb.isprivateci"),
            @Param(name = "isMenu", type = ApiParamType.INTEGER, defaultValue = "0", desc = "term.cmdb.isshowinmenu")})
    @Output({@Param(name = "id", type = ApiParamType.STRING, desc = "term.cmdb.ciid")})
    @Description(desc = "nmcac.saveciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo ciVo = JSON.toJavaObject(jsonObj, CiVo.class);
        Long ciId = jsonObj.getLong("id");
        if (Objects.equals(ciVo.getIsVirtual(), 1)) {
            if (StringUtils.isBlank(ciVo.getViewXml())) {
                throw new VirtualCiSettingFileNotFoundException();
            }
        }
        if (ciId == null) {
            if (!CiAuthChecker.chain().checkCiManagePrivilege().check()) {
                throw new CiAuthException();
            }
            ciService.insertCi(ciVo);
        } else {
            CiVo oldCiVo = ciMapper.getCiById(ciId);

            if (oldCiVo == null) {
                throw new CiNotFoundException(ciId);
            }
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CiAuthException();
            }
            oldCiVo.setHasChildren(oldCiVo.getRht() - oldCiVo.getLft() > 1);
            Integer rc = ciSchemaMapper.checkTableHasData(TenantContext.get().getDataDbName(), ciVo.getCiTableName(false));
            oldCiVo.setHasData(rc != null && rc > 0);
            if ((oldCiVo.getHasChildren() || oldCiVo.getHasData()) && !Objects.equals(oldCiVo.getIsAbstract(), ciVo.getIsAbstract())) {
                throw new CiIsAbstractedException(CiIsAbstractedException.Type.UPDATEABSTRACT, ciVo.getLabel());
            }
            if (oldCiVo.getHasData() && !Objects.equals(oldCiVo.getParentCiId(), ciVo.getParentCiId())) {
                throw new CiIsAbstractedException(CiIsAbstractedException.Type.UPDATEPARENT, ciVo.getLabel());
            }
            ciService.updateCi(ciVo);
        }
        return ciVo.getId();
    }

}
