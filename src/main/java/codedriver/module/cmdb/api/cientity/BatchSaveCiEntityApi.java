package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
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
            ciEntityTransactionVo.setAttrEntityData(attrObj);
            // 解析关系数据
            JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
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
