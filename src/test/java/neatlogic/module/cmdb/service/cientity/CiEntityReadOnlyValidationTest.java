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

package neatlogic.module.cmdb.service.cientity;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 确保配置预检不触发 CMDB 原校验流程中的续期写入。 */
public class CiEntityReadOnlyValidationTest {
    /** 同一份空变更通过原有规则，只读入口不更新，正式入口仍续期。 */
    @Test public void readOnlyDoesNotRenewStoredEntity() throws Exception {
        CiEntityServiceImpl service = new CiEntityServiceImpl();
        AtomicInteger writes = new AtomicInteger();
        for (String name : List.of("attrMapper", "relMapper", "globalAttrMapper", "ciMapper", "ciEntityMapper")) {
            Field field = CiEntityServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(service, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{field.getType()}, (bean, method, args) -> {
                if (method.getName().equals("getCiById")) {
                    CiVo ci = new CiVo();
                    ci.setId(1L);
                    return ci;
                }
                if (method.getName().equals("updateCiEntityBaseInfo")) {
                    writes.incrementAndGet();
                    return 1;
                }
                if (List.class.isAssignableFrom(method.getReturnType())) return List.of();
                throw new AssertionError(method.getName());
            }));
        }
        assertFalse(service.validateCiEntityTransactionReadOnly(transaction()));
        assertEquals(0, writes.get());
        assertFalse(service.validateCiEntityTransaction(transaction()));
        assertEquals(1, writes.get());
    }

    /** 带旧实体上下文的局部事务副本，无需创建正式记录。 */
    private CiEntityTransactionVo transaction() {
        CiEntityTransactionVo value = new CiEntityTransactionVo();
        value.setCiId(1L);
        value.setCiEntityId(2L);
        value.setOldCiEntityVo(new CiEntityVo());
        return value;
    }
}
