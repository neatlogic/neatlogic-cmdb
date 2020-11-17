package codedriver.module.cmdb.api.cientity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.module.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
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
            if (id == null && StringUtils.isNotBlank(uuid)) {
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
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
                    hasAuth = CiAuthChecker.builder().hasCiEntityUpdatePrivilege(ciId).isInGroup(id, GroupType.MATAIN)
                        .check();
                }
                ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(id);
                ciEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
            } else if (StringUtils.isNotBlank(uuid)) {
                if (!hasAuth) {
                    hasAuth = CiAuthChecker.hasCiEntityInsertPrivilege(ciId);
                }
                ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
                ciEntityTransactionVo.setCiEntityUuid(uuid);
                ciEntityTransactionVo.setTransactionMode(TransactionActionType.INSERT);
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
            if (MapUtils.isNotEmpty(attrObj)) {
                List<AttrEntityTransactionVo> attrEntityList = new ArrayList<>();
                Iterator<String> keys = attrObj.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Long attrId = null;
                    try {
                        attrId = Long.parseLong(key.replace("attr_", ""));
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                    if (attrId != null) {
                        AttrEntityTransactionVo attrEntityVo = new AttrEntityTransactionVo();
                        attrEntityVo.setAttrId(attrId);
                        JSONObject attrDataObj = attrObj.getJSONObject(key);
                        JSONArray valueObjList = attrDataObj.getJSONArray("valueList");
                        attrEntityVo.setActualValueList(
                            valueObjList.stream().map(v -> v.toString()).collect(Collectors.toList()));
                        attrEntityList.add(attrEntityVo);
                    }
                }
                ciEntityTransactionVo.setAttrEntityTransactionList(attrEntityList);
            }
            // 解析关系数据
            JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
            if (MapUtils.isNotEmpty(relObj)) {
                List<RelEntityTransactionVo> relEntityList = new ArrayList<>();
                Iterator<String> keys = relObj.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONArray relDataList = relObj.getJSONArray(key);

                    if (key.startsWith("relfrom_")) {// 当前配置项处于from位置
                        if (CollectionUtils.isNotEmpty(relDataList)) {
                            for (int i = 0; i < relDataList.size(); i++) {
                                JSONObject relEntityObj = relDataList.getJSONObject(i);
                                RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo();
                                relEntityVo.setRelId(Long.parseLong(key.replace("relfrom_", "")));
                                if (relEntityObj.getLong("ciEntityId") != null) {
                                    relEntityVo.setToCiEntityId(relEntityObj.getLong("ciEntityId"));
                                } else if (StringUtils.isNotBlank(relEntityObj.getString("ciEntityUuid"))) {
                                    CiEntityTransactionVo tmpVo =
                                        ciEntityTransactionMap.get(relEntityObj.getString("ciEntityUuid"));
                                    if (tmpVo != null) {
                                        relEntityVo.setToCiEntityId(tmpVo.getCiEntityId());
                                    } else {
                                        throw new ApiRuntimeException(
                                            "找不到" + relEntityObj.getString("ciEntityUuid") + "的新配置项");
                                    }
                                }
                                relEntityVo.setDirection(RelDirectionType.FROM.getValue());
                                relEntityVo.setFromCiEntityId(ciEntityTransactionVo.getCiEntityId());
                                relEntityVo.setAction(RelActionType.INSERT.getValue());// 默认是添加关系
                                relEntityList.add(relEntityVo);
                            }
                        }
                    } else if (key.startsWith("relto_")) {// 当前配置项处于to位置
                        if (CollectionUtils.isNotEmpty(relDataList)) {
                            for (int i = 0; i < relDataList.size(); i++) {
                                JSONObject relEntityObj = relDataList.getJSONObject(i);
                                RelEntityTransactionVo relEntityVo = new RelEntityTransactionVo();
                                relEntityVo.setRelId(Long.parseLong(key.replace("relto_", "")));
                                if (relEntityObj.getLong("ciEntityId") != null) {
                                    relEntityVo.setFromCiEntityId(relEntityObj.getLong("ciEntityId"));
                                } else if (StringUtils.isNotBlank(relEntityObj.getString("ciEntityUuid"))) {
                                    CiEntityTransactionVo tmpVo =
                                        ciEntityTransactionMap.get(relEntityObj.getString("ciEntityUuid"));
                                    if (tmpVo != null) {
                                        relEntityVo.setFromCiEntityId(tmpVo.getCiEntityId());
                                    } else {
                                        throw new ApiRuntimeException(
                                            "找不到" + relEntityObj.getString("ciEntityUuid") + "的新配置项");
                                    }
                                }
                                relEntityVo.setDirection(RelDirectionType.TO.getValue());
                                relEntityVo.setToCiEntityId(ciEntityTransactionVo.getCiEntityId());
                                relEntityVo.setAction(RelActionType.INSERT.getValue());// 默认是添加关系
                                relEntityList.add(relEntityVo);
                            }
                        }
                    }
                }
                ciEntityTransactionVo.setRelEntityTransactionList(relEntityList);
            }
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
