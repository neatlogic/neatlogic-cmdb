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

package neatlogic.module.cmdb.service.customview;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.crossover.ICustomViewDataCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.*;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewDataMapper;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomViewDataServiceImpl implements CustomViewDataService, ICustomViewDataCrossoverService {
    @Resource
    private CustomViewDataMapper customViewDataMapper;

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CustomViewService customViewService;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public CustomViewDataVo getCustomViewData(CustomViewConditionVo customViewConditionVo) {
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(customViewConditionVo.getCustomViewId());
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(customViewConditionVo.getCustomViewId());
        }
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        customViewConditionVo.setFieldList(customViewAttrList.stream().map(ci -> new CustomViewConditionFieldVo(ci.getUuid(), "id")).collect(Collectors.toList()));
        CustomViewDataVo customViewDataVo = new CustomViewDataVo();
        customViewDataVo.setAttrList(customViewAttrList);
        List<Map<String, Object>> dataList = customViewDataMapper.searchCustomViewData(customViewConditionVo);
        customViewDataVo.setDataList(dataList);
        customViewDataVo.setCustomViewId(customViewVo.getId());
        customViewDataVo.setCustomViewName(customViewVo.getName());
        return customViewDataVo;
    }


    public List<Map<String, Object>> searchCustomViewDataFlatten(CustomViewConditionVo customViewConditionVo) {
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        List<CustomViewConstAttrVo> customViewConstAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(new CustomViewConstAttrVo(customViewConditionVo.getCustomViewId()));
        //去掉所有引用属性
        customViewAttrList = customViewAttrList.stream().filter(attr -> attr.getAttrVo().getTargetCiId() == null).collect(Collectors.toList());
        Map<String, AttrVo> attrMap = new HashMap<>();
        Map<String, CustomViewAttrVo> attrNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(customViewAttrList)) {
            for (CustomViewAttrVo customViewAttr : customViewAttrList) {
                attrMap.put(customViewAttr.getUuid(), customViewAttr.getAttrVo());
                if (StringUtils.isNotBlank(customViewAttr.getName())) {
                    attrNameMap.put(customViewAttr.getName(), customViewAttr);
                }
                if (MapUtils.isNotEmpty(customViewAttr.getCondition())) {
                    String expression = customViewAttr.getCondition().getString("expression");
                    if (StringUtils.isNotBlank(expression)) {
                        JSONArray valueList = customViewAttr.getCondition().getJSONArray("valueList");
                        customViewConditionVo.addAttrFilter(new CustomViewConditionFilterVo(customViewAttr.getUuid(), customViewAttr.getAttrVo().getType(), expression, valueList));
                    }
                }
            }
        }
        //补充attrtype，搜索时需要使用，如果没有uuid的需要根据name来补充uuid，没有找到uuid的条件会被舍弃
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getAttrFilterList())) {
            Iterator<CustomViewConditionFilterVo> iterator = customViewConditionVo.getAttrFilterList().iterator();
            while (iterator.hasNext()) {
                CustomViewConditionFilterVo filterVo = iterator.next();
                if (StringUtils.isBlank(filterVo.getAttrUuid()) && StringUtils.isNotBlank(filterVo.getAttrName())) {
                    CustomViewAttrVo customAttrVo = attrNameMap.get(filterVo.getAttrName());
                    if (customAttrVo != null) {
                        filterVo.setAttrUuid(customAttrVo.getUuid());
                    }
                }
                if (StringUtils.isNotBlank(filterVo.getAttrUuid())) {
                    AttrVo attrVo = attrMap.get(filterVo.getAttrUuid());
                    if (attrVo != null) {
                        filterVo.setAttrType(attrVo.getType());
                    }
                } else {
                    iterator.remove();
                }
            }
        }
        List<CustomViewConditionFieldVo> customViewConditionFieldList = new ArrayList<>();
        customViewConditionFieldList.addAll(customViewAttrList.stream().filter(d -> StringUtils.isNotBlank(d.getName())).map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "attr", attr.getName())).collect(Collectors.toList()));
        customViewConditionFieldList.addAll(customViewConstAttrList.stream().filter(d -> StringUtils.isNotBlank(d.getName())).map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "constattr", attr.getName())).collect(Collectors.toList()));
        customViewConditionVo.setFieldList(customViewConditionFieldList);

        List<Map<String, Object>> dataList = customViewDataMapper.searchCustomViewDataFlatten(customViewConditionVo);
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getValueFilterList())) {
            for (Map<String, Object> data : dataList) {
                //必须要复制一份，否则序列化成json会出错
                List<CustomViewValueFilterVo> filterList = new ArrayList<>();
                for (CustomViewValueFilterVo filterVo : customViewConditionVo.getValueFilterList()) {
                    filterList.add(new CustomViewValueFilterVo(filterVo.getUuid(), filterVo.getValue()));
                }
                data.put("_filterList", filterList);

            }
        }

        //转换属性真实值
        for (Map<String, Object> data : dataList) {
            for (String key : data.keySet()) {
                if (!key.equals("id") && !key.endsWith("_hash") && attrMap.containsKey(key) && data.get(key) != null) {
                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrMap.get(key).getType());
                    if (handler != null) {
                        JSONArray vl = new JSONArray();
                        vl.add(data.get(key));
                        handler.transferValueListToDisplay(attrMap.get(key), vl);
                        if (CollectionUtils.isNotEmpty(vl)) {
                            data.put(key, vl.get(0));
                        }
                    }
                }
            }
        }
        return dataList;
    }


    @Override
    public List<Map<String, Object>> searchCustomViewData(CustomViewConditionVo customViewConditionVo) {
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        //去掉所有引用属性
        customViewAttrList = customViewAttrList.stream().filter(attr -> attr.getAttrVo().getTargetCiId() == null).collect(Collectors.toList());
        List<CustomViewConstAttrVo> customViewConstAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(new CustomViewConstAttrVo(customViewConditionVo.getCustomViewId()));
        Map<String, AttrVo> attrMap = new HashMap<>();
        Map<String, CustomViewAttrVo> attrNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(customViewAttrList)) {
            for (CustomViewAttrVo customViewAttr : customViewAttrList) {
                attrMap.put(customViewAttr.getUuid(), customViewAttr.getAttrVo());
                if (StringUtils.isNotBlank(customViewAttr.getName())) {
                    attrNameMap.put(customViewAttr.getName(), customViewAttr);
                }
                if (MapUtils.isNotEmpty(customViewAttr.getCondition())) {
                    String expression = customViewAttr.getCondition().getString("expression");
                    if (StringUtils.isNotBlank(expression)) {
                        JSONArray valueList = customViewAttr.getCondition().getJSONArray("valueList");
                        /*
                        由于前端保存的值是目标配置项的id，进入视图查询之间需要转换成对应配置项名称，否则无法匹配上
                         */
                        if (CollectionUtils.isNotEmpty(valueList) && customViewAttr.getAttrVo().getTargetCiId() != null) {
                            List<Long> ciEntityIdList = new ArrayList<>();
                            for (int i = 0; i < valueList.size(); i++) {
                                ciEntityIdList.add(valueList.getLongValue(i));
                            }
                            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
                            valueList = new JSONArray();
                            for (CiEntityVo cientityVo : ciEntityList) {
                                valueList.add(cientityVo.getName());
                            }
                        }
                        customViewConditionVo.addAttrFilter(new CustomViewConditionFilterVo(customViewAttr.getUuid(), customViewAttr.getAttrVo().getType(), expression, valueList));
                    }
                }
            }
        }
        //补充attrtype，搜索时需要使用，如果没有uuid的需要根据name来补充uuid，没有找到uuid的条件会被舍弃
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getAttrFilterList())) {
            Iterator<CustomViewConditionFilterVo> iterator = customViewConditionVo.getAttrFilterList().iterator();
            while (iterator.hasNext()) {
                CustomViewConditionFilterVo filterVo = iterator.next();
                if (StringUtils.isBlank(filterVo.getAttrUuid()) && StringUtils.isNotBlank(filterVo.getAttrName())) {
                    CustomViewAttrVo customAttrVo = attrNameMap.get(filterVo.getAttrName());
                    if (customAttrVo != null) {
                        filterVo.setAttrUuid(customAttrVo.getUuid());
                    }
                }
                if (StringUtils.isNotBlank(filterVo.getAttrUuid())) {
                    AttrVo attrVo = attrMap.get(filterVo.getAttrUuid());
                    if (attrVo != null) {
                        filterVo.setAttrType(attrVo.getType());
                    }
                } else {
                    iterator.remove();
                }
            }
        }
        List<CustomViewConditionFieldVo> customViewConditionFieldList = new ArrayList<>();
        customViewConditionFieldList.addAll(customViewAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "attr")).collect(Collectors.toList()));
        customViewConditionFieldList.addAll(customViewConstAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "constattr")).collect(Collectors.toList()));
        customViewConditionVo.setFieldList(customViewConditionFieldList);

        List<Map<String, Object>> dataList = customViewDataMapper.searchCustomViewData(customViewConditionVo);
        int rowNum = customViewDataMapper.searchCustomViewDataCount(customViewConditionVo);
        customViewConditionVo.setRowNum(rowNum);
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getValueFilterList())) {
            for (Map<String, Object> data : dataList) {
                //必须要复制一份，否则序列化成json会出错
                List<CustomViewValueFilterVo> filterList = new ArrayList<>();
                for (CustomViewValueFilterVo filterVo : customViewConditionVo.getValueFilterList()) {
                    filterList.add(new CustomViewValueFilterVo(filterVo.getUuid(), filterVo.getValue()));
                }
                data.put("_filterList", filterList);

            }
        }

        //转换属性真实值
        for (Map<String, Object> data : dataList) {
            for (String key : data.keySet()) {
                if (!key.equals("id") && !key.endsWith("_hash") && attrMap.containsKey(key) && data.get(key) != null) {
                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrMap.get(key).getType());
                    if (handler != null) {
                        JSONArray vl = new JSONArray();
                        vl.add(data.get(key));
                        handler.transferValueListToDisplay(attrMap.get(key), vl);
                        if (CollectionUtils.isNotEmpty(vl)) {
                            data.put(key, vl.get(0));
                        }
                    }
                }
            }
        }
        return dataList;
    }

    @Override
    public CustomViewVo getCustomViewCiEntityById(CustomViewConditionVo customViewConditionVo) {
        CustomViewVo customViewVo = customViewService.getCustomViewDetailById(customViewConditionVo.getCustomViewId());
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(customViewConditionVo.getCustomViewId());
        }
        List<CustomViewConditionFieldVo> fieldList = new ArrayList<>();
        fieldList.addAll(customViewVo.getCiList().stream().map(ci -> new CustomViewConditionFieldVo(ci.getUuid(), "id")).collect(Collectors.toList()));
        fieldList.addAll(customViewVo.getCiList().stream().map(ci -> new CustomViewConditionFieldVo(ci.getUuid(), "name")).collect(Collectors.toList()));
        customViewConditionVo.setFieldList(fieldList);
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getFieldList())) {
            List<Map<String, Object>> resultList = customViewDataMapper.getCustomViewCiEntityById(customViewConditionVo);
            for (Map<String, Object> result : resultList) {
                for (String key : result.keySet()) {
                    if (key.contains("_id")) {
                        String ciUuid = key.replace("_id", "");
                        CustomViewCiVo ciVo = customViewVo.getCustomCiByUuid(ciUuid);
                        if (ciVo != null) {
                            ciVo.addCiEntity(Long.parseLong(result.get(key).toString()), (String) result.get(ciUuid + "_name"));
                        }
                    }
                }
            }
        }
        //清空不需要返回的信息
        customViewVo.setConfig(null);
        customViewVo.setConfigStr(null);
        return customViewVo;
    }

    @Override
    public List<CiEntityVo> searchCustomViewCiEntity(CustomViewConditionVo customViewConditionVo) {
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        List<CustomViewConstAttrVo> customViewConstAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(new CustomViewConstAttrVo(customViewConditionVo.getCustomViewId()));
        List<CustomViewConditionFieldVo> customViewConditionFieldList = new ArrayList<>();
        customViewConditionFieldList.addAll(customViewAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "attr")).collect(Collectors.toList()));
        customViewConditionFieldList.addAll(customViewConstAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "constattr")).collect(Collectors.toList()));
        customViewConditionVo.setFieldList(customViewConditionFieldList);
        return customViewDataMapper.searchCustomViewCiEntity(customViewConditionVo);
    }

    @Override
    public List<CustomViewDataGroupVo> searchCustomViewDataGroup(CustomViewConditionVo customViewConditionVo) {
        CustomViewAttrVo customViewAttrVo = customViewMapper.getCustomViewAttrByUuid(customViewConditionVo.getCustomViewId(), customViewConditionVo.getGroupBy());
        CustomViewConstAttrVo customViewConstAttrVo = customViewMapper.getCustomViewConstAttrByUuid(customViewConditionVo.getCustomViewId(), customViewConditionVo.getGroupBy());
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        //去掉所有引用属性
        customViewAttrList = customViewAttrList.stream().filter(attr -> attr.getAttrVo().getTargetCiId() == null).collect(Collectors.toList());

        List<CustomViewConstAttrVo> customViewConstAttrList = customViewMapper.getCustomViewConstAttrByCustomViewId(new CustomViewConstAttrVo(customViewConditionVo.getCustomViewId()));
        Map<String, AttrVo> attrMap = new HashMap<>();
        Map<String, CustomViewAttrVo> attrNameMap = new HashMap<>();
        //Map<String, CustomViewConstAttrVo> constAttrMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(customViewAttrList)) {
            for (CustomViewAttrVo customViewAttr : customViewAttrList) {
                attrMap.put(customViewAttr.getUuid(), customViewAttr.getAttrVo());
                if (StringUtils.isNotBlank(customViewAttr.getName())) {
                    attrNameMap.put(customViewAttr.getName(), customViewAttr);
                }
                if (MapUtils.isNotEmpty(customViewAttr.getCondition())) {
                    String expression = customViewAttr.getCondition().getString("expression");
                    if (StringUtils.isNotBlank(expression)) {
                        JSONArray valueList = customViewAttr.getCondition().getJSONArray("valueList");
                        customViewConditionVo.addAttrFilter(new CustomViewConditionFilterVo(customViewAttr.getUuid(), customViewAttr.getAttrVo().getType(), expression, valueList));
                    }
                }
                //如果发现传入的groupby不是uuid，尝试转换成真实的uuid
                //不成熟，先注释
                /*if (customViewConditionVo.getGroupBy().length() != 32 && StringUtils.isNotBlank(customViewAttr.getName())) {
                    if (customViewAttr.getName().equalsIgnoreCase(customViewConditionVo.getGroupBy())) {
                        customViewConditionVo.setGroupBy(customViewAttr.getUuid());
                    }
                }*/
            }
        }
        //补充attrtype，搜索时需要使用，如果没有uuid的需要根据name来补充uuid，没有找到uuid的条件会被舍弃
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getAttrFilterList())) {
            Iterator<CustomViewConditionFilterVo> iterator = customViewConditionVo.getAttrFilterList().iterator();
            while (iterator.hasNext()) {
                CustomViewConditionFilterVo filterVo = iterator.next();
                if (StringUtils.isBlank(filterVo.getAttrUuid()) && StringUtils.isNotBlank(filterVo.getAttrName())) {
                    CustomViewAttrVo customAttrVo = attrNameMap.get(filterVo.getAttrName());
                    if (customAttrVo != null) {
                        filterVo.setAttrUuid(customAttrVo.getUuid());
                    }
                }
                if (StringUtils.isNotBlank(filterVo.getAttrUuid())) {
                    AttrVo attrVo = attrMap.get(filterVo.getAttrUuid());
                    if (attrVo != null) {
                        filterVo.setAttrType(attrVo.getType());
                    }
                } else {
                    iterator.remove();
                }
            }
        }
        List<CustomViewConditionFieldVo> customViewConditionFieldList = new ArrayList<>();
        customViewConditionFieldList.addAll(customViewAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "attr")).collect(Collectors.toList()));
        customViewConditionFieldList.addAll(customViewConstAttrList.stream().map(attr -> new CustomViewConditionFieldVo(attr.getUuid(), "constattr")).collect(Collectors.toList()));

        customViewConditionVo.setFieldList(customViewConditionFieldList);

        List<CustomViewDataGroupVo> groupList = customViewDataMapper.searchCustomViewDataGroup(customViewConditionVo);
        int rowNum = customViewDataMapper.searchCustomViewDataGroupCount(customViewConditionVo);
        customViewConditionVo.setRowNum(rowNum);
        for (CustomViewDataGroupVo customViewDataGroupVo : groupList) {
            if (customViewAttrVo != null) {
                customViewDataGroupVo.setAttrAlias(customViewAttrVo.getAlias());
                customViewDataGroupVo.setAttrUuid(customViewAttrVo.getUuid());
            } else if (customViewConstAttrVo != null) {
                customViewDataGroupVo.setAttrAlias(customViewConstAttrVo.getAlias());
                customViewDataGroupVo.setAttrUuid(customViewConstAttrVo.getUuid());
            }
            if (CollectionUtils.isNotEmpty(customViewConditionVo.getValueFilterList())) {
                //必须要复制一份，否则序列化成json会出错
                for (CustomViewValueFilterVo filterVo : customViewConditionVo.getValueFilterList()) {
                    customViewDataGroupVo.addValueFilter(new CustomViewValueFilterVo(filterVo.getUuid(), filterVo.getValue()));
                }
            }
            //转换属性真实值
            if (customViewAttrVo != null) {
                if (attrMap.containsKey(customViewAttrVo.getUuid())) {
                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrMap.get(customViewAttrVo.getUuid()).getType());
                    if (handler != null) {
                        JSONArray vl = new JSONArray();
                        vl.add(customViewDataGroupVo.getValue());
                        handler.transferValueListToDisplay(attrMap.get(customViewAttrVo.getUuid()), vl);
                        if (CollectionUtils.isNotEmpty(vl)) {
                            customViewDataGroupVo.setValue(vl.getString(0));
                        }
                    }
                }
            }
        }
        return groupList;
    }

}
