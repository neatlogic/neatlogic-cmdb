/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.tagent.register.handler;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import codedriver.framework.tagent.dto.TagentVo;
import codedriver.framework.tagent.register.core.AfterRegisterBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * 添加资源和账号的关联关系
 */
@Service
public class AddAccountResourceRelHandler extends AfterRegisterBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    /**
     * 提前写入关系，当资源中心ready后自然就关联上账号
     *
     * @param tagentVo tagent对象
     */
    @Override
    public void myExecute(TagentVo tagentVo) {
        if (tagentVo.getResourceId() != null && tagentVo.getAccountId() != null) {
            ResourceAccountVo resourceAccountVo = new ResourceAccountVo();
            resourceAccountVo.setResourceId(tagentVo.getResourceId());
            resourceAccountVo.setAccountId(tagentVo.getAccountId());
            resourceCenterMapper.insertIgnoreResourceAccount(new ArrayList<ResourceAccountVo>() {{
                this.add(resourceAccountVo);
            }});
        }

    }
}
