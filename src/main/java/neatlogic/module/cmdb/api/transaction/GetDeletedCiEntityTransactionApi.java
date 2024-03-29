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

package neatlogic.module.cmdb.api.transaction;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeletedCiEntityTransactionApi extends PrivateApiComponentBase {

    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public String getToken() {
        return "/cmdb/deletedcientitytransaction/get";
    }

    @Override
    public String getName() {
        return "获取已删除配置项事务详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "transactionId", isRequired = true, type = ApiParamType.LONG, desc = "事务id")})
    @Output({@Param(name = "transaction", explode = TransactionVo.class, desc = "事务信息"),
            @Param(name = "detail", type = ApiParamType.JSONARRAY, desc = "详细修改信息")})
    @Description(desc = "获取已删除配置项事务详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long transactionId = jsonObj.getLong("transactionId");
        TransactionVo transactionVo = transactionMapper.getTransactionById(transactionId);
        CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
        ciEntityTransactionVo.restoreSnapshot();
        JSONArray dataList = new JSONArray();
        JSONObject oldCiEntityObj = JSONObject.parseObject(ciEntityTransactionVo.getSnapshot());

        if (MapUtils.isNotEmpty(oldCiEntityObj)) {
            JSONObject attrData = oldCiEntityObj.getJSONObject("attrEntityData");
            if (MapUtils.isNotEmpty(attrData)) {
                for (String key : attrData.keySet()) {
                    JSONObject dataObj = new JSONObject();
                    //如果属性已删除，尝试使用snapshot数据还原原来的值
                    AttrVo attrVo = JSONObject.toJavaObject(attrData.getJSONObject(key), AttrVo.class);
                    dataObj.put("oldValue", buildAttrObj(attrVo, attrData.getJSONObject(key).getJSONArray("valueList")));
                    dataObj.put("action", "delattr");
                    dataObj.put("id", attrVo.getId());
                    dataObj.put("name", attrVo.getName());
                    dataObj.put("label", attrVo.getLabel());
                    dataObj.put("type", "attr");
                    dataList.add(dataObj);
                }
            }
            JSONObject relData = oldCiEntityObj.getJSONObject("relEntityData");
            if (MapUtils.isNotEmpty(relData)) {
                for (String key : relData.keySet()) {
                    JSONObject dataObj = new JSONObject();
                    JSONObject oldRelEntityData = relData.getJSONObject(key);
                    dataObj.put("oldValue", oldRelEntityData.getJSONArray("valueList"));
                    dataObj.put("id", oldRelEntityData.getLong("relId"));
                    dataObj.put("name", oldRelEntityData.getString("name"));
                    dataObj.put("label", oldRelEntityData.getString("label"));
                    dataObj.put("direction", oldRelEntityData.getString("direction"));
                    dataObj.put("type", "rel");
                    dataList.add(dataObj);
                }
            }
        }
        JSONObject returnObj = new JSONObject();
        returnObj.put("transaction", transactionVo);
        returnObj.put("detail", dataList);
        return returnObj;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    private JSONObject buildAttrObj(AttrVo attrVo, JSONArray valueList) {
        JSONObject attrObj = new JSONObject();
        attrObj.put("type", attrVo.getType());
        attrObj.put("name", attrVo.getName());
        attrObj.put("label", attrVo.getLabel());
        attrObj.put("config", attrVo.getConfig(true));//克隆一个config对象，避免json序列化出错
        attrObj.put("targetCiId", attrVo.getTargetCiId());
        attrObj.put("valueList", valueList);
        attrObj.put("actualValueList", AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList));
        return attrObj;
    }

}
