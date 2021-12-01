/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/get";
    }

    @Override
    public String getName() {
        return "获取配置项详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "limitRelEntity", type = ApiParamType.BOOLEAN, desc = "是否限制关系数量"),
            @Param(name = "limitAttrEntity", type = ApiParamType.BOOLEAN, desc = "是否限制引用属性数量"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要检查操作权限，会根据结果返回action列")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long ciId = jsonObj.getLong("ciId");
        Boolean limitRelEntity = jsonObj.getBoolean("limitRelEntity");
        Boolean limitAttrEntity = jsonObj.getBoolean("limitAttrEntity");
        boolean needAction = jsonObj.getBooleanValue("needAction");
        CiVo ciVo = ciMapper.getCiById(ciId);
        CiEntityVo pCiEntityVo = new CiEntityVo();
        pCiEntityVo.setId(ciEntityId);
        pCiEntityVo.setCiId(ciId);
        if (limitRelEntity != null) {
            pCiEntityVo.setLimitRelEntity(limitRelEntity);
        }
        if (limitAttrEntity != null) {
            pCiEntityVo.setLimitAttrEntity(limitAttrEntity);
        }
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(pCiEntityVo);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        ciEntityVo.setIsVirtual(ciVo.getIsVirtual());
        if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.READONLY).check()) {
            throw new CiEntityAuthException(ciEntityVo.getCiLabel(), "查看");
        }

        if (needAction && ciVo.getIsVirtual().equals(0) && ciVo.getIsAbstract().equals(0)) {
            ciEntityVo.setAuthData(new HashMap<String, Boolean>() {
                {
                    this.put(CiAuthType.CIENTITYUPDATE.getValue(), CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.MAINTAIN)
                            .check());
                    this.put(CiAuthType.PASSWORDVIEW.getValue(), CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)
                            .check());
                    this.put(CiAuthType.TRANSACTIONMANAGE.getValue(), CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.MAINTAIN).check());
                }
            });
        }
        //由于前端无法识别属性格式进行转换，例如时间和颜色等，所以先在后台处理好再输出
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        JSONObject entityObj = new JSONObject();
        entityObj.put("id", ciEntityVo.getId());
        entityObj.put("uuid", ciEntityVo.getUuid());
        entityObj.put("name", ciEntityVo.getName());
        entityObj.put("ciId", ciEntityVo.getCiId());
        entityObj.put("rootCiId", ciEntityVo.getRootCiId());
        entityObj.put("ciIcon", ciEntityVo.getCiIcon());
        entityObj.put("ciName", ciEntityVo.getCiName());
        entityObj.put("ciLabel", ciEntityVo.getCiLabel());
        entityObj.put("type", ciEntityVo.getTypeId());
        entityObj.put("typeName", ciEntityVo.getTypeName());
        entityObj.put("inspectTime", ciEntityVo.getInspectTime() != null ? sdf.format(ciEntityVo.getInspectTime()) : null);
        entityObj.put("inspectStatus", makeupStatus(ciEntityVo.getInspectStatus()));
        entityObj.put("monitorTime", ciEntityVo.getMonitorTime() != null ? sdf.format(ciEntityVo.getMonitorTime()) : null);
        entityObj.put("monitorStatus", makeupStatus(ciEntityVo.getMonitorStatus()));
        entityObj.put("renewTime", ciEntityVo.getRenewTime() != null ? sdf.format(ciEntityVo.getRenewTime()) : null);
        entityObj.put("actionType", ciEntityVo.getActionType());
        entityObj.put("attrEntityData", ciEntityVo.getAttrEntityData());
        entityObj.put("relEntityData", ciEntityVo.getRelEntityData());
        entityObj.put("maxRelEntityCount", ciEntityVo.getMaxRelEntityCount());
        entityObj.put("maxAttrEntityCount", ciEntityVo.getMaxAttrEntityCount());
        entityObj.put("isVirtual", ciEntityVo.getIsVirtual());
        entityObj.put("authData", ciEntityVo.getAuthData());
        return entityObj;
    }

    private String makeupStatus(String status) {
        if (StringUtils.isNotBlank(status)) {
            switch (status) {
                case "fatal":
                    return "<span class=\"text-error\">" + status.toUpperCase(Locale.ROOT) + "</span>";
                case "warn":
                    return "<span class=\"text-warning\">" + status.toUpperCase(Locale.ROOT) + "</span>";
                case "critical":
                    return "<span class=\"text-error\">" + status.toUpperCase(Locale.ROOT) + "</span>";
                default:
                    return "<span class=\"text-success\">" + status.toUpperCase(Locale.ROOT) + "</span>";
            }
        }
        return "";
    }

}
