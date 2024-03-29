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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiRelListApi extends PrivateApiComponentBase {

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/listrel";
    }

    @Override
    public String getName() {
        return "nmcar.getcirellistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "relId", type = ApiParamType.LONG, desc = "term.cmdb.sourcerelid"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcar.getcirellistapi.input.param.desc"),
            @Param(name = "allowEdit", type = ApiParamType.INTEGER, rule = "1,0", desc = "term.cmdb.allowedit"),
            @Param(name = "isRequired", type = ApiParamType.INTEGER, rule = "1,0", desc = "common.isrequired"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "common.displaytype"),
            @Param(name = "needAlias", type = ApiParamType.INTEGER, desc = "term.cmdb.needalias", rule = "0,1"),
            @Param(name = "mergeAlias", type = ApiParamType.INTEGER, defaultValue = "1", desc = "term.cmdb.mergealias", rule = "0,1")
    })
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "nmcar.getcirellistapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long relId = jsonObj.getLong("relId");
        String showType = jsonObj.getString("showType");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        Integer allowEdit = jsonObj.getInteger("allowEdit");
        Integer isRequired = jsonObj.getInteger("isRequired");
        int needAlias = jsonObj.getIntValue("needAlias");
        int mergeAlias = jsonObj.getInteger("mergeAlias");
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        if (relId != null) {
            relList.removeIf(d -> d.getId().equals(relId));
        }
        List<CiViewVo> ciViewList;
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
            Set<Long> relSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().startsWith("rel")) {
                    relSet.add(ciView.getItemId());
                }
            }
            relList.removeIf(rel -> !relSet.contains(rel.getId()));
        } else {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        }
        if (needAlias == 1 && CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo ciView : ciViewList) {
                if (StringUtils.isNotBlank(ciView.getAlias()) && (ciView.getType().equals("relfrom") || ciView.getType().equals("relto"))) {
                    Optional<RelVo> op = relList.stream().filter(d -> d.getId().equals(ciView.getItemId())).findFirst();
                    if (op.isPresent()) {
                        RelVo rel = op.get();
                        if (mergeAlias == 1) {
                            if (rel.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                rel.setToLabel(ciView.getAlias());
                            } else {
                                rel.setFromLabel(ciView.getAlias());
                            }
                        } else {
                            if (rel.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                rel.setAlias(ciView.getAlias());
                            } else {
                                rel.setAlias(ciView.getAlias());
                            }
                        }
                    }
                }
            }
        }
        if (allowEdit != null) {
            relList.removeIf(rel -> (allowEdit.equals(1) && (rel.getAllowEdit() != null && rel.getAllowEdit().equals(0)))
                    || (allowEdit.equals(0) && (rel.getAllowEdit() == null || rel.getAllowEdit().equals(1))));
        }
        if (isRequired != null && isRequired.equals(1)) {
            relList.removeIf(rel -> (rel.getDirection().equals(RelDirectionType.FROM.getValue()) && rel.getToIsRequired().equals(0)) ||
                    (rel.getDirection().equals(RelDirectionType.TO.getValue()) && rel.getFromIsRequired().equals(0)));
        }
        Map<Long, CiVo> checkCiMap = new HashMap<>();
        if (needAction) {
            for (RelVo relVo : relList) {
                if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                    CiVo ciVo = checkCiMap.get(relVo.getToCiId());
                    if (ciVo == null) {
                        ciVo = ciMapper.getCiById(relVo.getToCiId());
                        if (ciVo == null) {
                            throw new CiNotFoundException(relVo.getToCiId());
                        }
                        checkCiMap.put(relVo.getToCiId(), ciVo);
                    }
                    if (ciVo.getIsVirtual().equals(1)) {
                        relVo.setToAllowInsert(false);
                    } else {
                        relVo.setToAllowInsert(CiAuthChecker.chain().checkCiEntityInsertPrivilege(relVo.getToCiId()).check());
                    }
                } else {
                    CiVo ciVo = checkCiMap.get(relVo.getFromCiId());
                    if (ciVo == null) {
                        ciVo = ciMapper.getCiById(relVo.getFromCiId());
                        if (ciVo == null) {
                            throw new CiNotFoundException(relVo.getFromCiId());
                        }
                        checkCiMap.put(relVo.getFromCiId(), ciVo);
                    }
                    if (ciVo.getIsVirtual().equals(1)) {
                        relVo.setFromAllowInsert(false);
                    } else {
                        relVo.setFromAllowInsert(CiAuthChecker.chain().checkCiEntityInsertPrivilege(relVo.getFromCiId()).check());
                    }
                }
            }
        }
        return relList;
    }
}
