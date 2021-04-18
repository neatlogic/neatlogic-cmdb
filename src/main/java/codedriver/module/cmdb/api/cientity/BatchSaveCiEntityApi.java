/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.enums.GroupType;
import codedriver.framework.cmdb.enums.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchSaveCiEntityApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(BatchSaveCiEntityApi.class);

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/batchsave";
    }

    @Override
    public String getName() {
        return "批量保存配置项";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项数据")})
    @Description(desc = "批量保存配置项接口")
    @Example(example = "{\"ciEntityList\":[{\"attrEntityData\":{\"attr_323010784722944\":{\"valueList\":[\"测试环境\"],\"name\":\"label\",\"label\":\"显示名\",\"type\":\"text\",\"saveMode\":\"merge\"},\"attr_323010700836864\":{\"valueList\":[\"stg33\"],\"name\":\"name\",\"label\":\"唯一标识\",\"type\":\"text\"}},\"ciId\":323010541453312,\"ciLabel\":\"环境\",\"ciName\":\"env\",\"fcd\":1617187647522,\"fcu\":\"20f2fbfe97cf11ea94ff005056c00001\",\"id\":330340423237635,\"isLocked\":0,\"lcd\":1617273899288,\"lcu\":\"20f2fbfe97cf11ea94ff005056c00001\",\"uuid\":\"3e3e74b1947b400aa34d7c6964f79168\"}]}")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY");
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
        // 先给所有没有id的ciEntity分配新的id
        for (int ciindex = 0; ciindex < ciEntityObjList.size(); ciindex++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(ciindex);
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            if (StringUtils.isNotBlank(uuid)) {
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                if (id != null) {
                    ciEntityTransactionVo.setCiEntityId(id);
                }
                ciEntityTransactionMap.put(uuid, ciEntityTransactionVo);
            }
        }

        for (int ciindex = 0; ciindex < ciEntityObjList.size(); ciindex++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(ciindex);
            Long ciId = ciEntityObj.getLong("ciId");
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");

            if (!hasAuth) {
                // 拥有模型管理权限允许添加或修改配置项
                hasAuth = CiAuthChecker.hasCiManagePrivilege(ciId);
            }
            TransactionActionType mode = TransactionActionType.INSERT;
            CiEntityTransactionVo ciEntityTransactionVo = null;
            if (id != null) {
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.chain().hasCiEntityUpdatePrivilege(ciId).isInGroup(id, GroupType.MATAIN)
                            .check();
                }
                ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(id);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            } else if (StringUtils.isNotBlank(uuid)) {
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiEntityInsertPrivilege(ciId);
                }
                ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
                ciEntityTransactionVo.setCiEntityUuid(uuid);
                ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
            } else {
                throw new ApiRuntimeException("数据不合法，缺少id或uuid");
            }

            if (!hasAuth) {
                CiVo ciVo = ciMapper.getCiById(ciId);
                throw new CiEntityAuthException(ciVo.getLabel(), mode.getText());
            }

            ciEntityTransactionVo.setCiId(ciId);
            // 解析属性数据
            JSONObject attrObj = ciEntityObj.getJSONObject("attrEntityData");
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
                                String attrCiEntityUuid = valueObj.getString("uuid");
                                Long attrCiEntityId = valueObj.getLong("id");
                                if (attrCiEntityId == null && StringUtils.isNotBlank(attrCiEntityUuid)) {
                                    CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(attrCiEntityUuid);
                                    if (tmpVo != null) {
                                        //替换掉原来的ciEntityUuid为新的ciEntityId
                                        valueList.set(i, tmpVo.getCiEntityId());
                                    } else {
                                        throw new ApiRuntimeException("找不到" + attrCiEntityUuid + "的新配置项");
                                    }
                                } else if (attrCiEntityId != null) {
                                    valueList.set(i, attrCiEntityId);
                                } else {
                                    valueList.remove(i);
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setAttrEntityData(attrObj);

            // 解析关系数据
            JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
            //修正新配置项的uuid为id
            if (MapUtils.isNotEmpty(relObj)) {
                for (String key : relObj.keySet()) {
                    JSONObject obj = relObj.getJSONObject(key);
                    JSONArray relDataList = obj.getJSONArray("valueList");
                    if (CollectionUtils.isNotEmpty(relDataList)) {
                        for (int i = 0; i < relDataList.size(); i++) {
                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                            if (relEntityObj.getLong("ciEntityId") == null && StringUtils.isNotBlank(relEntityObj.getString("ciEntityUuid"))) {
                                CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(relEntityObj.getString("ciEntityUuid"));
                                if (tmpVo != null) {
                                    relEntityObj.put("ciEntityId", tmpVo.getCiEntityId());
                                } else {
                                    throw new ApiRuntimeException("找不到" + relEntityObj.getString("ciEntityUuid") + "的新配置项");
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setRelEntityData(relObj);
            ciEntityTransactionList.add(ciEntityTransactionVo);
        }
        if (CollectionUtils.isNotEmpty(ciEntityTransactionList)) {
            Long transactionGroupId = ciEntityService.saveCiEntity(ciEntityTransactionList);
            JSONObject returnObj = new JSONObject();
            returnObj.put("transactionGroupId", transactionGroupId);
            return returnObj;
        }
        return null;
    }

}
