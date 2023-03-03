package neatlogic.module.cmdb.formattribute.handler;

import neatlogic.framework.cmdb.crossover.ISearchCiEntityApiCrossoverService;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
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

    //表单组件配置信息
//{
//	"handler": "cientityselect",
//	"label": "配置项修改组件_1",
//	"type": "form",
//	"uuid": "89ca9dfd69e74599a60705abfdb20d83",
//	"config": {
//		"isRequired": false,
//		"actionDel": false,
//		"ciIdList": [
//			567085713113088
//		],
//		"ruleList": [],
//		"validList": [],
//		"quoteUuid": "",
//		"dataConfig": [
//			{
//				"fromCiId": 478701787553792,
//				"isEdit": false,
//				"title": "名称",
//				"key": "attr_478701787553792",
//				"isShow": true
//			},
//			{
//				"fromCiId": 478702072766464,
//				"isEdit": false,
//				"title": "备注",
//				"key": "attr_478702072766464",
//				"isShow": true
//			},
//			{
//				"fromCiId": 478702852907008,
//				"isEdit": false,
//				"title": "使用状态",
//				"key": "attr_478702852907008",
//				"isShow": true
//			},
//			{
//				"fromCiId": 478703406555136,
//				"isEdit": false,
//				"title": "负责人",
//				"key": "attr_478703406555136",
//				"isShow": true
//			},
//			{
//				"fromCiId": 478703658213376,
//				"isEdit": false,
//				"title": "事业部",
//				"key": "attr_478703658213376",
//				"isShow": true
//			},
//			{
//				"fromCiId": 480816840630272,
//				"isEdit": false,
//				"title": "维护窗口",
//				"key": "attr_480816840630272",
//				"isShow": true
//			},
//			{
//				"fromCiId": 567087072067584,
//				"isEdit": false,
//				"title": "数字属性",
//				"key": "attr_567087072067584",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567089035001856,
//				"isEdit": false,
//				"title": "文本域属性",
//				"key": "attr_567089035001856",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567090637225984,
//				"isEdit": false,
//				"title": "枚举属性",
//				"key": "attr_567090637225984",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567090997936128,
//				"isEdit": false,
//				"title": "日期属性",
//				"key": "attr_567090997936128",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567091249594368,
//				"isEdit": false,
//				"title": "时间属性",
//				"key": "attr_567091249594368",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567091685801984,
//				"isEdit": false,
//				"title": "日期时间属性",
//				"key": "attr_567091685801984",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567095360012288,
//				"isEdit": false,
//				"title": "日期时间范围属性",
//				"key": "attr_567095360012288",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567095871717376,
//				"isEdit": false,
//				"title": "密码属性",
//				"key": "attr_567095871717376",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567096148541440,
//				"isEdit": false,
//				"title": "附件属性",
//				"key": "attr_567096148541440",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567097792708608,
//				"isEdit": false,
//				"title": "表格",
//				"key": "attr_567097792708608",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567098648346624,
//				"isEdit": false,
//				"title": "表达式属性",
//				"key": "attr_567098648346624",
//				"isShow": false
//			},
//			{
//				"fromCiId": 567101106208768,
//				"isEdit": false,
//				"title": "超链接属性",
//				"key": "attr_567101106208768",
//				"isShow": false
//			}
//		],
//		"ciId": 567085713113088,
//		"width": "100%",
//		"actionEdit": false,
//		"actionAdd": true,
//		"defaultValueType": "self",
//		"authorityConfig": [
//			"common#alluser"
//		]
//	}
//}
//保存数据结构
//[
//	{
//		"rootCiId": 567085713113088,
//		"ciIcon": "tsfont-ci",
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
//					"isCiUnique": 1,
//					"canImport": true,
//					"label": "名称",
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
//					"groupName": "",
//					"typeText": "文本框",
//					"name": "description",
//					"ciLabel": "CI根对象"
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
//					"sort": 6,
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
//					"sort": 7,
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
//					"sort": 8,
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
//					"sort": 9,
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
//			}
//		],
//		"description": "test",
//		"uuid": "ae05d235b6904faa92b0e1857da89e31",
//		"ciName": "other",
//		"ciId": 567085713113088,
//		"actionType": "insert",
//		"maxRelEntityCount": 9999999999,
//		"relEntityData": {},
//		"_expander": false,
//		"attrEntityData": {
//			"attr_567095871717376": {
//				"actualValueList": [
//					"qwer"
//				],
//				"valueList": [
//					"qwer"
//				],
//				"type": "password"
//			},
//			"attr_478701787553792": {
//				"actualValueList": [
//					"名称2"
//				],
//				"valueList": [
//					"名称2"
//				],
//				"type": "text"
//			},
//			"attr_567089035001856": {
//				"actualValueList": [
//					"嗯嗯"
//				],
//				"valueList": [
//					"嗯嗯"
//				],
//				"type": "textarea"
//			},
//			"attr_478702852907008": {
//				"actualValueList": [
//					"使用中"
//				],
//				"valueList": [
//					481855425798147
//				],
//				"type": "select",
//				"config": {}
//			},
//			"attr_567090637225984": {
//				"actualValueList": [
//					"枚举2"
//				],
//				"valueList": [
//					"枚举2"
//				],
//				"type": "enum",
//				"config": {
//					"members": [
//						"枚举1",
//						"枚举2"
//					]
//				}
//			},
//			"attr_567091249594368": {
//				"actualValueList": [
//					"00:00:05"
//				],
//				"valueList": [
//					"00:00:05"
//				],
//				"type": "time"
//			},
//			"attr_567101106208768": {
//				"actualValueList": [
//					"http://localhost:8081/develop/process.html#/task-dispatch?uuid=fd782fe015d8463dae9440e9d07498c6"
//				],
//				"valueList": [
//					"http://localhost:8081/develop/process.html#/task-dispatch?uuid=fd782fe015d8463dae9440e9d07498c6"
//				],
//				"type": "hyperlink",
//				"config": {
//					"text": "内部链接",
//					"type": "innerlink"
//				}
//			},
//			"attr_567087072067584": {
//				"actualValueList": [
//					"1"
//				],
//				"valueList": [
//					"1"
//				],
//				"type": "number"
//			},
//			"attr_478702072766464": {
//				"actualValueList": [
//					"备注2"
//				],
//				"valueList": [
//					"备注2"
//				],
//				"type": "text"
//			},
//			"attr_480816840630272": {
//				"actualValueList": [
//					"2022-02-21"
//				],
//				"valueList": [
//					"2022-02-21"
//				],
//				"type": "date",
//				"config": {
//					"format": "HH:mm",
//					"type": "timerange"
//				}
//			},
//			"attr_567090997936128": {
//				"actualValueList": [
//					"2022-02-21"
//				],
//				"valueList": [
//					"2022-02-21"
//				],
//				"type": "date",
//				"config": {}
//			},
//			"attr_478703658213376": {
//				"actualValueList": [
//					"集团"
//				],
//				"valueList": [
//					481860601569283
//				],
//				"type": "select",
//				"config": {
//					"mode": "r"
//				}
//			},
//			"attr_567095360012288": {
//				"actualValueList": [
//					"2022-02-21 00:02,2022-03-21 00:03"
//				],
//				"valueList": [
//					"2022-02-21 00:02,2022-03-21 00:03"
//				],
//				"type": "datetimerange",
//				"config": {
//					"format": "yyyy-MM-dd HH:mm",
//					"type": "datetimerange"
//				}
//			},
//			"attr_478703406555136": {
//				"actualValueList": [
//					{
//						"text": "测试用户(test01)",
//						"value": 521740908240896
//					}
//				],
//				"valueList": [
//					521740908240896
//				],
//				"type": "select",
//				"config": {
//					"mode": "r",
//					"isMultiple": 1
//				}
//			},
//			"attr_567091685801984": {
//				"actualValueList": [
//					"2022-02-21 00:00:08"
//				],
//				"valueList": [
//					"2022-02-21 00:00:08"
//				],
//				"type": "datetime"
//			},
//			"attr_567096148541440": {
//				"actualValueList": [
//					{
//						"name": "asd.jpg",
//						"id": 567118537736192
//					}
//				],
//				"valueList": [
//					567118537736192
//				],
//				"type": "file",
//				"config": {}
//			}
//		},
//		"maxAttrEntityCount": 9999999999,
//		"ciLabel": "其他"
//	}
//]
//返回数据结构
//{
//   "value": 原始数据
//	"ciLabel": "其他",
//	"ciName": "other",
//	"theadList": [
//		{
//			"title": "操作类型",
//			"key": "actionType"
//		},
//		{
//			"title": "名称",
//			"key": "attr_478701787553792"
//		},
//		{
//			"title": "备注",
//			"key": "attr_478702072766464"
//		},
//		{
//			"title": "使用状态",
//			"key": "attr_478702852907008"
//		},
//		{
//			"title": "负责人",
//			"key": "attr_478703406555136"
//		},
//		{
//			"title": "事业部",
//			"key": "attr_478703658213376"
//		},
//		{
//			"title": "维护窗口",
//			"key": "attr_480816840630272"
//		}
//	],
//	"ciId": "567085713113088",
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
//					"名称2"
//				],
//				"valueList": [
//					"名称2"
//				],
//				"type": "text"
//			},
//			"attr_478702072766464": {
//				"actualValueList": [
//					"备注2"
//				],
//				"valueList": [
//					"备注2"
//				],
//				"type": "text"
//			},
//			"attr_478702852907008": {
//				"actualValueList": [
//					"使用中"
//				],
//				"valueList": [
//					481855425798147
//				],
//				"type": "select",
//				"config": {}
//			},
//			"attr_480816840630272": {
//				"actualValueList": [
//					"2022-02-21"
//				],
//				"valueList": [
//					"2022-02-21"
//				],
//				"type": "date",
//				"config": {
//					"format": "HH:mm",
//					"type": "timerange"
//				}
//			},
//			"attr_478703658213376": {
//				"actualValueList": [
//					"集团"
//				],
//				"valueList": [
//					481860601569283
//				],
//				"type": "select",
//				"config": {
//					"mode": "r"
//				}
//			},
//			"attr_478703406555136": {
//				"actualValueList": [
//					{
//						"text": "测试用户(test01)",
//						"value": 521740908240896
//					}
//				],
//				"valueList": [
//					521740908240896
//				],
//				"type": "select",
//				"config": {
//					"mode": "r",
//					"isMultiple": 1
//				}
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
                            tbodyObj.put(key, attrEntityData.getJSONObject(key));
                        }
                    }
                    JSONObject relEntityData = dataObj.getJSONObject("relEntityData");
                    if (MapUtils.isNotEmpty(relEntityData)) {
                        for (String key : keyList) {
                            tbodyObj.put(key, relEntityData.getJSONObject(key));
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
