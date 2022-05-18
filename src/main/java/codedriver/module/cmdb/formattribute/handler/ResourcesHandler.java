/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.formattribute.handler;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.form.attribute.core.FormHandlerBase;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.form.dto.AttributeDataVo;
import codedriver.framework.form.exception.AttributeValidException;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author linbq
 * @since 2021/12/27 11:14
 **/
@Component
public class ResourcesHandler extends FormHandlerBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getHandler() {
        return "formresoureces";
    }

    @Override
    public String getHandlerName() {
        return "执行目标";
    }

    @Override
    public String getHandlerType(FormConditionModel model) {
        return null;
    }

    @Override
    public String getIcon() {
        return "tsfont-blocks";
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
    public String getModule() {
        return "cmdb";
    }

    @Override
    public JSONObject valid(AttributeDataVo attributeDataVo, JSONObject jsonObj) throws AttributeValidException {
        JSONObject resultObj = new JSONObject();
        resultObj.put("result", true);
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        String type = dataObj.getString("type");
        if ("input".equals(type)) {
            JSONArray inputNodeArray = dataObj.getJSONArray("inputNodeList");
            if (CollectionUtils.isNotEmpty(inputNodeArray)) {
                List<ResourceSearchVo> resourceIsNotFoundList = new ArrayList<>();
                List<ResourceSearchVo> inputNodeList = inputNodeArray.toJavaList(ResourceSearchVo.class);
                for (ResourceSearchVo node : inputNodeList) {
                    Long resourceId = resourceCenterMapper.getResourceIdByIpAndPortAndName(node);
                    if (resourceId == null) {
                        resourceIsNotFoundList.add(node);
                    }
                }
                if (CollectionUtils.isNotEmpty(resourceIsNotFoundList)) {
                    resultObj.put("result", false);
                    resultObj.put("list", resourceIsNotFoundList);
                }
            }
        }
        return resultObj;
    }

    @Override
    public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return "已更新";
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        return null;
    }

    @Override
    public Object textConversionValue(List<String> values, JSONObject config) {
        return null;
    }

    @Override
    public int getSort() {
        return 21;
    }

    //表单组件配置信息
//    {
//        "handler": "formresoureces",
//        "label": "执行目标_2",
//        "type": "form",
//        "uuid": "3d9b9d122e694532a7d68e2b2436b85b",
//        "config": {
//            "isRequired": false,
//            "ruleList": [],
//            "width": "100%",
//            "validList": [],
//            "quoteUuid": "",
//            "defaultValueType": "self",
//            "placeholder": "选择执行目标",
//            "authorityConfig": [
//                "common#alluser"
//            ]
//        }
//    }
//保存数据
//过滤器
//    {
//        "filter": {
//            "envIdList": [
//                481856650534914
//            ],
//            "protocolIdList": [
//                547705260400640
//            ],
//            "stateIdList": [
//                481855425798147
//            ],
//            "typeIdList": [
//                442011534499840
//            ],
//            "appSystemIdList": [
//                481894852255745
//            ],
//            "appModuleIdList": [
//                481894986473474
//            ],
//            "tagIdList": [
//                504187276025856
//            ]
//        },
//        "type": "filter"
//    }
//输入文本
//    {
//        "inputNodeList": [
//            {
//                "port": "8181",
//                "ip": "192.168.0.10",
//                "name": "b"
//            },
//            {
//                "port": "8080",
//                "ip": "192.168.0.10",
//                "name": "a"
//            }
//        ],
//        "type": "input"
//    }
//节点
//    {
//        "selectNodeList": [
//            {
//                "port": 3306,
//                "ip": "192.168.0.101",
//                "name": "Mysql",
//                "id": 493223784800258
//            }
//        ],
//        "type": "node"
//    }
//返回数据结构
//{
//	"value": {
//		"selectNodeList": [
//			{
//				"port": 3306,
//				"ip": "192.168.0.101",
//				"name": "Mysql",
//				"id": 493223784800258
//			},
//			{
//				"port": 3306,
//				"ip": "192.168.0.21",
//				"name": "Mysql",
//				"id": 493223793188865
//			}
//		],
//		"inputNodeList": [
//			{
//				"port": "8081",
//				"ip": "192.168.0.1",
//				"name": "codedriver2"
//			},
//			{
//				"port": "8080",
//				"ip": "192.168.0.1",
//				"name": "codedriver"
//			}
//		],
//		"filter": {
//			"envIdList": [
//				481856650534914,
//				481856650534918,
//				481856650534925
//			],
//			"protocolIdList": [
//				547705260400640,
//				478219912355840
//			],
//			"stateIdList": [
//				481855425798147
//			],
//			"typeIdList": [
//				479596491317248,
//				479598143873024
//			],
//			"appSystemIdList": [
//				481894852255745,
//				481894852255749
//			],
//			"appModuleIdList": [
//				481894994862129,
//				481894986473498
//			],
//			"tagIdList": [
//				504187276025856,
//				508639420669952
//			]
//		},
//		"type": "input/node/filter"
//	},
//	"selectNodeList": [
//		{
//			"port": 3306,
//			"ip": "192.168.0.101",
//			"name": "Mysql",
//			"id": 493223784800258
//		},
//		{
//			"port": 3306,
//			"ip": "192.168.0.21",
//			"name": "Mysql",
//			"id": 493223793188865
//		}
//	],
//	"inputNodeList": [
//		{
//			"port": "8081",
//			"ip": "192.168.0.1",
//			"name": "codedriver2"
//		},
//		{
//			"port": "8080",
//			"ip": "192.168.0.1",
//			"name": "codedriver"
//		}
//	],
//	"filterList": [
//		{
//			"textList": [
//				"PRD",
//				"UAT",
//				"SIT"
//			],
//			"valueList": [
//				481856650534914,
//				481856650534918,
//				481856650534925
//			],
//			"label": "环境"
//		},
//		{
//			"textList": [
//				"https",
//				"ssh"
//			],
//			"valueList": [
//				547705260400640,
//				478219912355840
//			],
//			"label": "连接协议"
//		},
//		{
//			"textList": [
//				"使用中"
//			],
//			"valueList": [
//				481855425798147
//			],
//			"label": "状态"
//		},
//		{
//			"textList": [
//				"DBIns",
//				"DBCluster"
//			],
//			"valueList": [
//				479596491317248,
//				479598143873024
//			],
//			"label": "模型类型"
//		},
//		{
//			"textList": [
//				"理财资产管理",
//				"全网支付平台"
//			],
//			"valueList": [
//				481894852255745,
//				481894852255749
//			],
//			"label": "系统"
//		},
//		{
//			"textList": [
//				"交易反欺诈消费",
//				"台账系统"
//			],
//			"valueList": [
//				481894994862129,
//				481894986473498
//			],
//			"label": "模块"
//		},
//		{
//			"textList": [],
//			"valueList": [
//				504187276025856,
//				508639420669952
//			],
//			"label": "标签"
//		}
//	],
//	"type": "input/node/filter"
//}
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        resultObj.put("value", dataObj);
        String type = dataObj.getString("type");
        resultObj.put("type", type);
        if ("node".equals(type)) {
            resultObj.put("selectNodeList", dataObj.getJSONArray("selectNodeList"));
        } else if ("input".equals(type)) {
            resultObj.put("inputNodeList", dataObj.getJSONArray("inputNodeList"));
        } else if ("filter".equals(type)) {
            JSONObject filter = dataObj.getJSONObject("filter");
            if (MapUtils.isNotEmpty(filter)) {
                JSONArray filterList = new JSONArray();
                JSONArray envIdList = filter.getJSONArray("envIdList");
                if (CollectionUtils.isNotEmpty(envIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "环境");
                    jsonObj.put("valueList", envIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = envIdList.toJavaList(Long.class);
                    List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
                    Map<Long, String> nameMap = ciEntityList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                JSONArray protocolIdList = filter.getJSONArray("protocolIdList");
                if (CollectionUtils.isNotEmpty(protocolIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "连接协议");
                    jsonObj.put("valueList", protocolIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = protocolIdList.toJavaList(Long.class);
                    List<AccountProtocolVo> accountProtocolList = resourceCenterMapper.getAccountProtocolListByIdList(idList);
                    Map<Long, String> nameMap = accountProtocolList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                JSONArray stateIdList = filter.getJSONArray("stateIdList");
                if (CollectionUtils.isNotEmpty(stateIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "状态");
                    jsonObj.put("valueList", stateIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = stateIdList.toJavaList(Long.class);
                    List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
                    Map<Long, String> nameMap = ciEntityList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                JSONArray typeIdList = filter.getJSONArray("typeIdList");
                if (CollectionUtils.isNotEmpty(typeIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "模型类型");
                    jsonObj.put("valueList", typeIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = typeIdList.toJavaList(Long.class);
                    List<CiVo> ciList = ciMapper.getCiByIdList(idList);
                    Map<Long, String> nameMap = ciList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                JSONArray appSystemIdList = filter.getJSONArray("appSystemIdList");
                if (CollectionUtils.isNotEmpty(appSystemIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "系统");
                    jsonObj.put("valueList", appSystemIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = appSystemIdList.toJavaList(Long.class);
                    List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
                    Map<Long, String> nameMap = ciEntityList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                JSONArray appModuleIdList = filter.getJSONArray("appModuleIdList");
                if (CollectionUtils.isNotEmpty(appModuleIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "模块");
                    jsonObj.put("valueList", appModuleIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = appModuleIdList.toJavaList(Long.class);
                    List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
                    Map<Long, String> nameMap = ciEntityList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                JSONArray tagIdList = filter.getJSONArray("tagIdList");
                if (CollectionUtils.isNotEmpty(tagIdList)) {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("label", "标签");
                    jsonObj.put("valueList", tagIdList);
                    List<String> textList = new ArrayList<>();
                    List<Long> idList = appModuleIdList.toJavaList(Long.class);
                    TagVo searchVo = new TagVo();
                    searchVo.setDefaultValue(tagIdList);
                    List<TagVo> tagVoList = resourceCenterMapper.getTagListForSelect(searchVo);
                    Map<Long, String> nameMap = tagVoList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getName()));
                    for (Long id : idList) {
                        String name = nameMap.get(id);
                        if (name != null) {
                            textList.add(name);
                        }
                    }
                    jsonObj.put("textList", textList);
                    filterList.add(jsonObj);
                }
                resultObj.put("filterList", filterList);
            }
        }
        return resultObj;
    }

    @Override
    public Object dataTransformationForExcel(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject detailedData = getMyDetailedData(attributeDataVo, configObj);
        if (detailedData != null) {
            String type = detailedData.getString("type");
            if ("node".equals(type)) {
                JSONArray selectNodeList = detailedData.getJSONArray("selectNodeList");
                if (CollectionUtils.isNotEmpty(selectNodeList)) {
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < selectNodeList.size(); i++) {
                        JSONObject object = selectNodeList.getJSONObject(i);
                        String name = object.getString("name");
                        if (name != null) {
                            list.add(name);
                        }
                    }
                    return String.join(",", list);
                }
            } else if ("input".equals(type)) {
                JSONArray inputNodeList = detailedData.getJSONArray("inputNodeList");
                if (inputNodeList != null) {
                    return inputNodeList.toJSONString();
                }
            } else if ("filter".equals(type)) {
                JSONArray filterList = detailedData.getJSONArray("filterList");
                if (filterList != null) {
                    return filterList.toJSONString();
                }
            }
        }
        return null;
    }
}
