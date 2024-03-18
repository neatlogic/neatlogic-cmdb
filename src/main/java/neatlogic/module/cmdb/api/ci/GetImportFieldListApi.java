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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional // 需要启用事务，以便查询权限时激活一级缓存
public class GetImportFieldListApi extends PrivateApiComponentBase {

    @Resource
    private CiService ciService;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiViewMapper ciViewMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/import/listfield";
    }

    @Override
    public String getName() {
        return "nmcac.getimportfieldlistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "nmcac.getimportfieldlistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        CiVo ciVo = ciService.getCiById(ciId);
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        ciViewVo.setNeedAlias(1);
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
            ciVo.getAttrList().removeIf(attrVo -> !attrVo.getCanImport());
        }
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo view : ciViewList) {
                if (view.getType().equals("attr") && CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                    Optional<AttrVo> op = ciVo.getAttrList().stream().filter(d -> d.getId().equals(view.getItemId())).findFirst();
                    op.ifPresent(attrVo -> attrVo.setLabel(view.getAlias()));
                } else if (view.getType().equals("global") && CollectionUtils.isNotEmpty(ciVo.getGlobalAttrList())) {
                    Optional<GlobalAttrVo> op = ciVo.getGlobalAttrList().stream().filter(d -> d.getId().equals(view.getItemId())).findFirst();
                    op.ifPresent(globalAttrVo -> globalAttrVo.setLabel(view.getAlias()));
                } else if (view.getType().startsWith("rel") && CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                    Optional<RelVo> op = ciVo.getRelList().stream().filter(d -> d.getId().equals(view.getItemId())).findFirst();
                    if (op.isPresent()) {
                        if (view.getType().startsWith("relfrom")) {
                            op.get().setToLabel(view.getAlias());
                        } else if (view.getType().startsWith("relto")) {
                            op.get().setFromLabel(view.getAlias());
                        }
                    }
                }
            }
        }

        //唯一表达式的属性也需要设为必填
        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
            for (Long attrId : ciVo.getUniqueAttrIdList()) {
                AttrVo attrVo = ciVo.getAttrById(attrId);
                if (attrVo != null) {
                    attrVo.setIsRequired(1);
                }
            }
        }
        return ciVo;
    }
}
