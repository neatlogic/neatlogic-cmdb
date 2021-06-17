/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.cmdb.dto.customview.*;
import codedriver.framework.cmdb.exception.customview.CustomViewNotFoundException;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewDataMapper;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomViewDataServiceImpl implements CustomViewDataService {
    @Resource
    private CustomViewDataMapper customViewDataMapper;

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public CustomViewDataVo getCustomViewData(CustomViewConditionVo customViewConditionVo) {
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(customViewConditionVo.getCustomViewId());
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(customViewConditionVo.getCustomViewId());
        }
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        customViewConditionVo.setFieldList(customViewAttrList.stream().map(CustomViewAttrVo::getUuid).collect(Collectors.toList()));
        CustomViewDataVo customViewDataVo = new CustomViewDataVo();
        customViewDataVo.setAttrList(customViewAttrList);
        List<Map<String, Object>> dataList = customViewDataMapper.searchCustomViewData(customViewConditionVo);
        customViewDataVo.setDataList(dataList);
        customViewDataVo.setCustomViewId(customViewVo.getId());
        customViewDataVo.setCustomViewName(customViewVo.getName());
        return customViewDataVo;
    }

    @Override
    public List<Map<String, Object>> searchCustomViewData(CustomViewConditionVo customViewConditionVo) {
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        customViewConditionVo.setFieldList(customViewAttrList.stream().map(CustomViewAttrVo::getUuid).collect(Collectors.toList()));
        List<Map<String, Object>> dataList = customViewDataMapper.searchCustomViewData(customViewConditionVo);
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
        return dataList;
    }

    @Override
    public List<Map<String, Long>> getCustomViewCiEntityIdById(CustomViewConditionVo customViewConditionVo) {
        return customViewDataMapper.getCustomViewCiEntityIdById(customViewConditionVo);
    }

    @Override
    public List<CustomViewDataGroupVo> searchCustomViewDataGroup(CustomViewConditionVo customViewConditionVo) {
        CustomViewAttrVo customViewAttrVo = customViewMapper.getCustomViewAttrByUuid(customViewConditionVo.getGroupBy());
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
        customViewConditionVo.setFieldList(customViewAttrList.stream().map(CustomViewAttrVo::getUuid).collect(Collectors.toList()));
        List<CustomViewDataGroupVo> groupList = customViewDataMapper.searchCustomViewDataGroup(customViewConditionVo);
        for (CustomViewDataGroupVo customViewDataGroupVo : groupList) {
            customViewDataGroupVo.setAttrAlias(customViewAttrVo.getAlias());
            customViewDataGroupVo.setAttrUuid(customViewAttrVo.getUuid());
            if (CollectionUtils.isNotEmpty(customViewConditionVo.getValueFilterList())) {
                //必须要复制一份，否则序列化成json会出错
                for (CustomViewValueFilterVo filterVo : customViewConditionVo.getValueFilterList()) {
                    customViewDataGroupVo.addValueFilter(new CustomViewValueFilterVo(filterVo.getUuid(), filterVo.getValue()));
                }
            }
        }
        return groupList;
    }

}
