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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
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
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
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
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.ENGLISH_NUMBER_NAME, xss = true, isRequired = true, maxLength = 25, desc = "common.uniquename"),
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
