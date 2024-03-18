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

package neatlogic.module.cmdb.api.attr;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiGlobalAttrListApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/listglobalattr";
    }

    @Override
    public String getName() {
        return "nmcaa.getciglobalattrlistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "term.cmdb.ciname"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "common.displaytype"),
            @Param(name = "allowEdit", type = ApiParamType.INTEGER, rule = "1,0", desc = "term.cmdb.allowedit"),
            @Param(name = "needAlias", type = ApiParamType.INTEGER, desc = "term.cmdb.needalias", rule = "0,1"),
            @Param(name = "mergeAlias", type = ApiParamType.INTEGER, defaultValue = "1", desc = "term.cmdb.mergealias", rule = "0,1")
    })
    @Output({
            @Param(name = "Return", explode = GlobalAttrVo[].class)
    })
    @Description(desc = "nmcaa.getciglobalattrlistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        int needAlias = jsonObj.getIntValue("needAlias");
        int mergeAlias = jsonObj.getInteger("mergeAlias");
        if (ciId == null) {
            String ciName = jsonObj.getString("ciName");
            if (StringUtils.isBlank(ciName)) {
                throw new ParamNotExistsException("ciId", "ciName");
            }
            CiVo ciVo = ciMapper.getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
            ciId = ciVo.getId();
        }
        String showType = jsonObj.getString("showType");
        Integer allowEdit = jsonObj.getInteger("allowEdit");
        List<GlobalAttrVo> attrList = globalAttrMapper.getGlobalAttrByCiId(ciId);
        List<CiViewVo> ciViewList;
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> attrSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().equals("global")) {
                    attrSet.add(ciView.getItemId());
                }
            }
            attrList.removeIf(attr -> !attrSet.contains(attr.getId()));
        } else {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        }
        if (needAlias == 1 && CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo ciView : ciViewList) {
                if (StringUtils.isNotBlank(ciView.getAlias()) && ciView.getType().equals("global")) {
                    Optional<GlobalAttrVo> op = attrList.stream().filter(d -> d.getId().equals(ciView.getItemId())).findFirst();
                    if(mergeAlias==1) {
                        op.ifPresent(attrVo -> attrVo.setLabel(ciView.getAlias()));
                    }else{
                        op.ifPresent(attrVo -> attrVo.setAlias(ciView.getAlias()));
                    }
                }
            }
        }
        if (allowEdit != null) {
            attrList.removeIf(attr -> (allowEdit.equals(1) && (attr.getAllowEdit() != null && attr.getAllowEdit().equals(0)))
                    || (allowEdit.equals(0) && (attr.getAllowEdit() == null || attr.getAllowEdit().equals(1))));
        }
        return attrList;
    }
}
