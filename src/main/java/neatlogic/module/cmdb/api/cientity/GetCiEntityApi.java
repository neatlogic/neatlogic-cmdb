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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
        return "nmcac.getcientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "limitRelEntity", type = ApiParamType.BOOLEAN, desc = "nmcac.getcientityapi.input.param.desc.limitrelentity"),
            @Param(name = "limitAttrEntity", type = ApiParamType.BOOLEAN, desc = "nmcac.getcientityapi.input.param.desc.limitattrentity"),
            @Param(name = "showAttrRelList", type = ApiParamType.JSONARRAY, desc = "nmcac.getcientityapi.input.param.desc.showattrrellist"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.getcientityapi.input.param.desc.needaction")})
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "nmcac.getcientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long ciId = jsonObj.getLong("ciId");
        Boolean limitRelEntity = jsonObj.getBoolean("limitRelEntity");
        Boolean limitAttrEntity = jsonObj.getBoolean("limitAttrEntity");
        if (limitRelEntity == null) {
            limitRelEntity = true;
        }
        if (limitAttrEntity == null) {
            limitAttrEntity = true;
        }
        JSONArray showAttrRelList = jsonObj.getJSONArray("showAttrRelList");
        Set<String> showAttrRelSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(showAttrRelList)) {
            for (int i = 0; i < showAttrRelList.size(); i++) {
                showAttrRelSet.add(showAttrRelList.getString(i));
            }
        }
        boolean needAction = jsonObj.getBooleanValue("needAction");
        CiVo ciVo = ciMapper.getCiById(ciId);
        CiEntityVo pCiEntityVo = new CiEntityVo();
        pCiEntityVo.setId(ciEntityId);
        pCiEntityVo.setCiId(ciId);
        pCiEntityVo.setLimitRelEntity(limitRelEntity);
        pCiEntityVo.setLimitAttrEntity(limitAttrEntity);
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(pCiEntityVo);
        ciEntityVo.setIsVirtual(ciVo.getIsVirtual());
        if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.READONLY, GroupType.MAINTAIN, GroupType.AUTOEXEC).check()) {
            throw new CiEntityAuthException(ciEntityVo.getCiLabel(), TransactionActionType.VIEW.getText());
        }

        if (needAction && ciVo.getIsVirtual().equals(0) && ciVo.getIsAbstract().equals(0)) {
            ciEntityVo.setAuthData(new HashMap<String, Boolean>() {
                {
                    this.put(CiAuthType.CIMANAGE.getValue(), CiAuthChecker.chain().checkCiManagePrivilege(ciEntityVo.getCiId()).check());
                    this.put(CiAuthType.CIENTITYUPDATE.getValue(), CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.MAINTAIN)
                            .check());
                    this.put(CiAuthType.PASSWORDVIEW.getValue(), CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityVo.getId(), GroupType.MAINTAIN)
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
        entityObj.put("attrEntityData", CollectionUtils.isEmpty(showAttrRelSet) ? ciEntityVo.getAttrEntityData() : getFilterAttrRel(showAttrRelSet, ciEntityVo.getAttrEntityData()));
        entityObj.put("relEntityData", CollectionUtils.isEmpty(showAttrRelSet) ? ciEntityVo.getRelEntityData() : getFilterAttrRel(showAttrRelSet, ciEntityVo.getRelEntityData()));
        entityObj.put("globalAttrEntityData", CollectionUtils.isEmpty(showAttrRelSet) ? ciEntityVo.getGlobalAttrEntityData() : getFilterAttrRel(showAttrRelSet, ciEntityVo.getGlobalAttrEntityData()));
        entityObj.put("maxRelEntityCount", ciEntityVo.getMaxRelEntityCount());
        entityObj.put("maxAttrEntityCount", ciEntityVo.getMaxAttrEntityCount());
        entityObj.put("isVirtual", ciEntityVo.getIsVirtual());
        entityObj.put("authData", ciEntityVo.getAuthData());
        return entityObj;
    }

    private JSONObject getFilterAttrRel(Set<String> showAttrRelSet, JSONObject attrRelData) {
        JSONObject returnObj = new JSONObject();
        for (String key : attrRelData.keySet()) {
            if (showAttrRelSet.contains(key)) {
                returnObj.put(key, attrRelData.get(key));
            }
        }
        return returnObj;
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
