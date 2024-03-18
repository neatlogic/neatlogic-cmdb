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
