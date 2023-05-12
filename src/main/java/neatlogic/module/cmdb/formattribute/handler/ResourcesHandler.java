/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.cmdb.formattribute.handler;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.cmdb.enums.FormHandler;
import neatlogic.framework.cmdb.resourcecenter.condition.ResourcecenterConditionUtil;
import neatlogic.framework.common.constvalue.ParamType;
import neatlogic.framework.dto.condition.*;
import neatlogic.framework.form.attribute.core.FormHandlerBase;
import neatlogic.framework.form.constvalue.FormConditionModel;
import neatlogic.framework.form.dto.AttributeDataVo;
import neatlogic.framework.form.exception.AttributeValidException;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;
    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getHandler() {
        return FormHandler.FORMRESOURECES.getHandler();
    }

    @Override
    public String getHandlerName() {
        return FormHandler.FORMRESOURECES.getHandlerName();
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
    public boolean isProcessTaskBatchSubmissionTemplateParam() {
        return false;
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
                    Long resourceId = resourceMapper.getResourceIdByIpAndPortAndName(node);
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

    private String nodeJSONObjectToString(JSONObject nodeObj) {
        String ip = nodeObj.getString("ip");
        String port = nodeObj.getString("port");
        if (StringUtils.isNotBlank(port)) {
            ip = ip + ":" + port;
            String name = nodeObj.getString("name");
            if (StringUtils.isNotBlank(name)) {
                ip = ip + "/" + name;
            }
        }
        return ip;
    }

    @Override
    public Object dataTransformationForEmail(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject dataObj = getMyDetailedData(attributeDataVo, configObj);
        if (MapUtils.isEmpty(dataObj)) {
            return StringUtils.EMPTY;
        }
        JSONArray selectNodeList = dataObj.getJSONArray("selectNodeList");
        JSONArray inputNodeList = dataObj.getJSONArray("inputNodeList");
//        JSONObject conditionConfig = dataObj.getJSONObject("conditionConfig");
        JSONArray filterList = dataObj.getJSONArray("filterList");
        if (CollectionUtils.isNotEmpty(selectNodeList)) {
            List<String> nodeList = new ArrayList<>();
            for (int i = 0; i < selectNodeList.size(); i++) {
                JSONObject nodeObj = selectNodeList.getJSONObject(i);
                nodeList.add(nodeJSONObjectToString(nodeObj));
            }
            return String.join("、", nodeList);
        } else if (CollectionUtils.isNotEmpty(inputNodeList)) {
            List<String> nodeList = new ArrayList<>();
            for (int i = 0; i < inputNodeList.size(); i++) {
                JSONObject nodeObj = inputNodeList.getJSONObject(i);
                nodeList.add(nodeJSONObjectToString(nodeObj));
            }
            return String.join("、", nodeList);
        } else if (CollectionUtils.isNotEmpty(filterList)) {
            // 过滤简单模式
            List<String> resultList = new ArrayList<>();
            for (int i = 0; i < filterList.size(); i++) {
                JSONObject filterObj = filterList.getJSONObject(i);
                String label = filterObj.getString("label");
                JSONArray textArray = filterObj.getJSONArray("textList");
                List<String> textList = textArray.toJavaList(String.class);
                resultList.add(label + "：" + String.join("|", textList));
            }
            return String.join("、", resultList);
        }
//        else if (MapUtils.isNotEmpty(conditionConfig)) {
//            ResourceSearchVo resourceSearchVo = conditionConfig.toJavaObject(ResourceSearchVo.class);
//            String result = resourceSearchVo.getBuildNaturalLanguageExpressions();
//            return result;
//        }
        return StringUtils.EMPTY;
    }

    @Override
    public Object textConversionValue(Object text, JSONObject config) {
        return null;
    }

    @Override
    public int getSort() {
        return 17;
    }

    /*
    //表单组件配置信息
    {
        "handler": "formresoureces",
        "reaction": {
            "hide": {},
            "readonly": {},
            "display": {}
        },
        "override_config": {},
        "icon": "tsfont-adapter",
        "hasValue": true,
        "label": "执行目标_1",
        "type": "form",
        "category": "autoexec",
        "config": {
            "isRequired": false,
            "disableDefaultValue": true,
            "isMask": false,
            "width": "100%",
            "description": "",
            "isHide": false
        },
        "uuid": "e9d06c776ebb4737b96b245d6ad4d65e"
    }
    //保存数据
    //节点
        {
        "selectNodeList": [
            {
                "port": 3306,
                "ip": "192.168.0.101",
                "name": "customerBaseline",
                "id": 493223784800258
            },
            {
                "port": 3306,
                "ip": "192.168.0.21",
                "name": "sit-db-21",
                "id": 493223793188865
            }
        ]
    }
    //输入文本
    {
        "inputNodeList": [
            {
                "port": "3306",
                "ip": "192.168.0.21"
            },
            {
                "port": "3306",
                "ip": "192.168.0.101"
            }
        ]
    }
    //过滤器简单模式
    {
        "filter": {
            "envIdList": [
                481856650534914,
                481856650534918,
                481856650534925,
                699864006320129
            ],
            "protocolIdList": [
                478219912355840,
                547705260400640,
                658651035262976
            ],
            "vendorIdList": [
                481861851471878,
                481861851471874,
                481861851471882
            ],
            "typeIdList": [
                442011534499840
            ],
            "stateIdList": [
                481855425798147,
                575058128723968
            ],
            "appSystemIdList": [
                481894852255745
            ],
            "appModuleIdList": [
                481894986473474
            ],
            "tagIdList": [
                686828596027393
            ]
        }
    }
    //过滤器高级模式
    {
        "filter": {
            "conditionGroupList": [
                {
                    "conditionList": [
                        {
                            "expression": "include",
                            "valueList": [
                                442011534499840
                            ],
                            "name": "typeIdList",
                            "uuid": "8b2d7cf26de643baa8958c629885ea47"
                        },
                        {
                            "expression": "include",
                            "valueList": [
                                481894852255745
                            ],
                            "name": "appSystemIdList",
                            "uuid": "370ec10f3ec8419db4e44b90cc16fc99"
                        }
                    ],
                    "conditionRelList": [
                        {
                            "joinType": "or",
                            "from": "8b2d7cf26de643baa8958c629885ea47",
                            "to": "370ec10f3ec8419db4e44b90cc16fc99"
                        }
                    ],
                    "uuid": "4ff25e95bb344ed8a37a6e1fb4e40068"
                },
                {
                    "conditionList": [
                        {
                            "expression": "include",
                            "valueList": [
                                699864006320129,
                                481856650534925,
                                481856650534918,
                                481856650534914
                            ],
                            "name": "envIdList",
                            "uuid": "326d54b079aa474eb9640efa03cbab63"
                        }
                    ],
                    "conditionRelList": [],
                    "uuid": "0a24501fa53d4cf3b0a638ea639b7842"
                }
            ],
            "keyword": "",
            "conditionGroupRelList": [
                {
                    "joinType": "or",
                    "from": "4ff25e95bb344ed8a37a6e1fb4e40068",
                    "to": "0a24501fa53d4cf3b0a638ea639b7842"
                }
            ]
        }
    }
    //返回数据结构
    {
        "value": {
            "selectNodeList": [
                {
                    "port": 3306,
                    "ip": "192.168.0.101",
                    "name": "Mysql",
                    "id": 493223784800258
                },
                {
                    "port": 3306,
                    "ip": "192.168.0.21",
                    "name": "Mysql",
                    "id": 493223793188865
                }
            ],
            "inputNodeList": [
                {
                    "port": "8081",
                    "ip": "192.168.0.1",
                    "name": "neatlogic2"
                },
                {
                    "port": "8080",
                    "ip": "192.168.0.1",
                    "name": "neatlogic"
                }
            ],
            "filter": {
                "envIdList": [
                    481856650534914,
                    481856650534918,
                    481856650534925
                ],
                "protocolIdList": [
                    547705260400640,
                    478219912355840
                ],
                "stateIdList": [
                    481855425798147
                ],
                "typeIdList": [
                    479596491317248,
                    479598143873024
                ],
                "appSystemIdList": [
                    481894852255745,
                    481894852255749
                ],
                "appModuleIdList": [
                    481894994862129,
                    481894986473498
                ],
                "tagIdList": [
                    504187276025856,
                    508639420669952
                ]
            },
            "type": "input/node/filter"
        },
        "selectNodeList": [
            {
                "port": 3306,
                "ip": "192.168.0.101",
                "name": "Mysql",
                "id": 493223784800258
            },
            {
                "port": 3306,
                "ip": "192.168.0.21",
                "name": "Mysql",
                "id": 493223793188865
            }
        ],
        "inputNodeList": [
            {
                "port": "8081",
                "ip": "192.168.0.1",
                "name": "neatlogic2"
            },
            {
                "port": "8080",
                "ip": "192.168.0.1",
                "name": "neatlogic"
            }
        ],
        "filterList": [
            {
                "textList": [
                    "PRD",
                    "UAT",
                    "SIT"
                ],
                "valueList": [
                    481856650534914,
                    481856650534918,
                    481856650534925
                ],
                "label": "环境"
            },
            {
                "textList": [
                    "https",
                    "ssh"
                ],
                "valueList": [
                    547705260400640,
                    478219912355840
                ],
                "label": "连接协议"
            },
            {
                "textList": [
                    "使用中"
                ],
                "valueList": [
                    481855425798147
                ],
                "label": "状态"
            },
            {
                "textList": [
                    "DBIns",
                    "DBCluster"
                ],
                "valueList": [
                    479596491317248,
                    479598143873024
                ],
                "label": "模型类型"
            },
            {
                "textList": [
                    "理财资产管理",
                    "全网支付平台"
                ],
                "valueList": [
                    481894852255745,
                    481894852255749
                ],
                "label": "系统"
            },
            {
                "textList": [
                    "交易反欺诈消费",
                    "台账系统"
                ],
                "valueList": [
                    481894994862129,
                    481894986473498
                ],
                "label": "模块"
            },
            {
                "textList": [],
                "valueList": [
                    504187276025856,
                    508639420669952
                ],
                "label": "标签"
            }
        ]
    }

     */
    @Override
    protected JSONObject getMyDetailedData(AttributeDataVo attributeDataVo, JSONObject configObj) {
        JSONObject resultObj = new JSONObject();
        JSONObject dataObj = (JSONObject) attributeDataVo.getDataObj();
        if (MapUtils.isEmpty(dataObj)) {
            return dataObj;
        }
        resultObj.put("value", dataObj);
        JSONArray selectNodeList = dataObj.getJSONArray("selectNodeList");
        JSONArray inputNodeList = dataObj.getJSONArray("inputNodeList");
        JSONObject filter = dataObj.getJSONObject("filter");
        if (CollectionUtils.isNotEmpty(selectNodeList)) {
            resultObj.put("selectNodeList", selectNodeList);
        } else if (CollectionUtils.isNotEmpty(inputNodeList)) {
            resultObj.put("inputNodeList", inputNodeList);
        } else if (MapUtils.isNotEmpty(filter)) {
            if (filter.containsKey("conditionGroupList")) {
                // 过滤高级模式
//                resultObj.put("conditionConfig", filter);
            } else {
                // 过滤简单模式
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
                    List<AccountProtocolVo> accountProtocolList = resourceAccountMapper.getAccountProtocolListByIdList(idList);
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
                    List<Long> idList = tagIdList.toJavaList(Long.class);
                    TagVo searchVo = new TagVo();
                    searchVo.setDefaultValue(tagIdList);
                    List<TagVo> tagVoList = resourceTagMapper.getTagListForSelect(searchVo);
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
