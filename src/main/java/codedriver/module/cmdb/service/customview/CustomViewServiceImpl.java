/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewCiVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.exception.customview.CreateCustomViewFailedException;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.utils.CustomViewBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class CustomViewServiceImpl implements CustomViewService {
    @Resource
    private CustomViewMapper customViewMapper;

    @Override
    @Transactional
    public void insertCustomView(CustomViewVo customViewVo) {
        customViewMapper.insertCustomView(customViewVo);
        for (CustomViewCiVo customViewCiVo : customViewVo.getCiList()) {
            customViewCiVo.setCustomViewId(customViewVo.getId());
            customViewMapper.insertCustomViewCi(customViewCiVo);
            for (CustomViewAttrVo customViewAttrVo : customViewCiVo.getAttrList()) {
                customViewAttrVo.setCustomViewId(customViewVo.getId());
                customViewAttrVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                customViewMapper.insertCustomViewAttr(customViewAttrVo);
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

    @Override
    @Transactional
    public void updateCustomView(CustomViewVo customViewVo) {

    }

    public void buildCustomView(String sql) {
        customViewMapper.buildCustomView(sql);
    }


}
