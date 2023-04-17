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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CiEntityModifyHandler extends FormHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(CiEntityModifyHandler.class);

    @Override
    public String getHandler() {
        return FormHandler.FORMCIENTITYMODIFY.getHandler();
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
        return null;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        if (!attributeDataVo.dataIsEmpty()) {
            return "已更新";
        } else {
            return "";
        }
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = getMyDetailedData(attributeDataVo, configObj);
        if (MapUtils.isEmpty(resultObj)) {
            return null;
        }
        resultObj.remove("value");
        JSONArray tbodyArray = resultObj.getJSONArray("tbodyList");
        if (CollectionUtils.isEmpty(tbodyArray)) {
            return null;
        }
        List<Map<String, String>> tbodyList = new ArrayList<>();
        for (int i = 0; i < tbodyArray.size(); i++) {
            JSONObject tbodyObj = tbodyArray.getJSONObject(i);
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : tbodyObj.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof JSONObject) {
                    JSONObject jsonObj = (JSONObject) value;
                    JSONArray actualValueList = jsonObj.getJSONArray("actualValueList");
                    if (CollectionUtils.isEmpty(actualValueList)) {
                        map.put(entry.getKey(), "");
                        continue;
                    }
                    for (int j = 0; j < actualValueList.size(); j++) {
                        Object actualValue = actualValueList.get(j);
                        if (actualValue instanceof String) {
                            map.put(entry.getKey(), (String) actualValue);
                        } else if (actualValue instanceof JSONObject) {
                            JSONObject actualValueObj = (JSONObject) actualValue;
                            if (MapUtils.isEmpty(actualValueObj)) {
                                map.put(entry.getKey(), "");
                            } else {
                                map.put(entry.getKey(), actualValueObj.getString("text"));
                            }
                        }
                    }
                }
            }
            tbodyList.add(map);
        }
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        return null;
    }

    @Override
    public ParamType getParamType() {
        return null;
    }

    @Override
    public boolean isConditionable() {
        return false;
    }

    @Override
    public boolean isProcessTaskBatchSubmissionTemplateParam() {
        return false;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    /*
    //表单组件配置信息
    {
        "disableDefaultValue": true,
        "isMask": false,
        "actionDel": true,
        "ciIdList": [
            479609502048256
        ],
        "width": "100%",
        "description": "",
        "actionEdit": false,
        "actionAdd": false,
        "dataConfig": [
            {
                "fromCiId": 481537078337536,
                "isEdit": false,
                "title": "编号",
                "key": "attr_481537078337536",
                "isShow": true
            },
            {
                "fromCiId": 480848331464705,
                "isEdit": false,
                "title": "简称",
                "key": "attr_480848331464705",
                "isShow": true
            },
            {
                "fromCiId": 478701787553792,
                "isEdit": false,
                "title": "名称",
                "key": "attr_478701787553792",
                "isShow": true
            },
            {
                "fromCiId": 478702852907008,
                "isEdit": false,
                "title": "使用状态",
                "key": "attr_478702852907008",
                "isShow": true
            },
            {
                "fromCiId": 480857726705664,
                "isEdit": false,
                "title": "负责人领导",
                "key": "attr_480857726705664",
                "isShow": true
            },
            {
                "fromCiId": 480858834001920,
                "isEdit": false,
                "title": "业务负责人",
                "key": "attr_480858834001920",
                "isShow": false
            },
            {
                "fromCiId": 480859505090560,
                "isEdit": false,
                "title": "业务负责人领导",
                "key": "attr_480859505090560",
                "isShow": false
            },
            {
                "fromCiId": 480860184567808,
                "isEdit": false,
                "title": "所属开发中心",
                "key": "attr_480860184567808",
                "isShow": false
            },
            {
                "fromCiId": 480860595609600,
                "isEdit": false,
                "title": "所属业务部门",
                "key": "attr_480860595609600",
                "isShow": false
            },
            {
                "fromCiId": 480870116679680,
                "isEdit": false,
                "title": "开发类型",
                "key": "attr_480870116679680",
                "isShow": false
            },
            {
                "fromCiId": 480869848244224,
                "isEdit": false,
                "title": "供应商",
                "key": "attr_480869848244224",
                "isShow": false
            },
            {
                "fromCiId": 480870410280963,
                "isEdit": false,
                "title": "上线时间",
                "key": "attr_480870410280963",
                "isShow": false
            },
            {
                "fromCiId": 478703658213376,
                "isEdit": false,
                "title": "事业部",
                "key": "attr_478703658213376",
                "isShow": false
            },
            {
                "fromCiId": 478703406555136,
                "isEdit": false,
                "title": "负责人",
                "key": "attr_478703406555136",
                "isShow": false
            },
            {
                "fromCiId": 479638602129408,
                "isEdit": false,
                "title": "应用模块",
                "key": "relfrom_479638602129408",
                "isShow": false
            },
            {
                "fromCiId": 478702072766464,
                "isEdit": false,
                "title": "备注",
                "key": "attr_478702072766464",
                "isShow": false
            },
            {
                "fromCiId": 493359185321984,
                "isEdit": false,
                "title": "我调用的应用系统",
                "key": "relfrom_493359185321984",
                "isShow": false
            },
            {
                "fromCiId": 493359185321984,
                "isEdit": false,
                "title": "调用我的应用系统",
                "key": "relto_493359185321984",
                "isShow": false
            },
            {
                "fromCiId": 529435476156416,
                "isEdit": false,
                "title": "系统显示名",
                "key": "attr_529435476156416",
                "isShow": false
            },
            {
                "fromCiId": 532533548474368,
                "isEdit": false,
                "title": "test附件",
                "key": "attr_532533548474368",
                "isShow": false
            },
            {
                "fromCiId": 655562282688512,
                "isEdit": false,
                "title": "连接",
                "key": "attr_655562282688512",
                "isShow": false
            },
            {
                "fromCiId": 684521024184320,
                "isEdit": false,
                "title": "投产时段",
                "key": "attr_684521024184320",
                "isShow": false
            },
            {
                "fromCiId": 691144643895296,
                "isEdit": false,
                "title": "维护窗口",
                "key": "attr_691144643895296",
                "isShow": false
            },
            {
                "fromCiId": 756298140147712,
                "isEdit": false,
                "title": "test_range",
                "key": "attr_756298140147712",
                "isShow": false
            }
        ],
        "isHide": false,
        "ciId": 479609502048256
    }
     */
    /*
    //保存数据结构
    [
        {
            "ciIcon": "tsfont-app",
            "rootCiId": 479609502048256,
            "typeName": "应用系统",
            "type": 441079325270016,
            "inspectStatus": "",
            "uuid": "7ba5e269295a450ea4e7d2c184480eff",
            "ciName": "APP",
            "ciId": 479609502048256,
            "renewTime": "2022-12-20 14:59",
            "_selected": true,
            "actionType": "delete",
            "maxRelEntityCount": 3,
            "relEntityData": {
                "relfrom_479638602129408": {
                    "relId": 479638602129408,
                    "ciEntityId": 639089665433601,
                    "valueList": [
                        {
                            "ciEntityId": 676653491347457,
                            "id": 676653491347464,
                            "ciEntityName": "测试模块",
                            "ciId": 479610550624256
                        }
                    ],
                    "name": "APPComponent",
                    "label": "应用模块",
                    "direction": "from",
                    "ciId": 479610550624256
                }
            },
            "_expander": false,
            "name": "IMAP系统",
            "isSelected": true,
            "attrEntityData": {
                "attr_478701787553792": {
                    "ciEntityId": 639089665433601,
                    "attrId": 478701787553792,
                    "actualValueList": [
                        "IMAP系统"
                    ],
                    "valueList": [
                        "IMAP系统"
                    ],
                    "name": "name",
                    "label": "名称",
                    "type": "text",
                    "ciId": 441087512551424
                },
                "attr_480848331464705": {
                    "ciEntityId": 639089665433601,
                    "attrId": 480848331464705,
                    "actualValueList": [
                        "IMAP"
                    ],
                    "valueList": [
                        "IMAP"
                    ],
                    "name": "abbrName",
                    "label": "简称",
                    "type": "text",
                    "ciId": 479609502048256
                },
                "attr_529435476156416": {
                    "ciEntityId": 639089665433601,
                    "attrId": 529435476156416,
                    "actualValueList": [
                        "IMAP[IMAP系统]"
                    ],
                    "valueList": [
                        "IMAP[IMAP系统]"
                    ],
                    "name": "systemDisplayName",
                    "label": "系统显示名",
                    "type": "expression",
                    "config": {
                        "expression": [
                            "{480848331464705}",
                            "[",
                            "{478701787553792}",
                            "]"
                        ]
                    },
                    "ciId": 479609502048256
                },
                "attr_481537078337536": {
                    "ciEntityId": 639089665433601,
                    "attrId": 481537078337536,
                    "actualValueList": [
                        "1"
                    ],
                    "valueList": [
                        "1"
                    ],
                    "name": "applicationCode",
                    "label": "编号",
                    "type": "text",
                    "ciId": 479609502048256
                }
            },
            "id": 639089665433601,
            "maxAttrEntityCount": 3,
            "ciLabel": "应用系统",
            "monitorStatus": ""
        }
    ]
     */
//返回数据结构
    /*
    {
        "ciId": "479609502048256",
        "ciLabel": "应用系统",
        "ciName": "APP",
        "value": 原始数据,
        "theadList": [
            {
                "title": "操作类型",
                "key": "actionType"
            },
            {
                "title": "编号",
                "key": "attr_481537078337536"
            },
            {
                "title": "简称",
                "key": "attr_480848331464705"
            },
            {
                "title": "名称",
                "key": "attr_478701787553792"
            },
            {
                "title": "使用状态",
                "key": "attr_478702852907008"
            },
            {
                "title": "负责人领导",
                "key": "attr_480857726705664"
            },
            {
                "title": "业务负责人",
                "key": "attr_480858834001920"
            },
            {
                "title": "业务负责人领导",
                "key": "attr_480859505090560"
            },
            {
                "title": "所属开发中心",
                "key": "attr_480860184567808"
            },
            {
                "title": "所属业务部门",
                "key": "attr_480860595609600"
            },
            {
                "title": "开发类型",
                "key": "attr_480870116679680"
            },
            {
                "title": "供应商",
                "key": "attr_480869848244224"
            },
            {
                "title": "上线时间",
                "key": "attr_480870410280963"
            },
            {
                "title": "事业部",
                "key": "attr_478703658213376"
            },
            {
                "title": "负责人",
                "key": "attr_478703406555136"
            },
            {
                "title": "应用模块",
                "key": "relfrom_479638602129408"
            },
            {
                "title": "备注",
                "key": "attr_478702072766464"
            },
            {
                "title": "我调用的应用系统",
                "key": "relfrom_493359185321984"
            },
            {
                "title": "调用我的应用系统",
                "key": "relto_493359185321984"
            },
            {
                "title": "系统显示名",
                "key": "attr_529435476156416"
            },
            {
                "title": "test附件",
                "key": "attr_532533548474368"
            },
            {
                "title": "连接",
                "key": "attr_655562282688512"
            },
            {
                "title": "投产时段",
                "key": "attr_684521024184320"
            },
            {
                "title": "维护窗口",
                "key": "attr_691144643895296"
            },
            {
                "title": "test_range",
                "key": "attr_756298140147712"
            }
        ],
        "tbodyList": [
            {
                "attr_655562282688512": {
                    "ciEntityId": 639091091496961,
                    "attrId": 655562282688512,
                    "actualValueList": [
                        "Http://www.baidu.com"
                    ],
                    "valueList": [
                        "Http://www.baidu.com"
                    ],
                    "name": "link",
                    "label": "连接",
                    "type": "hyperlink",
                    "config": {
                        "text": "工作台",
                        "type": "outterlink"
                    },
                    "ciId": 479609502048256
                },
                "actionType": {
                    "actualValueList": [
                        "删除"
                    ],
                    "valueList": [
                        "delete"
                    ],
                    "type": "text"
                },
                "attr_480848331464705": {
                    "ciEntityId": 639091091496961,
                    "attrId": 480848331464705,
                    "actualValueList": [
                        "UPBS"
                    ],
                    "valueList": [
                        "UPBS"
                    ],
                    "name": "abbrName",
                    "label": "简称",
                    "type": "text",
                    "ciId": 479609502048256
                },
                "attr_478701787553792": {
                    "ciEntityId": 639091091496961,
                    "attrId": 478701787553792,
                    "actualValueList": [
                        "统一支付系统"
                    ],
                    "valueList": [
                        "统一支付系统"
                    ],
                    "name": "name",
                    "label": "名称",
                    "type": "text",
                    "ciId": 441087512551424
                },
                "attr_529435476156416": {
                    "ciEntityId": 639091091496961,
                    "attrId": 529435476156416,
                    "actualValueList": [
                        "UPBS[统一支付系统]"
                    ],
                    "valueList": [
                        "UPBS[统一支付系统]"
                    ],
                    "name": "systemDisplayName",
                    "label": "系统显示名",
                    "type": "expression",
                    "config": {
                        "expression": [
                            "{480848331464705}",
                            "[",
                            "{478701787553792}",
                            "]"
                        ]
                    },
                    "ciId": 479609502048256
                },
                "attr_481537078337536": {
                    "ciEntityId": 639091091496961,
                    "attrId": 481537078337536,
                    "actualValueList": [
                        "8"
                    ],
                    "valueList": [
                        "8"
                    ],
                    "name": "applicationCode",
                    "label": "编号",
                    "type": "text",
                    "ciId": 479609502048256
                },
                "relfrom_479638602129408": {
                    "relId": 479638602129408,
                    "ciEntityId": 639091091496961,
                    "valueList": [
                        {
                            "ciEntityId": 481894994862179,
                            "id": 663530613039115,
                            "ciEntityName": "交易反欺诈引擎&数据抽取",
                            "ciId": 479610550624256
                        }
                    ],
                    "name": "APPComponent",
                    "label": "应用模块",
                    "direction": "from",
                    "ciId": 479610550624256
                }
            }
        ]
    }
     */
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONArray theadList = new JSONArray();
        JSONObject actionObj = new JSONObject();
        actionObj.put("key", "actionType");
        actionObj.put("title", "操作类型");
        theadList.add(actionObj);
        List<String> keyList = new ArrayList<>();
        JSONArray dataConfig = configObj.getJSONArray("dataConfig");
        if (CollectionUtils.isNotEmpty(dataConfig)) {
            //计算勾选显示属性个数，如不勾选任何属性则按照模型显示设置中的配置显示相关属性
            getTheadList(theadList, keyList, dataConfig);
        }
        resultObj.put("theadList", theadList);
        JSONArray dataList = (JSONArray) attributeDataVo.getDataObj();
        resultObj.put("value", dataList);
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(dataList)) {
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject dataObj = dataList.getJSONObject(i);
                if (MapUtils.isNotEmpty(dataObj)) {
                    if (i == 0) {
                        resultObj.put("ciId", dataObj.getString("ciId"));
                        resultObj.put("ciName", dataObj.getString("ciName"));
                        resultObj.put("ciLabel", dataObj.getString("ciLabel"));
                    }
                    String actionType = dataObj.getString("actionType");
                    List<String> valueList = new ArrayList<>();
                    valueList.add(actionType);
                    List<String> actualValueList = new ArrayList<>();
                    String actualValue = "";
                    if ("insert".equals(actionType)) {
                        actualValue = "新增";
                    } else if ("update".equals(actionType)) {
                        actualValue = "编辑";
                    } else if ("delete".equals(actionType)) {
                        actualValue = "删除";
                    }
                    actualValueList.add(actualValue);
                    JSONObject actionTypeObj = new JSONObject();
                    actionTypeObj.put("type", "text");
                    actionTypeObj.put("valueList", valueList);
                    actionTypeObj.put("actualValueList", actualValueList);
                    JSONObject tbodyObj = new JSONObject();
                    tbodyObj.put("actionType", actionTypeObj);
                    JSONObject attrEntityData = dataObj.getJSONObject("attrEntityData");
                    if (MapUtils.isNotEmpty(attrEntityData)) {
                        for (String key : keyList) {
                            JSONObject value = attrEntityData.getJSONObject(key);
                            if (MapUtils.isNotEmpty(value)) {
                                tbodyObj.put(key, value);
                            }
                        }
                    }
                    JSONObject relEntityData = dataObj.getJSONObject("relEntityData");
                    if (MapUtils.isNotEmpty(relEntityData)) {
                        for (String key : keyList) {
                            JSONObject value = relEntityData.getJSONObject(key);
                            if (MapUtils.isNotEmpty(value)) {
                                tbodyObj.put(key, relEntityData.getJSONObject(key));
                            }
                        }
                    }
                    tbodyList.add(tbodyObj);
                }
            }
        }
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONArray theadList = new JSONArray();
        JSONObject actionObj = new JSONObject();
        actionObj.put("key", "actionType");
        actionObj.put("title", "操作类型");
        theadList.add(actionObj);
        List<String> keyList = new ArrayList<>();
        JSONArray dataConfig = configObj.getJSONArray("dataConfig");
        if (CollectionUtils.isNotEmpty(dataConfig)) {
            getTheadList(theadList, keyList, dataConfig);
        }
        resultObj.put("theadList", theadList);
        JSONArray dataList = (JSONArray) attributeDataVo.getDataObj();
        resultObj.put("value", dataList);
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(dataList)) {
            for (int i = 0; i < dataList.size(); i++) {
                JSONObject dataObj = dataList.getJSONObject(i);
                if (MapUtils.isNotEmpty(dataObj)) {
                    String actionType = dataObj.getString("actionType");
                    List<String> actualValueList = new ArrayList<>();
                    String actualValue = "";
                    if ("insert".equals(actionType)) {
                        actualValue = "新增";
                    } else if ("update".equals(actionType)) {
                        actualValue = "编辑";
                    } else if ("delete".equals(actionType)) {
                        actualValue = "删除";
                    }
                    actualValueList.add(actualValue);
                    JSONObject tbodyObj = new JSONObject();
                    tbodyObj.put("actionType", new JSONObject() {
                        {
                            this.put("text", String.join(",", actualValueList));
                        }
                    });
                    JSONObject attrEntityData = dataObj.getJSONObject("attrEntityData");
                    if (MapUtils.isNotEmpty(attrEntityData)) {
                        for (String key : keyList) {
                            JSONObject object = attrEntityData.getJSONObject(key);
                            if (object != null) {
                                JSONArray valueList = object.getJSONArray("actualValueList");
                                tbodyObj.put(key, new JSONObject() {
                                    {
                                        this.put("text", valueList != null ? String.join(",", valueList.toJavaList(String.class)) : "");
                                    }
                                });
                            }
                        }
                    }
                    JSONObject relEntityData = dataObj.getJSONObject("relEntityData");
                    if (MapUtils.isNotEmpty(relEntityData)) {
                        for (String key : keyList) {
                            JSONObject object = relEntityData.getJSONObject(key);
                            if (object != null) {
                                JSONArray valueList = object.getJSONArray("valueList");
                                if (CollectionUtils.isNotEmpty(valueList)) {
                                    List<String> entityNameList = new ArrayList<>();
                                    for (int j = 0; j < valueList.size(); j++) {
                                        JSONObject obj = valueList.getJSONObject(j);
                                        String ciEntityName = obj.getString("ciEntityName");
                                        if (ciEntityName != null) {
                                            entityNameList.add(ciEntityName);
                                        }
                                    }
                                    tbodyObj.put(key, new JSONObject() {
                                        {
                                            this.put("text", entityNameList.size() > 0 ? String.join(",", entityNameList) : "");
                                        }
                                    });
                                }

                            }
                        }
                    }
                    tbodyList.add(tbodyObj);
                }
            }
        }
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }

    private void getTheadList(JSONArray theadList, List<String> keyList, JSONArray dataConfig) {
        //计算勾选显示属性个数，如不勾选任何属性则按照模型显示设置中的配置显示相关属性
        int isShowTrueCount = getIsShowTrueCount(dataConfig);
        for (int i = 0; i < dataConfig.size(); i++) {
            JSONObject dataObj = dataConfig.getJSONObject(i);
            if (MapUtils.isNotEmpty(dataObj)) {
                if (isShowTrueCount > 0) {
                    Boolean isShow = dataObj.getBoolean("isShow");
                    if (!Objects.equals(isShow, true)) {
                        continue;
                    }
                }
                String key = dataObj.getString("key");
                String title = dataObj.getString("title");
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(title)) {
                    JSONObject theadObj = new JSONObject();
                    theadObj.put("key", key);
                    theadObj.put("title", title);
                    theadList.add(theadObj);
                    keyList.add(key);
                }
            }
        }
    }

    private int getIsShowTrueCount(JSONArray dataConfig) {
        int isShowTrueCount = 0;
        for (int i = 0; i < dataConfig.size(); i++) {
            JSONObject dataObj = dataConfig.getJSONObject(i);
            if (MapUtils.isNotEmpty(dataObj)) {
                Boolean isShow = dataObj.getBoolean("isShow");
                if (Objects.equals(isShow, true)) {
                    isShowTrueCount++;
                }
            }
        }
        return isShowTrueCount;
    }

    @Override
    public int getExcelHeadLength(JSONObject configObj) {
        int count = 0;
        JSONArray dataConfig = configObj.getJSONArray("dataConfig");
        if (CollectionUtils.isNotEmpty(dataConfig)) {
            //计算勾选显示属性个数，如不勾选任何属性则按照模型显示设置中的配置显示相关属性
            int isShowTrueCount = getIsShowTrueCount(dataConfig);
            for (int i = 0; i < dataConfig.size(); i++) {
                JSONObject dataObj = dataConfig.getJSONObject(i);
                if (MapUtils.isNotEmpty(dataObj)) {
                    if (isShowTrueCount > 0) {
                        Boolean isShow = dataObj.getBoolean("isShow");
                        if (!Objects.equals(isShow, true)) {
                            continue;
                        }
                    }
                    String key = dataObj.getString("key");
                    String title = dataObj.getString("title");
                    if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(title)) {
                        count++;
                    }
                }
            }
            count++;
        }
        if (count == 0) {
            count++;
        }
        return count;
    }

    @Override
    public int getExcelRowCount(AttributeDataVo attributeDataVo, JSONObject configObj) {
        int count = 1;
        JSONArray dataList = (JSONArray) attributeDataVo.getDataObj();
        for (int i = 0; i < dataList.size(); i++) {
            JSONObject dataObj = dataList.getJSONObject(i);
            if (MapUtils.isNotEmpty(dataObj)) {
                count++;
            }
        }
        return count;
    }
}
