/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewCiVo;
import codedriver.framework.cmdb.dto.customview.CustomViewLinkVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.exception.customview.CreateCustomViewFailedException;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.utils.CustomViewBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomViewServiceImpl implements CustomViewService {
    @Resource
    private CustomViewMapper customViewMapper;

    @Override
    public void updateCustomViewActive(CustomViewVo customViewVo) {
        customViewMapper.updateCustomViewActive(customViewVo);
    }

    @Override
    public CustomViewVo getCustomViewById(Long id) {
        return customViewMapper.getCustomViewById(id);
    }

    @Override
    public List<CustomViewVo> searchCustomView(CustomViewVo customViewVo) {
        List<CustomViewVo> customViewList = customViewMapper.searchCustomView(customViewVo);
        if (CollectionUtils.isNotEmpty(customViewList)) {
            int rowNum = customViewMapper.searchCustomViewCount(customViewVo);
            customViewVo.setRowNum(rowNum);
        }
        return customViewList;
    }

    @Override
    @Transactional
    public void insertCustomView(CustomViewVo customViewVo) {
        customViewMapper.insertCustomView(customViewVo);
        saveCustomView(customViewVo);
    }

    @Override
    @Transactional
    public void updateCustomView(CustomViewVo customViewVo) {
        customViewMapper.updateCustomView(customViewVo);
        customViewMapper.deleteCustomViewCiByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewLinkByCustomViewId(customViewVo.getId());
        saveCustomView(customViewVo);
    }

    private void saveCustomView(CustomViewVo customViewVo) {
        if (CollectionUtils.isNotEmpty(customViewVo.getCiList())) {
            for (CustomViewCiVo customViewCiVo : customViewVo.getCiList()) {
                customViewCiVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewCi(customViewCiVo);
                for (CustomViewAttrVo customViewAttrVo : customViewCiVo.getAttrList()) {
                    customViewAttrVo.setCustomViewId(customViewVo.getId());
                    customViewAttrVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                    customViewMapper.insertCustomViewAttr(customViewAttrVo);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(customViewVo.getLinkList())) {
            for (CustomViewLinkVo customViewLinkVo : customViewVo.getLinkList()) {
                customViewLinkVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewLink(customViewLinkVo);
            }
        }

        EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
            CustomViewBuilder builder = new CustomViewBuilder(customViewVo);
            builder.buildView();
        }).execute();
        if (!s.isSucceed()) {
            throw new CreateCustomViewFailedException(s.getError());
        }
    }

    public void buildCustomView(String sql) {
        customViewMapper.buildCustomView(sql);
    }


}
