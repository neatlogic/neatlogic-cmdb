/*
 * Copyright (C) 2026  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.service.resourcecenter.account;

import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import org.junit.Test;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

/** 校验与保存共用账号定义规则，失败前不允许访问账号写入方法。 */
public class ResourceAccountDefinitionValidationTest {
    /** 协议不存在、登录账号为空、公共名称冲突和 tagent 创建均在预检被拦截。 */
    @Test public void rejectsSameDefinitionsBeforeSave() {
        for (String problem : new String[]{"protocol", "login", "duplicate", "tagent"}) {
            ResourceCenterAccountServiceImpl service = new ResourceCenterAccountServiceImpl();
            service.resourceAccountMapper = (ResourceAccountMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResourceAccountMapper.class}, (bean, method, args) -> {
                        if (method.getName().equals("getAccountProtocolVoByProtocolId")) {
                            if (problem.equals("protocol")) return null;
                            AccountProtocolVo protocol = new AccountProtocolVo();
                            protocol.setId(1L);
                            protocol.setName(problem.equals("tagent") ? "tagent" : "ssh");
                            return protocol;
                        }
                        if (method.getName().equals("checkAccountNameIsRepeats")) return problem.equals("duplicate") ? 1 : 0;
                        throw new AssertionError("预检不应调用 " + method.getName());
                    });
            AccountVo account = new AccountVo();
            account.setProtocolId(1L);
            account.setAccount(problem.equals("login") ? null : "ops");
            account.setName("test");
            account.setType("public");
            ApiRuntimeException preview = assertThrows(ApiRuntimeException.class, () -> service.validateAccountDefinition(account, true));
            ApiRuntimeException save = assertThrows(ApiRuntimeException.class, () -> service.saveAccount(null, account));
            assertEquals(preview.getClass(), save.getClass());
        }
    }
}
