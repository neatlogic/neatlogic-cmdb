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

package neatlogic.module.cmdb.formattribute.handler;

import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.form.attribute.core.FormHandlerBase;
import neatlogic.framework.form.constvalue.FormConditionModel;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.exception.AttributeValidException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * @author linbq
 * @since 2021/8/24 14:13
 **/
@Component
public class AccountsHandler extends FormHandlerBase {

    private final JSONArray theadList = new JSONArray();

    {
        JSONObject name = new JSONObject();
        name.put("title", "资产名");
        name.put("key", "name");
        theadList.add(name);
        JSONObject ip = new JSONObject();
        ip.put("title", "资产IP");
        ip.put("key", "ip");
        theadList.add(ip);
        JSONObject accountName = new JSONObject();
        accountName.put("title", "帐号");
        accountName.put("key", "accountName");
        theadList.add(accountName);
        JSONObject account = new JSONObject();
        account.put("title", "用户名");
        account.put("key", "account");
        theadList.add(account);
        JSONObject protocol = new JSONObject();
        protocol.put("title", "连接协议");
        protocol.put("key", "protocol");
        theadList.add(protocol);
        JSONObject port = new JSONObject();
        port.put("title", "端口");
        port.put("key", "port");
        theadList.add(port);
    }

    @Override
    public String getHandler() {
        return FormHandler.FORMACCOUNTS.getHandler();
    }

    @Override
    public String getHandlerName() {
        return FormHandler.FORMACCOUNTS.getHandlerName();
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    @Override
    public String getIcon() {
        return "tsfont-group";
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    public String getDataType() {
        return "list";
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public boolean isConditionable() {
        return false;
    }

    @Override
    public boolean isShowable() {
        return true;
    }

    @Override
    public boolean isValueable() {
        return false;
    }

    @Override
    public boolean isFilterable() {
        return false;
    }

    @Override
    public boolean isExtendable() {
        return false;
    }

    @Override
    public boolean isForTemplate() {
        return true;
    }

    @Override
    public boolean isProcessTaskBatchSubmissionTemplateParam() {
        return false;
    }

    @Override
    public String getModule() {
        return "framework";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object conversionDataType(Object source, String attributeLabel) {
        return convertToJSONArray(source, attributeLabel);
    }

    @Override
    public int getSort() {
        return 16;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return "已更新";
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject tableObj = getMyDetailedData(attributeDataVo, configObj);
        tableObj.remove("value");
        return tableObj;
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        return null;
    }

    /*
    表单组件配置信息
    {
        "handler": "formaccounts",
        "reaction": {
            "hide": {},
            "readonly": {},
            "display": {}
        },
        "override_config": {},
        "icon": "tsfont-group",
        "hasValue": true,
        "label": "帐号_1",
        "type": "form",
        "category": "basic",
        "config": {
            "isRequired": false,
            "disableDefaultValue": true,
            "isMask": false,
            "width": "100%",
            "description": "",
            "placeholder": "选择帐号",
            "isHide": false
        },
        "uuid": "d722aa78701c4b4baac595a7dbfa8513"
    }
     */
    /*
    保存数据
    [
        {
            "startPage": 1,
            "accountName": "192.168.0.24_3939_tagent",
            "ip": "192.168.0.83",
            "accountId": 669480417812482,
            "actionType": "创建",
            "_selected": true,
            "protocol": "tagent",
            "protocolId": 478184378212353,
            "port": "3939",
            "name": "prd-gitlab-runner",
            "isSelected": true,
            "fcu": "fccf704231734072a1bf80d90b2d1de2",
            "id": 516341538545669,
            "_selectId": "669480417812482_516341538545669",
            "lcu": "fccf704231734072a1bf80d90b2d1de2"
        }
	]
     */
    /*
    返回数据结构
    {
	    "value": [
            {
                "startPage": 1,
                "accountName": "192.168.0.24_3939_tagent",
                "ip": "192.168.0.83",
                "accountId": 669480417812482,
                "actionType": "创建",
                "_selected": true,
                "protocol": "tagent",
                "protocolId": 478184378212353,
                "port": "3939",
                "name": "prd-gitlab-runner",
                "isSelected": true,
                "fcu": "fccf704231734072a1bf80d90b2d1de2",
                "id": 516341538545669,
                "_selectId": "669480417812482_516341538545669",
                "lcu": "fccf704231734072a1bf80d90b2d1de2"
            }
        ],
        "theadList": [
            {
                "title": "资产名",
                "key": "name"
            },
            {
                "title": "资产IP",
                "key": "ip"
            },
            {
                "title": "帐号名称",
                "key": "accountName"
            },
            {
                "title": "用户名",
                "key": "account"
            },
            {
                "title": "连接协议",
                "key": "protocol"
            },
            {
                "title": "端口",
                "key": "port"
            }
        ],
        "tbodyList": [
            {
                "startPage": 1,
                "accountName": "192.168.0.24_3939_tagent",
                "ip": "192.168.0.83",
                "accountId": 669480417812482,
                "_selected": true,
                "protocol": "tagent",
                "protocolId": 478184378212353,
                "port": "3939",
                "name": "prd-gitlab-runner",
                "isSelected": true,
                "id": 516341538545669,
                "_selectId": "669480417812482_516341538545669"
            }
        ]
    }
     */
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject tableObj = new JSONObject();
        JSONArray valueArray = (JSONArray) attributeDataVo.getDataObj();
        tableObj.put("value", valueArray);
        if (CollectionUtils.isNotEmpty(valueArray)) {
            tableObj.put("theadList", theadList);
            JSONArray tbodyList = new JSONArray();
            for (int i = 0; i < valueArray.size(); i++) {
                JSONObject valueObj = valueArray.getJSONObject(i);
                JSONObject tbodyObj = new JSONObject();
                tbodyObj.putAll(valueObj);
                tbodyObj.remove("actionType");
                tbodyObj.remove("fcu");
                tbodyObj.remove("lcu");
                tbodyObj.remove("fcuVo");
                tbodyObj.remove("lcuVo");
                tbodyList.add(tbodyObj);
            }
            tableObj.put("tbodyList", tbodyList);
        }
        return tableObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject tableObj = new JSONObject();
        JSONArray valueArray = (JSONArray) attributeDataVo.getDataObj();
        tableObj.put("value", valueArray);
        if (CollectionUtils.isNotEmpty(valueArray)) {
            tableObj.put("theadList", theadList);
            JSONArray tbodyList = new JSONArray();
            for (int i = 0; i < valueArray.size(); i++) {
                JSONObject valueObj = valueArray.getJSONObject(i);
                valueObj.remove("actionType");
                valueObj.remove("fcu");
                valueObj.remove("lcu");
                valueObj.remove("fcuVo");
                valueObj.remove("lcuVo");
                Set<Map.Entry<String, Object>> entrySet = valueObj.entrySet();
                JSONObject tbodyObj = new JSONObject();
                for (Map.Entry<String, Object> entry : entrySet) {
                    tbodyObj.put(entry.getKey(), new JSONObject() {
                        {
                            this.put("text", entry.getValue());
                        }
                    });
                }
                tbodyList.add(tbodyObj);
            }
            tableObj.put("tbodyList", tbodyList);
        }
        return tableObj;
    }

    @Override
    public int getExcelHeadLength(JSONObject configObj) {
        return theadList.size();
    }

    @Override
    public int getExcelRowCount(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONArray tbodyList = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(tbodyList)) {
            return tbodyList.size() + 1;
        }
        return 1;
    }
}
