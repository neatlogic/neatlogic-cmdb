/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
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

    }

    @Override
    @Transactional
    public void updateCustomView(CustomViewVo customViewVo) {

    }


}
