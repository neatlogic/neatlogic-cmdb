package codedriver.module.cmdb.api.cientity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.cmdb.constvalue.RelActionType;
import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.RelEntityTransactionVo;
import codedriver.module.cmdb.exception.cientity.CiEntityAuthException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiEntityApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(SaveCiEntityApi.class);

    @Autowired
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/save";
    }

    @Override
    public String getName() {
        return "保存配置项";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "id", type = ApiParamType.LONG, desc = "配置项id，不存在代表添加"),
        @Param(name = "attrEntityData", type = ApiParamType.JSONOBJECT, desc = "属性数据"),
        @Param(name = "relEntityData", type = ApiParamType.JSONOBJECT, desc = "关系数据")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "配置项id")})
    @Description(desc = "保存配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long id = jsonObj.getLong("id");
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY");
        if (!hasAuth) {
            // 拥有模型管理权限允许添加或修改配置项
            hasAuth = CiAuthChecker.hasCiManagePrivilege(ciId);
        }
        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
        if (id != null) {
            if (!hasAuth) {
                hasAuth =
                    CiAuthChecker.builder().hasCiEntityUpdatePrivilege(ciId).isInGroup(id, GroupType.MATAIN).check();
            }
            ciEntityTransactionVo.setCiEntityId(id);
            ciEntityTransactionVo.setTransactionMode(TransactionActionType.UPDATE);
        } else {
            if (!hasAuth) {
                hasAuth = CiAuthChecker.hasCiEntityInsertPrivilege(ciId);
            }
            ciEntityTransactionVo.setTransactionMode(TransactionActionType.INSERT);
        }

        if (!hasAuth) {
            throw new CiEntityAuthException(ciEntityTransactionVo.getTransactionMode().getText());
        }

        ciEntityTransactionVo.setCiId(ciId);
        // 解析属性数据
        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
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
                    attrEntityVo
                        .setActualValueList(valueObjList.stream().map(v -> v.toString()).collect(Collectors.toList()));
                    attrEntityList.add(attrEntityVo);
                }
            }
            ciEntityTransactionVo.setAttrEntityTransactionList(attrEntityList);
        }
        // 解析关系数据
        JSONObject relObj = jsonObj.getJSONObject("relEntityData");
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
                            relEntityVo.setToCiEntityId(relEntityObj.getLong("ciEntityId"));
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
                            relEntityVo.setFromCiEntityId(relEntityObj.getLong("ciEntityId"));
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

        Long transactionId = ciEntityService.saveCiEntity(ciEntityTransactionVo);
        JSONObject returnObj = new JSONObject();
        returnObj.put("transactionId", transactionId);
        if (transactionId > 0) {
            returnObj.put("ciEntityId", ciEntityTransactionVo.getCiEntityId());
        }
        return returnObj;
    }

}
