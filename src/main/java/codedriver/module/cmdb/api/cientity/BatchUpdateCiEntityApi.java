/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchUpdateCiEntityApi extends PrivateApiComponentBase {
    //static Logger logger = LoggerFactory.getLogger(BatchSaveCiEntityApi.class);

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchupdate";
    }

    @Override
    public String getName() {
        return "批量修改配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "ciEntityIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "被修改配置项id"),
            @Param(name = "attrEntityData", type = ApiParamType.JSONOBJECT, desc = "需要修改的属性"),
            @Param(name = "relEntityData", type = ApiParamType.JSONOBJECT, desc = "需要修改的关系"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, desc = "是否提交"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注")})
    @Description(desc = "批量修改配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciId).check()) {
            throw new CiEntityAuthException(ciVo.getLabel(), TransactionActionType.UPDATE.getText());
        }

        boolean needCommit = jsonObj.getBooleanValue("needCommit");
        if (needCommit) {
            if (!CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciId).check()) {
                needCommit = false;
            }
        }

        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
        //修正新配置项的uuid为id
        if (MapUtils.isNotEmpty(attrObj)) {
            for (String key : attrObj.keySet()) {
                JSONObject obj = attrObj.getJSONObject(key);
                JSONArray valueList = obj.getJSONArray("valueList");
                //删除没用的属性
                obj.remove("actualValueList");
                if (CollectionUtils.isNotEmpty(valueList)) {
                    //因为可能需要删除某些成员，所以需要倒着循环
                    for (int i = valueList.size() - 1; i >= 0; i--) {
                        if (valueList.get(i) instanceof JSONObject) {
                            JSONObject valueObj = valueList.getJSONObject(i);
                            Long attrCiEntityId = valueObj.getLong("id");
                            if (attrCiEntityId != null) {
                                valueList.set(i, attrCiEntityId);
                            } else {
                                valueList.remove(i);
                            }
                        }
                    }
                }
            }
        }

        JSONArray ciEntityIdList = jsonObj.getJSONArray("ciEntityIdList");
        String description = jsonObj.getString("description");
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            for (int i = 0; i < ciEntityIdList.size(); i++) {
                Long ciEntityId = ciEntityIdList.getLong(i);
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(ciEntityId);
                ciEntityTransactionVo.setCiId(ciId);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
                ciEntityTransactionVo.setAllowCommit(needCommit);
                ciEntityTransactionVo.setDescription(description);
                ciEntityTransactionVo.setAttrEntityData(JSONObject.parseObject(jsonObj.getString("attrEntityData")));
                ciEntityTransactionVo.setRelEntityData(JSONObject.parseObject(jsonObj.getString("relEntityData")));
                ciEntityTransactionVo.setEditMode(EditModeType.PARTIAL.getValue());
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
            ciEntityService.saveCiEntity(ciEntityTransactionList);
        }
        return null;
    }

}
