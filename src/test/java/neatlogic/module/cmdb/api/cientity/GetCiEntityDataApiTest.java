package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 验证关系返回契约及真实 Vo 数据转换，不连接数据库或启动 Spring。 */
public class GetCiEntityDataApiTest {
    /** 多个对端保持名称与旧版 ID 数组的顺序一致，反向关系取来源端名称。 */
    @Test
    public void returnsPeerNamesAlongsideLegacyIdsInBothDirections() throws Exception {
        CiEntityVo entity = entity();
        entity.addRelEntityData(10L, "from", relationData("目标模型", peer(9007199254740993L, "目标一"), peer(12L, "目标二")));
        entity.addRelEntityData(20L, "to", relationData("来源模型", peer(21L, "来源一")));
        JSONArray result = project(entity, Collections.emptyList(), Collections.emptyList());
        JSONObject from = find(result, 10L, "from");
        Assert.assertEquals(Arrays.asList(9007199254740993L, 12L), from.getJSONArray("value"));
        assertPeer(from, 0, 9007199254740993L, "目标一");
        assertPeer(from, 1, 12L, "目标二");
        Assert.assertEquals("目标模型", from.getString("ciLabel"));
        JSONObject to = find(result, 20L, "to");
        Assert.assertEquals(Collections.singletonList(21L), to.getJSONArray("value"));
        assertPeer(to, 0, 21L, "来源一");
        Assert.assertEquals("来源模型", to.getString("ciLabel"));
    }

    /** 同一关系在自关联模型中同时存在两个方向时，名称与别名不可相互覆盖。 */
    @Test
    public void separatesBothDirectionsOfSelfRelation() throws Exception {
        CiEntityVo entity = entity();
        entity.addRelEntityData(10L, "from", relationData("同模型", peer(11L, "下游")));
        entity.addRelEntityData(10L, "to", relationData("同模型", peer(12L, "上游")));
        JSONArray result = project(entity, Collections.emptyList(), Arrays.asList(alias("from", "依赖"), alias("to", "被依赖")));
        Assert.assertEquals(2, result.size());
        JSONObject from = find(result, 10L, "from");
        JSONObject to = find(result, 10L, "to");
        assertPeer(from, 0, 11L, "下游");
        assertPeer(to, 0, 12L, "上游");
        Assert.assertEquals("依赖", from.getString("label"));
        Assert.assertEquals("被依赖", to.getString("label"));
    }

    /** 仅一个方向有值时，另一个方向仍返回带正确别名的空数组。 */
    @Test
    public void keepsEmptyOppositeDirectionAndItsAlias() throws Exception {
        CiEntityVo entity = entity();
        entity.addRelEntityData(10L, "from", relationData("同模型", peer(11L, "下游")));
        JSONArray result = project(entity, Arrays.asList(relation("from"), relation("to")),
                Arrays.asList(alias("from", "依赖"), alias("to", "被依赖")));
        Assert.assertEquals(2, result.size());
        JSONObject empty = find(result, 10L, "to");
        Assert.assertTrue(empty.getJSONArray("value").isEmpty());
        Assert.assertTrue(empty.getJSONArray("valueList").isEmpty());
        Assert.assertEquals("被依赖", empty.getString("label"));
        Assert.assertEquals("来源模型", empty.getString("ciLabel"));
    }

    /** 无关系和无关系值均返回可直接遍历的数组，名称缺失时保留对端 ID。 */
    @Test
    public void supportsEmptyRelationsAndMissingNames() throws Exception {
        Assert.assertTrue(project(entity(), Collections.emptyList(), Collections.emptyList()).isEmpty());
        JSONArray empty = project(entity(), Arrays.asList(relation("from"), relation("to")), Collections.emptyList());
        Assert.assertEquals(2, empty.size());
        for (int i = 0; i < empty.size(); i++) {
            Assert.assertTrue(empty.getJSONObject(i).getJSONArray("value").isEmpty());
            Assert.assertTrue(empty.getJSONObject(i).getJSONArray("valueList").isEmpty());
        }
        CiEntityVo entity = entity();
        entity.addRelEntityData(10L, "from", relationData("模型", peer(11L, null)));
        JSONObject rel = find(project(entity, Collections.emptyList(), Collections.emptyList()), 10L, "from");
        assertPeer(rel, 0, 11L, null);
        Assert.assertEquals(Collections.singletonList(11L), rel.getJSONArray("value"));
    }

    /** 使用接口实际调用的内部投影方法，避免为测试添加生产装配接口。 */
    private JSONArray project(CiEntityVo entity, List<RelVo> definitions, List<CiViewVo> aliases) throws Exception {
        Method method = GetCiEntityDataApi.class.getDeclaredMethod("buildRelList", List.class, List.class, List.class);
        method.setAccessible(true);
        return (JSONArray) method.invoke(new GetCiEntityDataApi(), entity.getRelEntityList(), definitions, aliases);
    }

    /** 创建接口读取的配置项，关系由真实 Vo 恢复正反两端字段。 */
    private CiEntityVo entity() {
        CiEntityVo entity = new CiEntityVo();
        entity.setId(1L);
        entity.setCiId(2L);
        return entity;
    }

    /** 构造服务层已返回的单个关系对端。 */
    private JSONObject peer(Long id, String name) {
        JSONObject peer = new JSONObject();
        peer.put("ciEntityId", id);
        peer.put("ciEntityName", name);
        peer.put("ciId", 2L);
        return peer;
    }

    /** 构造服务层已有的关系值结构，包含目标模型及名称。 */
    private JSONObject relationData(String label, JSONObject... peers) {
        JSONObject data = new JSONObject();
        data.put("label", "关系");
        data.put("ciName", "model");
        data.put("ciLabel", label);
        data.put("valueList", new JSONArray(Arrays.asList(peers)));
        return data;
    }

    /** 构造同一模型中两个方向的关系定义。 */
    private RelVo relation(String direction) {
        RelVo rel = new RelVo();
        rel.setId(10L);
        rel.setDirection(direction);
        rel.setFromName("source");
        rel.setFromLabel("来源关系");
        rel.setFromCiId(2L);
        rel.setFromCiName("source-model");
        rel.setFromCiLabel("来源模型");
        rel.setToName("target");
        rel.setToLabel("目标关系");
        rel.setToCiId(2L);
        rel.setToCiName("target-model");
        rel.setToCiLabel("目标模型");
        return rel;
    }

    /** 生成按方向区分的关系视图别名。 */
    private CiViewVo alias(String direction, String label) {
        CiViewVo view = new CiViewVo();
        view.setItemId(10L);
        view.setType("rel" + direction);
        view.setAlias(label);
        return view;
    }

    /** 按公开契约查找关系，测试不依赖底层关系 Map 的遍历顺序。 */
    private JSONObject find(JSONArray list, Long id, String direction) {
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (id.equals(item.getLong("id")) && direction.equals(item.getString("direction"))) {
                return item;
            }
        }
        throw new AssertionError("缺少关系：" + id + "/" + direction);
    }

    /** 校验新增名称与兼容 ID 数组逐项对应。 */
    private void assertPeer(JSONObject relation, int index, Long id, String name) {
        JSONObject peer = relation.getJSONArray("valueList").getJSONObject(index);
        Assert.assertEquals(id, peer.getLong("id"));
        Assert.assertEquals(name, peer.getString("name"));
        Assert.assertEquals(id, relation.getJSONArray("value").getLong(index));
    }
}
