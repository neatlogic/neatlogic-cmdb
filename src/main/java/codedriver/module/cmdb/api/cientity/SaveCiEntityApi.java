package codedriver.module.cmdb.api.cientity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiEntityApi extends PrivateApiComponentBase {

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
        Long id = jsonObj.getLong("id");
        TransactionActionType mode = TransactionActionType.INSERT;
        CiEntityVo ciEntityVo = new CiEntityVo();
        if (id != null) {
            ciEntityVo.setId(id);
            mode = TransactionActionType.UPDATE;
        }
        ciEntityVo.setCiId(jsonObj.getLong("ciId"));
        JSONObject attrObj = jsonObj.getJSONObject("attrEntityData");
        if (MapUtils.isNotEmpty(attrObj)) {
            List<AttrEntityVo> attrEntityList = new ArrayList<>();
            Iterator<String> keys = attrObj.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                AttrEntityVo attrEntityVo = new AttrEntityVo();
                attrEntityVo.setAttrId(Long.parseLong(key));
                JSONArray valueObjList = attrObj.getJSONArray(key);
                attrEntityVo.setValueList(valueObjList.stream().map(v -> v.toString()).collect(Collectors.toList()));
                attrEntityList.add(attrEntityVo);
            }
            ciEntityVo.setAttrEntityList(attrEntityList);
        }
        Long transactionId = ciEntityService.saveCiEntity(ciEntityVo, mode);
        JSONObject returnObj = new JSONObject();
        returnObj.put("transactionId", transactionId);
        if (transactionId > 0) {
            returnObj.put("ciEntityId", ciEntityVo.getId());
        }
        return returnObj;
    }

}
