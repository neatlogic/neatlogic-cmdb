package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.cmdb.crossover.ISearchCiEntityApiCrossoverService;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CiEntitySyncHandler extends FormHandlerBase {
    private final static Logger logger = LoggerFactory.getLogger(CiEntitySyncHandler.class);
    @Override
    public String getHandler() {
        return "cientityselect";
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
        JSONArray tableArray = new JSONArray();
        JSONArray dataArray = (JSONArray) attributeDataVo.getDataObj();
        if (CollectionUtils.isNotEmpty(dataArray)) {
            JSONObject paramObj = new JSONObject();
            paramObj.put("currentPage", 1);
            paramObj.put("needPage", false);
            paramObj.put("mode", "dialog");
            paramObj.put("needActionType", true);
            List<Long> ciIdList = new ArrayList<>();
            Map<Long, String> ciLabelMap = new HashMap<>();
            Map<Long, List<CiEntityVo>> ciEntityListMap = new HashMap<>();
            List<CiEntityVo> ciEntityVoList = dataArray.toJavaList(CiEntityVo.class);
            for (CiEntityVo ciEntityVo : ciEntityVoList) {
                Long ciId = ciEntityVo.getCiId();
                if (!ciIdList.contains(ciId)) {
                    ciIdList.add(ciId);
                    ciLabelMap.put(ciId, ciEntityVo.getCiLabel());
                }
                ciEntityListMap.computeIfAbsent(ciId, key -> new ArrayList<>()).add(ciEntityVo);
            }
            for (Long ciId : ciIdList) {
                paramObj.put("ciId", ciId);
                paramObj.put("ciEntityList", ciEntityListMap.get(ciId));
                try {
                    ISearchCiEntityApiCrossoverService searchCiEntityApiCrossoverService = CrossoverServiceFactory.getApi(ISearchCiEntityApiCrossoverService.class);
                    JSONObject tableObj = (JSONObject) searchCiEntityApiCrossoverService.myDoService(paramObj);
                    JSONArray tbodyList = new JSONArray();
                    JSONArray tbodyArray = tableObj.getJSONArray("tbodyList");
                    for (int i = 0; i < tbodyArray.size(); i++) {
                        JSONObject tbodyObj = tbodyArray.getJSONObject(i);
                        String actionType = tbodyObj.getString("actionType");
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
                        JSONObject attrEntityData = tbodyObj.getJSONObject("attrEntityData");
                        attrEntityData.put("actionType", actionTypeObj);
                        tbodyList.add(attrEntityData);
                    }
                    tableObj.put("tbodyList", tbodyList);
                    tableObj.put("ciId", ciId);
                    tableObj.put("ciLabel", ciLabelMap.get(ciId));
                    tableArray.add(tableObj);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return tableArray;
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        return null;
    }

    @Override
    public String getHandlerName() {
        return "配置项修改组件";
    }

    @Override
    public String getIcon() {
        return "tsfont-cientityselect";
    }

    @Override
    public ParamType getParamType() {
        return null;
    }

    @Override
    public String getDataType() {
        return null;
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
        return true;
    }

    @Override
    public String getModule() {
        return "cmdb";
    }

    @Override
    public boolean isForTemplate() {
        return false;
    }

    @Override
    public int getSort() {
        return 10;
    }

    @Override
    public boolean isAudit() {
        return true;
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }
//表单组件配置信息
//    {
//        "handler": "cientityselect",
//        "label": "配置项修改组件_2",
//        "type": "form",
//        "uuid": "aba0a1afd21443d49e77258827cafef2",
//        "config": {
//            "isRequired": false,
//            "actionDel": false,
//            "actionEdit": false,
//            "actionAdd": true,
//            "ciIdList": [
//                483279895322624
//		    ],
//            "ciId": 483279895322624,
//            "ruleList": [],
//            "validList": [],
//            "quoteUuid": "",
//            "dataConfig": [
//                {
//                    "fromCiId": 478701787553792,
//                    "isEdit": true,
//                    "title": "名称",
//                    "key": "attr_478701787553792",
//                    "isShow": true
//                },
//                {
//                    "fromCiId": 486022852698112,
//                    "isEdit": true,
//                    "title": "IP地址",
//                    "key": "attr_486022852698112",
//                    "isShow": true
//                },
//                {
//                    "fromCiId": 446084220461056,
//                    "isEdit": true,
//                    "title": "IP列表",
//                    "key": "attr_446084220461056",
//                    "isShow": true
//                },
//                {
//                    "fromCiId": 478816971530240,
//                    "isEdit": true,
//                    "title": "端口",
//                    "key": "attr_478816971530240",
//                    "isShow": true
//                },
//                {
//                    "fromCiId": 481541616574464,
//                    "isEdit": false,
//                    "title": "服务",
//                    "key": "attr_481541616574464",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483219136634880,
//                    "isEdit": false,
//                    "title": "运行用户",
//                    "key": "attr_483219136634880",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 478702852907008,
//                    "isEdit": false,
//                    "title": "使用状态",
//                    "key": "attr_478702852907008",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 446087097753600,
//                    "isEdit": false,
//                    "title": "网络区域",
//                    "key": "attr_446087097753600",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 446087349411840,
//                    "isEdit": false,
//                    "title": "数据中心",
//                    "key": "attr_446087349411840",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 478703406555136,
//                    "isEdit": false,
//                    "title": "负责人",
//                    "key": "attr_478703406555136",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 478703658213376,
//                    "isEdit": false,
//                    "title": "事业部",
//                    "key": "attr_478703658213376",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 478818439536640,
//                    "isEdit": false,
//                    "title": "应用环境",
//                    "key": "attr_478818439536640",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479603923623936,
//                    "isEdit": false,
//                    "title": "管理端口",
//                    "key": "attr_479603923623936",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479604200448000,
//                    "isEdit": false,
//                    "title": "SSL端口",
//                    "key": "attr_479604200448000",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479604485660672,
//                    "isEdit": false,
//                    "title": "管理SSL端口",
//                    "key": "attr_479604485660672",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479604779261952,
//                    "isEdit": false,
//                    "title": "监控端口",
//                    "key": "attr_479604779261952",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479605257412608,
//                    "isEdit": false,
//                    "title": "安装目录",
//                    "key": "attr_479605257412608",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479605517459456,
//                    "isEdit": false,
//                    "title": "配置目录",
//                    "key": "attr_479605517459456",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 480816840630272,
//                    "isEdit": false,
//                    "title": "维护窗口",
//                    "key": "attr_480816840630272",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483223792312320,
//                    "isEdit": false,
//                    "title": "版本",
//                    "key": "attr_483223792312320",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483270466527232,
//                    "isEdit": false,
//                    "title": "JAVA_HOME",
//                    "key": "attr_483270466527232",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483270600744960,
//                    "isEdit": false,
//                    "title": "JAVA版本",
//                    "key": "attr_483270600744960",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483270785294336,
//                    "isEdit": false,
//                    "title": "JVM类型",
//                    "key": "attr_483270785294336",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483271154393088,
//                    "isEdit": false,
//                    "title": "JVM版本",
//                    "key": "attr_483271154393088",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483271364108288,
//                    "isEdit": false,
//                    "title": "最小Heap Size",
//                    "key": "attr_483271364108288",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483271540269056,
//                    "isEdit": false,
//                    "title": "最大Heap Size",
//                    "key": "attr_483271540269056",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483271800315904,
//                    "isEdit": false,
//                    "title": "JMX端口",
//                    "key": "attr_483271800315904",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483275113816064,
//                    "isEdit": false,
//                    "title": "JMX开启SSL服务",
//                    "key": "attr_483275113816064",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483280457359360,
//                    "isEdit": false,
//                    "title": "Catalina家目录",
//                    "key": "attr_483280457359360",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483280969064448,
//                    "isEdit": false,
//                    "title": "Catalina根目录",
//                    "key": "attr_483280969064448",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 479639013171200,
//                    "isEdit": false,
//                    "title": "应用模块",
//                    "key": "relto_479639013171200",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483307393179648,
//                    "isEdit": false,
//                    "title": "应用实例集群",
//                    "key": "relfrom_483307393179648",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483311948193795,
//                    "isEdit": false,
//                    "title": "操作系统",
//                    "key": "relfrom_483311948193795",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 478702072766464,
//                    "isEdit": false,
//                    "title": "备注",
//                    "key": "attr_478702072766464",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 483303886741504,
//                    "isEdit": false,
//                    "title": "我调用的DB实例",
//                    "key": "relfrom_483303886741504",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 493369620750336,
//                    "isEdit": false,
//                    "title": "我调用的DB集群",
//                    "key": "relfrom_493369620750336",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 493368320516096,
//                    "isEdit": false,
//                    "title": "我调用的应用实例",
//                    "key": "relfrom_493368320516096",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 493368320516096,
//                    "isEdit": false,
//                    "title": "调用我的应用实例",
//                    "key": "relto_493368320516096",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 493373194297344,
//                    "isEdit": false,
//                    "title": "我调用的应用实例集群",
//                    "key": "relfrom_493373194297344",
//                    "isShow": false
//                },
//                {
//                    "fromCiId": 542441266274304,
//                    "isEdit": false,
//                    "title": "命令行",
//                    "key": "attr_542441266274304",
//                    "isShow": false
//                }
//            ],
//            "defaultValueType": "self",
//            "width": "100%",
//            "authorityConfig": [
//                "common#alluser"
//            ]
//        }
//    }
//保存数据结构
//[
//	{
//		"rootCiId": 483279895322624,
//		"ciIcon": "tsfont-tomcat",
//		"authData": {
//			"cientityupdate": true,
//			"cientityinsert": true,
//			"cimanage": true,
//			"transactionmanage": true
//		},
//		"_elementList": [
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "CIRoot",
//					"ciId": 441087512551424,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 478701787553792,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "名称",
//					"sort": 1,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "文本框",
//					"name": "name",
//					"ciLabel": "CI根对象"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "IPObject",
//					"ciId": 442011534499840,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 486022852698112,
//					"isRequired": 1,
//					"canInput": true,
//					"isCiUnique": 1,
//					"canImport": true,
//					"label": "IP地址",
//					"sort": 2,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "文本框",
//					"name": "ip",
//					"ciLabel": "IP软硬件"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"targetIsVirtual": 0,
//					"isUnique": 0,
//					"needTargetCi": true,
//					"isPrivate": 1,
//					"type": "select",
//					"canSearch": true,
//					"ciName": "IPObject",
//					"ciId": 442011534499840,
//					"inputTypeText": "自动发现",
//					"allowEdit": 0,
//					"inputType": "at",
//					"id": 446084220461056,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "IP列表",
//					"sort": 3,
//					"targetCiId": 446083406766080,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "下拉框",
//					"name": "all_ip",
//					"ciLabel": "IP软硬件",
//					"config": {
//						"mode": "rw"
//					}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "SoftwareService",
//					"ciId": 478816686317568,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 478816971530240,
//					"isRequired": 1,
//					"canInput": true,
//					"isCiUnique": 1,
//					"canImport": true,
//					"label": "端口",
//					"sort": 4,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "文本框",
//					"name": "port",
//					"ciLabel": "软件服务"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "expression",
//					"canSearch": true,
//					"ciName": "SoftwareService",
//					"ciId": 478816686317568,
//					"inputTypeText": "人工录入",
//					"allowEdit": 0,
//					"inputType": "mt",
//					"id": 481541616574464,
//					"isRequired": 0,
//					"canInput": false,
//					"isCiUnique": 0,
//					"canImport": false,
//					"label": "服务",
//					"sort": 5,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "表达式",
//					"name": "display_name",
//					"ciLabel": "软件服务",
//					"config": {
//						"expression": [
//							"{486022852698112}",
//							":",
//							"{478816971530240}"
//						]
//					}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483219136634880,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "运行用户",
//					"sort": 6,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "os_user",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"targetIsVirtual": 0,
//					"isUnique": 0,
//					"needTargetCi": true,
//					"isPrivate": 1,
//					"type": "select",
//					"canSearch": true,
//					"ciName": "CIRoot",
//					"ciId": 441087512551424,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 478702852907008,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "使用状态",
//					"sort": 7,
//					"targetCiId": 479550169423872,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "下拉框",
//					"name": "state",
//					"ciLabel": "CI根对象",
//					"config": {}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "IPObject",
//					"ciId": 442011534499840,
//					"inputTypeText": "自动发现",
//					"allowEdit": 0,
//					"inputType": "at",
//					"id": 446087097753600,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "网络区域",
//					"sort": 8,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "文本框",
//					"name": "network_area",
//					"ciLabel": "IP软硬件"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"targetIsVirtual": 0,
//					"isUnique": 0,
//					"needTargetCi": true,
//					"isPrivate": 1,
//					"type": "select",
//					"canSearch": true,
//					"ciName": "IPObject",
//					"ciId": 442011534499840,
//					"inputTypeText": "人工录入",
//					"allowEdit": 0,
//					"inputType": "mt",
//					"id": 446087349411840,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "数据中心",
//					"sort": 9,
//					"targetCiId": 441135981928448,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "下拉框",
//					"name": "data_center",
//					"ciLabel": "IP软硬件",
//					"config": {}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"targetIsVirtual": 1,
//					"isUnique": 0,
//					"needTargetCi": true,
//					"isPrivate": 1,
//					"type": "select",
//					"canSearch": true,
//					"ciName": "CIRoot",
//					"ciId": 441087512551424,
//					"inputTypeText": "人工录入",
//					"allowEdit": 1,
//					"inputType": "mt",
//					"id": 478703406555136,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "负责人",
//					"sort": 10,
//					"targetCiId": 479643459133440,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "下拉框",
//					"name": "owner",
//					"ciLabel": "CI根对象",
//					"config": {
//						"mode": "r",
//						"isMultiple": 1
//					}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"targetIsVirtual": 0,
//					"isUnique": 0,
//					"needTargetCi": true,
//					"isPrivate": 1,
//					"type": "select",
//					"canSearch": true,
//					"ciName": "CIRoot",
//					"ciId": 441087512551424,
//					"inputTypeText": "人工录入",
//					"allowEdit": 0,
//					"inputType": "mt",
//					"id": 478703658213376,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "事业部",
//					"sort": 11,
//					"targetCiId": 480125644709888,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "下拉框",
//					"name": "business_group",
//					"ciLabel": "CI根对象",
//					"config": {
//						"mode": "r"
//					}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"targetIsVirtual": 0,
//					"isUnique": 0,
//					"needTargetCi": true,
//					"isPrivate": 1,
//					"type": "select",
//					"canSearch": true,
//					"ciName": "SoftwareService",
//					"ciId": 478816686317568,
//					"inputTypeText": "人工录入",
//					"allowEdit": 1,
//					"inputType": "mt",
//					"id": 478818439536640,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "应用环境",
//					"sort": 12,
//					"targetCiId": 479551469658112,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "下拉框",
//					"name": "app_environment",
//					"ciLabel": "软件服务",
//					"config": {}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 0,
//					"inputType": "at",
//					"id": 479603923623936,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "管理端口",
//					"sort": 13,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "admin_port",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 479604200448000,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "SSL端口",
//					"sort": 14,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "ssl_port",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 0,
//					"inputType": "at",
//					"id": 479604485660672,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "管理SSL端口",
//					"sort": 15,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "admin_ssl_port",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 479604779261952,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "监控端口",
//					"sort": 16,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "mon_port",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 479605257412608,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "安装目录",
//					"sort": 17,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "install_path",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 479605517459456,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "配置目录",
//					"sort": 18,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "config_path",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": true,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "date",
//					"canSearch": true,
//					"ciName": "CIRoot",
//					"ciId": 441087512551424,
//					"inputTypeText": "人工录入",
//					"allowEdit": 1,
//					"inputType": "mt",
//					"id": 480816840630272,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "维护窗口",
//					"sort": 19,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "日期",
//					"name": "maintenance_window",
//					"ciLabel": "CI根对象",
//					"config": {
//						"format": "HH:mm",
//						"type": "timerange"
//					}
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483223792312320,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "版本",
//					"sort": 20,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "version",
//					"ciLabel": "应用实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483270466527232,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "JAVA_HOME",
//					"sort": 22,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "java_home",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483270600744960,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "JAVA版本",
//					"sort": 23,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "java_version",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483270785294336,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "JVM类型",
//					"sort": 24,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "jvm_type",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483271154393088,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "JVM版本",
//					"sort": 25,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "jvm_versoin",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483271364108288,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "最小Heap Size",
//					"sort": 26,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "min_heap_size",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483271540269056,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "最大Heap Size",
//					"sort": 27,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "max_heap_size",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483271800315904,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "JMX端口",
//					"sort": 28,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "jmx_port",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Java",
//					"ciId": 483270063874052,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483275113816064,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "JMX开启SSL服务",
//					"sort": 29,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "jmx_ssl",
//					"ciLabel": "JAVA类应用"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Tomcat",
//					"ciId": 483279895322624,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483280457359360,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "Catalina家目录",
//					"sort": 30,
//					"isExtended": 0,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "catalina_home",
//					"ciLabel": "Tomcat实例"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "Tomcat",
//					"ciId": 483279895322624,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 483280969064448,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "Catalina根目录",
//					"sort": 31,
//					"isExtended": 0,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本框",
//					"name": "catalina_base",
//					"ciLabel": "Tomcat实例"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-os",
//					"toIsRequired": 0,
//					"fromRule": "O",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用模块",
//					"fromLabel": "应用模块",
//					"toCiId": 479603630022656,
//					"fromAllowInsert": true,
//					"toName": "APPIns",
//					"allowEdit": 1,
//					"toIsUnique": 1,
//					"fromName": "APPComponent",
//					"inputType": "at",
//					"id": 479639013171200,
//					"fromIsVirtual": 0,
//					"toCiLabel": "应用实例",
//					"direction": "to",
//					"toLabel": "应用实例",
//					"toRule": "N",
//					"fromCiId": 479610550624256,
//					"toIsVirtual": 0,
//					"fromRuleText": "一个",
//					"toRuleText": "多个",
//					"fromCiIcon": "tsfont-lun",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "APPIns",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "成员",
//					"typeId": 479637670993920,
//					"fromCiName": "APPComponent"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-db-ins",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "应用实例",
//					"toCiId": 479606037553152,
//					"toName": "APPInsCluster",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "APPIns",
//					"inputType": "at",
//					"id": 483307393179648,
//					"fromIsVirtual": 0,
//					"toCiLabel": "应用实例集群",
//					"direction": "from",
//					"toLabel": "应用实例集群",
//					"toRule": "O",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toAllowInsert": true,
//					"toRuleText": "一个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "APPInsCluster",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "集群成员",
//					"typeId": 479637209620480,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-os",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "应用实例",
//					"toCiId": 479593471418368,
//					"toName": "OS",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "APPIns",
//					"inputType": "at",
//					"id": 483311948193795,
//					"fromIsVirtual": 0,
//					"toCiLabel": "操作系统",
//					"direction": "from",
//					"toLabel": "操作系统",
//					"toRule": "O",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toAllowInsert": true,
//					"toRuleText": "一个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "OS",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "运行于",
//					"typeId": 479636689526784,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"startPage": 1,
//					"needWholeRow": false,
//					"isUnique": 0,
//					"needTargetCi": false,
//					"isPrivate": 1,
//					"type": "text",
//					"canSearch": true,
//					"ciName": "CIRoot",
//					"ciId": 441087512551424,
//					"inputTypeText": "自动发现",
//					"allowEdit": 1,
//					"inputType": "at",
//					"id": 478702072766464,
//					"isRequired": 0,
//					"canInput": true,
//					"isCiUnique": 0,
//					"canImport": true,
//					"label": "备注",
//					"sort": 38,
//					"isExtended": 1,
//					"expressionList": [
//						{
//							"text": "等于",
//							"value": "equal"
//						},
//						{
//							"text": "不等于",
//							"value": "notequal"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"groupName": "",
//					"typeText": "文本框",
//					"name": "description",
//					"ciLabel": "CI根对象"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-datacenter",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "调用我的应用实例",
//					"toCiId": 479596491317248,
//					"toName": "to_dbins",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "from_appins",
//					"inputType": "at",
//					"id": 483303886741504,
//					"fromIsVirtual": 0,
//					"toCiLabel": "DB实例",
//					"direction": "from",
//					"toLabel": "我调用的DB实例",
//					"toRule": "N",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toAllowInsert": true,
//					"toRuleText": "多个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "DBIns",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "物理调用",
//					"typeId": 493363790667776,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-db-ins",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "调用我的应用实例",
//					"toCiId": 479598143873024,
//					"toName": "to_db_cluster",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "from_appins",
//					"inputType": "mt",
//					"id": 493369620750336,
//					"fromIsVirtual": 0,
//					"toCiLabel": "DB集群",
//					"direction": "from",
//					"toLabel": "我调用的DB集群",
//					"toRule": "N",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toAllowInsert": true,
//					"toRuleText": "多个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "DBCluster",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "物理调用",
//					"typeId": 493363790667776,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-os",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "调用我的应用实例",
//					"toCiId": 479603630022656,
//					"toName": "to_appins",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "from_appins",
//					"inputType": "mt",
//					"id": 493368320516096,
//					"fromIsVirtual": 0,
//					"toCiLabel": "应用实例",
//					"direction": "from",
//					"toLabel": "我调用的应用实例",
//					"toRule": "N",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toAllowInsert": true,
//					"toRuleText": "多个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "APPIns",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "物理调用",
//					"typeId": 493363790667776,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-os",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "调用我的应用实例",
//					"toCiId": 479603630022656,
//					"fromAllowInsert": true,
//					"toName": "to_appins",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "from_appins",
//					"inputType": "mt",
//					"id": 493368320516096,
//					"fromIsVirtual": 0,
//					"toCiLabel": "应用实例",
//					"direction": "to",
//					"toLabel": "我调用的应用实例",
//					"toRule": "N",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toRuleText": "多个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "APPIns",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "物理调用",
//					"typeId": 493363790667776,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "rel",
//				"element": {
//					"toCiIcon": "tsfont-db-ins",
//					"toIsRequired": 0,
//					"fromRule": "N",
//					"fromIsRequired": 0,
//					"fromCiLabel": "应用实例",
//					"fromLabel": "调用我的应用实例",
//					"toCiId": 479606037553152,
//					"toName": "to_appins_cluster",
//					"allowEdit": 1,
//					"toIsUnique": 0,
//					"fromName": "from_appins",
//					"inputType": "mt",
//					"id": 493373194297344,
//					"fromIsVirtual": 0,
//					"toCiLabel": "应用实例集群",
//					"direction": "from",
//					"toLabel": "我调用的应用实例集群",
//					"toRule": "O",
//					"fromCiId": 479603630022656,
//					"toIsVirtual": 0,
//					"fromRuleText": "多个",
//					"toAllowInsert": true,
//					"toRuleText": "一个",
//					"fromCiIcon": "tsfont-os",
//					"isExtended": 1,
//					"fromIsUnique": 0,
//					"toCiName": "APPInsCluster",
//					"expressionList": [
//						{
//							"text": "在此区间",
//							"value": "between"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						}
//					],
//					"typeText": "物理调用",
//					"typeId": 493363790667776,
//					"fromCiName": "APPIns"
//				}
//			},
//			{
//				"type": "attr",
//				"element": {
//					"needConfig": false,
//					"isRequired": 0,
//					"startPage": 1,
//					"canInput": true,
//					"isCiUnique": 0,
//					"needWholeRow": true,
//					"isUnique": 0,
//					"canImport": true,
//					"needTargetCi": false,
//					"isPrivate": 0,
//					"label": "命令行",
//					"sort": 999999,
//					"isExtended": 1,
//					"type": "textarea",
//					"canSearch": true,
//					"ciName": "APPIns",
//					"ciId": 479603630022656,
//					"inputTypeText": "自动发现",
//					"expressionList": [
//						{
//							"text": "包含",
//							"value": "like"
//						},
//						{
//							"text": "不包含",
//							"value": "notlike"
//						},
//						{
//							"text": "不为空",
//							"value": "is-not-null"
//						},
//						{
//							"text": "为空",
//							"value": "is-null"
//						}
//					],
//					"typeText": "文本域",
//					"name": "command",
//					"inputType": "at",
//					"id": 542441266274304,
//					"ciLabel": "应用实例"
//				}
//			}
//		],
//		"uuid": "fd9eed9d45af40bbbf1d6f0720b51e6d",
//		"ciName": "Tomcat",
//		"ciId": 483279895322624,
//		"actionType": "insert",
//		"maxRelEntityCount": 9999999999,
//		"relEntityData": {},
//		"_expander": false,
//		"attrEntityData": {
//			"attr_486022852698112": {
//				"actualValueList": [
//					"192.168.0.1"
//				],
//				"valueList": [
//					"192.168.0.1"
//				],
//				"type": "text"
//			},
//			"attr_478702852907008": {
//				"valueList": [],
//				"type": "select",
//				"config": {}
//			},
//			"attr_483270785294336": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483223792312320": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483271154393088": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_479605517459456": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_478702072766464": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_478816971530240": {
//				"actualValueList": [
//					"1000"
//				],
//				"valueList": [
//					"1000"
//				],
//				"type": "text"
//			},
//			"attr_446084220461056": {
//				"actualValueList": [
//					"192.168.0.65"
//				],
//				"valueList": [
//					484847801655299
//				],
//				"type": "select",
//				"config": {
//					"mode": "rw"
//				}
//			},
//			"attr_478703658213376": {
//				"valueList": [],
//				"type": "select",
//				"config": {
//					"mode": "r"
//				}
//			},
//			"attr_479605257412608": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483275113816064": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483270600744960": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483280457359360": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_446087349411840": {
//				"valueList": [],
//				"type": "select",
//				"config": {}
//			},
//			"attr_478701787553792": {
//				"actualValueList": [
//					"a"
//				],
//				"valueList": [
//					"a"
//				],
//				"type": "text"
//			},
//			"attr_483271364108288": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_478818439536640": {
//				"valueList": [],
//				"type": "select",
//				"config": {}
//			},
//			"attr_483219136634880": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483271540269056": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483280969064448": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483270466527232": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_479603923623936": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_480816840630272": {
//				"valueList": [],
//				"type": "date",
//				"config": {
//					"format": "HH:mm",
//					"type": "timerange"
//				}
//			},
//			"attr_479604485660672": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_446087097753600": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_479604779261952": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_483271800315904": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_478703406555136": {
//				"valueList": [],
//				"type": "select",
//				"config": {
//					"mode": "r",
//					"isMultiple": 1
//				}
//			},
//			"attr_479604200448000": {
//				"valueList": [],
//				"type": "text"
//			},
//			"attr_542441266274304": {
//				"valueList": [],
//				"type": "textarea"
//			}
//		},
//		"maxAttrEntityCount": 9999999999,
//		"ciLabel": "Tomcat实例"
//	}
//]
//返回数据结构
//{
//	"value": 原始数据,
//	"ciId": 483279895322624,
//	"ciName": "Tomcat",
//	"ciLabel": "Tomcat实例",
//	"theadList": [
//		{
//			"title": "操作",
//			"key": "actionType"
//		},
//		{
//			"title": "名称",
//			"key": "attr_478701787553792"
//		},
//		{
//			"title": "IP地址",
//			"key": "attr_486022852698112"
//		},
//		{
//			"title": "IP列表",
//			"key": "attr_446084220461056"
//		},
//		{
//			"title": "端口",
//			"key": "attr_478816971530240"
//		}
//	],
//	"tbodyList": [
//		{
//			"actionType": {
//				"actualValueList": [
//					"新增"
//				],
//				"valueList": [
//					"insert"
//				],
//				"type": "text"
//			},
//			"attr_478701787553792": {
//				"actualValueList": [
//					"a"
//				],
//				"valueList": [
//					"a"
//				],
//				"type": "text"
//			},
//			"attr_486022852698112": {
//				"actualValueList": [
//					"192.168.0.1"
//				],
//				"valueList": [
//					"192.168.0.1"
//				],
//				"type": "text"
//			},
//			"attr_446084220461056": {
//				"actualValueList": [
//					"192.168.0.65"
//				],
//				"valueList": [
//					484847801655299
//				],
//				"type": "select",
//				"config": {
//					"mode": "rw"
//				}
//			},
//			"attr_478816971530240": {
//				"actualValueList": [
//					"1000"
//				],
//				"valueList": [
//					"1000"
//				],
//				"type": "text"
//			}
//		}
//	]
//}
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
            for (int i = 0; i < dataConfig.size(); i++) {
                JSONObject dataObj = dataConfig.getJSONObject(i);
                if (MapUtils.isNotEmpty(dataObj)) {
                    Boolean isShow = dataObj.getBoolean("isShow");
                    if (Objects.equals(isShow, true)) {
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
                            tbodyObj.put(key, attrEntityData.getJSONObject(key));
                        }
                    }
                    tbodyList.add(tbodyObj);
                }
            }
        }
        resultObj.put("tbodyList", tbodyList);
        return resultObj;
    }
}
