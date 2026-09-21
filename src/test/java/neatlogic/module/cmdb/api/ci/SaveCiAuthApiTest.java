package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.ci.CiAuthInvalidException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.rel.RelFilterI18nTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

/** 验证模型授权保存接口的单模型兼容和批量策略，不启动 Spring 或连接数据库。 */
@SuppressWarnings("unchecked")
public class SaveCiAuthApiTest extends RelFilterI18nTestBase {
    private TestSaveCiAuthApi api;
    private AuthMapperStub authMapperStub;
    private Map<Long, CiVo> ciMap;

    @Before
    public void setUp() throws Exception {
        api = new TestSaveCiAuthApi();
        authMapperStub = new AuthMapperStub();
        ciMap = new LinkedHashMap<>();
        putCi(1L, "普通模型一", 0);
        putCi(2L, "普通模型二", 0);
        putCi(3L, "虚拟模型", 1);
        setField(api, "ciAuthMapper", authMapperStub.proxy());
        setField(api, "ciMapper", ciMapperProxy());
    }

    /** 旧版 ciId 请求继续覆盖原授权，虚拟模型只写入管理和查询权限。 */
    @Test
    public void legacyRequestReplacesAndFiltersVirtualActions() throws Exception {
        authMapperStub.authList.add(auth(3L, "cimanage", "role", "old"));
        JSONObject input = new JSONObject();
        input.put("ciId", 3L);
        input.put("authList", authArray(
                authObject("cimanage", "role", "new"),
                authObject("cientityquery", "team", "team-a"),
                authObject("cientitydelete", "user", "user-a")
        ));

        api.myDoService(input);

        Assert.assertEquals(2, authMapperStub.authList.size());
        Assert.assertTrue(authMapperStub.has(3L, "cimanage", "role", "new"));
        Assert.assertTrue(authMapperStub.has(3L, "cientityquery", "team", "team-a"));
        Assert.assertFalse(authMapperStub.hasAction(3L, "cientitydelete"));
    }

    /** 追加模型保留原授权，覆盖模型先清空；重复模型和授权项只处理一次。 */
    @Test
    public void batchRequestSupportsMixedStrategiesAndDeduplication() throws Exception {
        authMapperStub.authList.add(auth(1L, "cimanage", "role", "old-role"));
        authMapperStub.authList.add(auth(2L, "cientitydelete", "user", "old-user"));
        JSONObject input = new JSONObject();
        input.put("appendCiIdList", new JSONArray(Arrays.asList(1L, 1L)));
        input.put("replaceCiIdList", new JSONArray(Arrays.asList(2L, 2L)));
        JSONObject newAuth = authObject("cientityquery", "role", "query-role");
        input.put("authList", authArray(newAuth, newAuth));

        api.myDoService(input);

        Assert.assertEquals(3, authMapperStub.authList.size());
        Assert.assertTrue(authMapperStub.has(1L, "cimanage", "role", "old-role"));
        Assert.assertTrue(authMapperStub.has(1L, "cientityquery", "role", "query-role"));
        Assert.assertTrue(authMapperStub.has(2L, "cientityquery", "role", "query-role"));
        Assert.assertFalse(authMapperStub.hasAction(2L, "cientitydelete"));
    }

    /** 空授权列表对追加模型不产生写入，对覆盖模型执行清空。 */
    @Test
    public void emptyAuthListKeepsAppendAndClearsReplace() throws Exception {
        authMapperStub.authList.add(auth(1L, "cimanage", "role", "role-a"));
        authMapperStub.authList.add(auth(2L, "cimanage", "role", "role-b"));
        JSONObject input = new JSONObject();
        input.put("appendCiIdList", new JSONArray(Arrays.asList(1L)));
        input.put("replaceCiIdList", new JSONArray(Arrays.asList(2L)));

        api.myDoService(input);

        Assert.assertEquals(1, authMapperStub.authList.size());
        Assert.assertTrue(authMapperStub.has(1L, "cimanage", "role", "role-a"));
    }

    /** 目标策略交叉或出现数据库不支持的动作时，必须在任何写入前失败。 */
    @Test
    public void invalidInputFailsBeforeMutation() {
        authMapperStub.authList.add(auth(1L, "cimanage", "role", "role-a"));
        JSONObject overlapInput = new JSONObject();
        overlapInput.put("appendCiIdList", new JSONArray(Arrays.asList(1L)));
        overlapInput.put("replaceCiIdList", new JSONArray(Arrays.asList(1L)));
        Assert.assertThrows(CiAuthInvalidException.class, () -> api.myDoService(overlapInput));

        JSONObject unsupportedActionInput = new JSONObject();
        unsupportedActionInput.put("appendCiIdList", new JSONArray(Arrays.asList(1L)));
        unsupportedActionInput.put("authList", authArray(authObject("accountmanagement", "role", "role-a")));
        Assert.assertThrows(CiAuthInvalidException.class, () -> api.myDoService(unsupportedActionInput));

        JSONObject mixedContractInput = new JSONObject();
        mixedContractInput.put("ciId", 1L);
        mixedContractInput.put("appendCiIdList", new JSONArray());
        Assert.assertThrows(CiAuthInvalidException.class, () -> api.myDoService(mixedContractInput));

        Assert.assertEquals(1, authMapperStub.authList.size());
        Assert.assertEquals(0, authMapperStub.writeCount);
    }

    /** 任一目标模型不存在时，覆盖目标也不能提前删除。 */
    @Test
    public void missingCiFailsBeforeMutation() {
        authMapperStub.authList.add(auth(1L, "cimanage", "role", "role-a"));
        JSONObject input = new JSONObject();
        input.put("replaceCiIdList", new JSONArray(Arrays.asList(1L, 999L)));

        Assert.assertThrows(CiNotFoundException.class, () -> api.myDoService(input));
        Assert.assertEquals(1, authMapperStub.authList.size());
        Assert.assertEquals(0, authMapperStub.writeCount);
    }

    /** 任一模型缺少管理权限时，所有覆盖和追加目标都不能产生写入。 */
    @Test
    public void unauthorizedCiFailsBeforeMutation() {
        api.hasGlobalPrivilege = false;
        api.hasCiPrivilege = false;
        authMapperStub.authList.add(auth(1L, "cimanage", "role", "role-a"));
        JSONObject input = new JSONObject();
        input.put("replaceCiIdList", new JSONArray(Arrays.asList(1L, 2L)));

        Assert.assertThrows(CiAuthException.class, () -> api.myDoService(input));
        Assert.assertEquals(1, authMapperStub.authList.size());
        Assert.assertEquals(0, authMapperStub.writeCount);
    }

    /** 超过单批上限时，模型读取、删除和写入都按批次执行。 */
    @Test
    public void largeRequestIsPartitioned() throws Exception {
        JSONArray ciIdList = new JSONArray();
        for (long ciId = 10L; ciId < 511L; ciId++) {
            putCi(ciId, "模型" + ciId, 0);
            ciIdList.add(ciId);
        }
        JSONObject input = new JSONObject();
        input.put("replaceCiIdList", ciIdList);
        input.put("authList", authArray(authObject("cientityquery", "role", "query-role")));

        api.myDoService(input);

        Assert.assertEquals(2, authMapperStub.deleteBatchCount);
        Assert.assertEquals(2, authMapperStub.insertBatchCount);
        Assert.assertEquals(501, authMapperStub.authList.size());
    }

    private void putCi(Long id, String label, Integer isVirtual) {
        CiVo ciVo = new CiVo();
        ciVo.setId(id);
        ciVo.setLabel(label);
        ciVo.setIsVirtual(isVirtual);
        ciMap.put(id, ciVo);
    }

    private CiMapper ciMapperProxy() {
        return (CiMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CiMapper.class},
                (proxy, method, args) -> {
                    if ("getCiByIdList".equals(method.getName())) {
                        List<CiVo> resultList = new ArrayList<>();
                        for (Long ciId : (List<Long>) args[0]) {
                            if (ciMap.containsKey(ciId)) {
                                resultList.add(ciMap.get(ciId));
                            }
                        }
                        return resultList;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SaveCiAuthApi.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static JSONArray authArray(JSONObject... authObjects) {
        JSONArray authArray = new JSONArray();
        authArray.addAll(Arrays.asList(authObjects));
        return authArray;
    }

    private static JSONObject authObject(String action, String authType, String authUuid) {
        JSONObject authObject = new JSONObject();
        authObject.put("action", action);
        authObject.put("authType", authType);
        authObject.put("authUuid", authUuid);
        return authObject;
    }

    private static CiAuthVo auth(Long ciId, String action, String authType, String authUuid) {
        CiAuthVo authVo = new CiAuthVo();
        authVo.setCiId(ciId);
        authVo.setAction(action);
        authVo.setAuthType(authType);
        authVo.setAuthUuid(authUuid);
        return authVo;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType.equals(int.class)) {
            return 0;
        } else if (returnType.equals(long.class)) {
            return 0L;
        } else if (returnType.equals(boolean.class)) {
            return false;
        }
        return null;
    }

    /** 单元测试直接授予全局模型管理权限，避免依赖线程上下文。 */
    private static class TestSaveCiAuthApi extends SaveCiAuthApi {
        private boolean hasGlobalPrivilege = true;
        private boolean hasCiPrivilege = true;

        @Override
        protected boolean hasGlobalCiManagePrivilege() {
            return hasGlobalPrivilege;
        }

        @Override
        protected boolean hasCiManagePrivilege(List<CiAuthVo> ciAuthList) {
            return hasCiPrivilege;
        }
    }

    /** 使用内存列表模拟授权表，并记录实际批次数。 */
    private static class AuthMapperStub {
        private final List<CiAuthVo> authList = new ArrayList<>();
        private int writeCount;
        private int deleteBatchCount;
        private int insertBatchCount;

        private CiAuthMapper proxy() {
            return (CiAuthMapper) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CiAuthMapper.class},
                    (proxy, method, args) -> {
                        if ("deleteCiAuthByCiIdList".equals(method.getName())) {
                            deleteBatchCount++;
                            writeCount++;
                            Set<Long> ciIdSet = new HashSet<>((List<Long>) args[0]);
                            Iterator<CiAuthVo> iterator = authList.iterator();
                            while (iterator.hasNext()) {
                                if (ciIdSet.contains(iterator.next().getCiId())) {
                                    iterator.remove();
                                }
                            }
                            return 0;
                        } else if ("insertCiAuthList".equals(method.getName())) {
                            insertBatchCount++;
                            writeCount++;
                            for (CiAuthVo authVo : (List<CiAuthVo>) args[0]) {
                                if (!has(authVo.getCiId(), authVo.getAction(), authVo.getAuthType(), authVo.getAuthUuid())) {
                                    authList.add(authVo);
                                }
                            }
                            return 0;
                        } else if ("getCiAuthByCiIdList".equals(method.getName())) {
                            Set<Long> ciIdSet = new HashSet<>((List<Long>) args[0]);
                            List<CiAuthVo> resultList = new ArrayList<>();
                            for (CiAuthVo authVo : authList) {
                                if (ciIdSet.contains(authVo.getCiId())) {
                                    resultList.add(authVo);
                                }
                            }
                            return resultList;
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private boolean has(Long ciId, String action, String authType, String authUuid) {
            for (CiAuthVo authVo : authList) {
                if (ciId.equals(authVo.getCiId()) && action.equals(authVo.getAction())
                        && authType.equals(authVo.getAuthType()) && authUuid.equals(authVo.getAuthUuid())) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasAction(Long ciId, String action) {
            for (CiAuthVo authVo : authList) {
                if (ciId.equals(authVo.getCiId()) && action.equals(authVo.getAction())) {
                    return true;
                }
            }
            return false;
        }
    }
}
