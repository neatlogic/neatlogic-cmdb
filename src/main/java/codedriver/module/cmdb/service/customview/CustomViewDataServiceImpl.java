/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.dto.customview.CustomViewDataVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.exception.customview.CustomViewNotFoundException;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewDataMapper;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
        List<CustomViewAttrVo> customViewAttrList = customViewMapper.getCustomViewAttrByCustomViewId(customViewConditionVo.getCustomViewId());
        customViewConditionVo.setFieldList(customViewAttrList.stream().map(CustomViewAttrVo::getUuid).collect(Collectors.toList()));
        CustomViewDataVo customViewDataVo = new CustomViewDataVo();
        customViewDataVo.setAttrList(customViewAttrList);
        List<Map<String, Object>> dataList = customViewDataMapper.searchCustomViewData(customViewConditionVo);
        customViewDataVo.setDataList(dataList);
        customViewDataVo.setCustomViewId(customViewVo.getId());
        customViewDataVo.setCustomViewName(customViewVo.getName());
        return customViewDataVo;
    }

}
