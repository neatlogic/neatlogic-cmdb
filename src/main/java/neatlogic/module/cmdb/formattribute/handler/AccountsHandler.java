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
import org.apache.commons.collections4.MapUtils;
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
        accountName.put("title", "帐号名称");
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
    public int getSort() {
        return 16;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return "已更新";
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject tableObj = new JSONObject();
        // TODO 由于表单重构，数据结构已发生变化，暂时屏蔽些代码使得工单处理触发邮件时不报错，已建需求【ID1007302】重构表单邮件模板及取数逻辑，后面统一测试表单所有组件的邮件模板显示
//        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
//        if (MapUtils.isNotEmpty(dataObj)) {
//            JSONArray tbodyList = dataObj.getJSONArray("selectedDataList");
//            if (CollectionUtils.isNotEmpty(tbodyList)) {
//                tableObj.put("theadList", theadList);
//                tableObj.put("tbodyList", tbodyList);
//            }
//        }
        return tableObj;
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        return null;
    }

    //表单组件配置信息
//    {
//        "handler": "formaccounts",
//        "label": "帐号组件_1",
//        "type": "form",
//        "uuid": "79ea2bc150cf4d6396882f66e07850b4",
//        "config": {
//            "isRequired": false,
//            "ruleList": [],
//            "width": "100%",
//            "validList": [],
//            "quoteUuid": "",
//            "defaultValueType": "self",
//            "placeholder": "选择帐号组件",
//            "authorityConfig": [
//                "common#alluser"
//            ]
//        }
//    }
//保存数据
//    {
//        "selectedDataList": [
//            {
//                "id": 493223793189019,
//                "name": "Mysql",
//                "ip": "192.168.1.140",
//                "accountId": 502566202695680,
//                "account": "root",
//                "accountName": "mysql_ro_勿动",
//                "protocolId": 478219996241920,
//                "protocol": "database",
//                "port": "3306",
//                "tempId": 3,
//                "_selected": true,
//                "actionType": "创建",
//                "fcu": "fccf704231734072a1bf80d90b2d1de2",
//                "lcu": "fccf704231734072a1bf80d90b2d1de2",
//                "fcuVo": {
//                    "roleUuidList": [],
//                    "startPage": 1,
//                    "userAuthList": [],
//                    "teamUuidList": [],
//                    "initType": "user",
//                    "roleList": [],
//                    "uuid": "fccf704231734072a1bf80d90b2d1de2",
//                    "teamRoleList": [],
//                    "pinyin": "",
//                    "teamNameList": [],
//                    "id": 564355489644556,
//                    "teamList": [],
//                    "isMaintenanceMode": 0
//                },
//                "lcuVo": {
//                    "roleUuidList": [],
//                    "startPage": 1,
//                    "userAuthList": [],
//                    "teamUuidList": [],
//                    "initType": "user",
//                    "roleList": [],
//                    "uuid": "fccf704231734072a1bf80d90b2d1de2",
//                    "teamRoleList": [],
//                    "pinyin": "",
//                    "teamNameList": [],
//                    "id": 564355489644557,
//                    "teamList": [],
//                    "isMaintenanceMode": 0
//                }
//            }
//        ]
//    }
//返回数据结构
//    {
//        "value": 原始数据,
//        "theadList": [
//            {
//                "key": "name",
//                    "title": "资产名"
//            },
//            {
//                "key": "ip",
//                    "title": "资产IP"
//            },
//            {
//                "key": "account",
//                    "title": "帐号名称"
//            },
//            {
//                "key": "accountName",
//                    "title": "用户名"
//            },
//            {
//                "key": "protocol",
//                    "title": "连接协议"
//            },
//            {
//                "key": "port",
//                    "title": "端口"
//            }
//        ],
//        "tbodyList": [
//            {
//                "id": 493223793189019,
//                    "name": "Mysql",
//                    "ip": "192.168.1.140",
//                    "accountId": 502566202695680,
//                    "account": "root",
//                    "accountName": "mysql_ro_勿动",
//                    "protocolId": 478219996241920,
//                    "protocol": "database",
//                    "port": "8080",
//                    "tempId": 3,
//                    "_selected": true
//            }
//        ]
//    }
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject tableObj = new JSONObject();
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        tableObj.put("value", dataObj);
        if (MapUtils.isNotEmpty(dataObj)) {
            JSONArray tbodyList = dataObj.getJSONArray("selectedDataList");
            if (CollectionUtils.isNotEmpty(tbodyList)) {
                tableObj.put("theadList", theadList);
                for (int i = 0; i < tbodyList.size(); i++) {
                    JSONObject tbodyObj = tbodyList.getJSONObject(i);
                    tbodyObj.remove("actionType");
                    tbodyObj.remove("fcu");
                    tbodyObj.remove("lcu");
                    tbodyObj.remove("fcuVo");
                    tbodyObj.remove("lcuVo");
                }
                tableObj.put("tbodyList", tbodyList);
            }
        }
        return tableObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject tableObj = new JSONObject();
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        tableObj.put("value", dataObj);
        if (MapUtils.isNotEmpty(dataObj)) {
            JSONArray selectedDataList = dataObj.getJSONArray("selectedDataList");
            if (CollectionUtils.isNotEmpty(selectedDataList)) {
                tableObj.put("theadList", theadList);
                JSONArray tbodyList = new JSONArray();
                for (int i = 0; i < selectedDataList.size(); i++) {
                    JSONObject tbodyObj = selectedDataList.getJSONObject(i);
                    tbodyObj.remove("actionType");
                    tbodyObj.remove("fcu");
                    tbodyObj.remove("lcu");
                    tbodyObj.remove("fcuVo");
                    tbodyObj.remove("lcuVo");
                    Set<Map.Entry<String, Object>> entrySet = tbodyObj.entrySet();
                    JSONObject obj = new JSONObject();
                    for (Map.Entry<String, Object> entry : entrySet) {
                        obj.put(entry.getKey(), new JSONObject() {
                            {
                                this.put("text", entry.getValue());
                            }
                        });
                    }
                    tbodyList.add(obj);
                }
                tableObj.put("tbodyList", tbodyList);
            }
        }
        return tableObj;
    }

    @Override
    public int getExcelHeadLength(JSONObject configObj) {
        return theadList.size();
    }

    @Override
    public int getExcelRowCount(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        if (MapUtils.isNotEmpty(dataObj)) {
            JSONArray tbodyList = dataObj.getJSONArray("selectedDataList");
            if (CollectionUtils.isNotEmpty(tbodyList)) {
                return tbodyList.size() + 1;
            }
        }
        return 1;
    }
}
