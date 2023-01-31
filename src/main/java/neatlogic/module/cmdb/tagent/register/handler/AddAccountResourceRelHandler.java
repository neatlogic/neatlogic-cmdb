/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.tagent.register.handler;

import neatlogic.framework.cmdb.dto.resourcecenter.AccountIpVo;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.tagent.register.core.AfterRegisterBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 添加资源和账号的关联关系
 */
@Service
public class AddAccountResourceRelHandler extends AfterRegisterBase {

    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    /**
     * 提前写入关系，当资源中心ready后自然就关联上账号，每次注册都会写入新的resource_id，所以关系表可能会存在垃圾数据，暂时没有好办法解决
     * 而且由于配置项是根据ip来定位的，只要第一次注册成功，后面如果密码更新也没法关联到老的配置项
     *
     * @param tagentVo tagent对象
     */
    @Override
    public void myExecute(TagentVo tagentVo) {
        if (tagentVo != null && StringUtils.isNotBlank(tagentVo.getIp()) && tagentVo.getAccountId() != null) {
            resourceAccountMapper.insertAccountIp(new AccountIpVo(tagentVo.getAccountId(), tagentVo.getIp()));
        }
    }
}
