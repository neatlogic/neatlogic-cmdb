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

import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceAccountVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceAccountBindingConflictException;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/** 核对审批追加与解除账号时，仅修改已确认的那一条绑定。 */
public class ResourceBindingBaselineTest {
    private final Set<Long> bound = new LinkedHashSet<>(List.of(11L, 12L));
    private final ResourceCenterAccountServiceImpl service = new ResourceCenterAccountServiceImpl();

    /** 用隔离的账号绑定集合替代正式资源库。 */
    @Before public void prepareMapper() {
        service.resourceAccountMapper = (ResourceAccountMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResourceAccountMapper.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "lockBoundAccountIds": return new ArrayList<>(bound);
                        case "getAccountById":
                            AccountVo account = new AccountVo();
                            account.setId((Long) args[0]);
                            account.setType("public");
                            return account;
                        case "getResourceAccountListByResourceId": return new ArrayList<>();
                        case "insertIgnoreResourceAccount":
                            bound.add(((ResourceAccountVo) ((List<?>) args[0]).get(0)).getAccountId()); return 1;
                        case "deleteResourceAccountByResourceIdListAndAccountIdList":
                            bound.removeAll((List<?>) args[1]); return 1;
                        default: throw new AssertionError(method.getName());
                    }
                });
    }

    /** 审批期间新增的无关绑定必须保留。 */
    @Test public void preservesUnrelatedBindings() {
        bound.add(18L);
        service.changePublicBinding(1L, 15L, false, true);
        service.changePublicBinding(1L, 12L, true, false);
        assertEquals(Set.of(11L, 15L, 18L), bound);
    }

    /** 同一绑定的实际状态偏离基线时，审批须失败且不改变其他关系。 */
    @Test public void rejectsSameBindingChangedAfterSubmission() {
        assertThrows(ResourceAccountBindingConflictException.class, () -> service.changePublicBinding(1L, 11L, false, true));
        assertThrows(ResourceAccountBindingConflictException.class, () -> service.changePublicBinding(1L, 15L, true, false));
        assertEquals(Set.of(11L, 12L), bound);
    }
}
