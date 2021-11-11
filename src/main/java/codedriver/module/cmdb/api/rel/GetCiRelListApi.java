/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.framework.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONObject;
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
        return "/cmdb/ci/{ciId}/listrel";
    }

    @Override
    public String getName() {
        return "获取模型关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要则根据用户权限返回合适的操作列"),
            @Param(name = "allowEdit", type = ApiParamType.ENUM, rule = "1,0", desc = "是否允许编辑"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "显示类型")
    })
    @Output({@Param(explode = RelVo[].class)})
    @Description(desc = "获取模型关系列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String showType = jsonObj.getString("showType");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        Integer allowEdit = jsonObj.getInteger("allowEdit");
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
            Set<Long> relSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().startsWith("rel")) {
                    relSet.add(ciView.getItemId());
                }
            }
            relList.removeIf(rel -> !relSet.contains(rel.getId()));
        }
        if (allowEdit != null) {
            relList.removeIf(rel -> (allowEdit.equals(1) && (rel.getAllowEdit() != null && rel.getAllowEdit().equals(0)))
                    || (allowEdit.equals(0) && (rel.getAllowEdit() == null || rel.getAllowEdit().equals(1))));
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
