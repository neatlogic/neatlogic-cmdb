/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.cmdb.crossover.IBatchSaveCiEntityPublicApiCrossoverService;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.EditModeType;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.MyApiComponent;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentFactory;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class BatchSaveCiEntityPublicApi extends PublicApiComponentBase implements IBatchSaveCiEntityPublicApiCrossoverService {
    @Resource
    private CiService ciService;

    @Resource
    private CiMapper ciMapper;
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getName() {
        return "保存配置项public";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "配置项数据"),
            @Param(name = "needCommit", type = ApiParamType.BOOLEAN, isRequired = true, desc = "是否需要提交")})
    @Description(desc = "保存配置项public 简化接口，attrEntityData 仅需传 属性name:属性value。注意：默认为局部修改，如需要全部修改则需要在每个entity对象声明 editMode=global")
    @Example(example = "{\"ciEntityList\":[{\"entityData\":{\"name\":[\"名称s4\"],\"description\":[\"描述s\"],\"state\":[\"下线\"],\"APPComponent\":[\"应用模块11\"],\"owner\":[\"林邦泉2\"],\"bg\":[\"部门1\"],\"data_center\":[\"MAIN\"],\"maintenance_window\":[\"20:00~22:00\"],\"APPIns\":[\"a\"]},\"ciName\":\"APP\",\"name\":\"名称s3\",\"editMode\":\"global\"}],\"needCommit\":true}")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        JSONArray ciEntityResultArray = new JSONArray();
        result.put("needCommit", jsonObj.getBooleanValue("needCommit"));
        result.put("ciEntityList", ciEntityResultArray);
        JSONArray ciEntityObjArrayParam = jsonObj.getJSONArray("ciEntityList");
        Map<Long, CiVo> ciMap = new HashMap<>();
        //遍历ciEntityList入参
        for (int i = 0; i < ciEntityObjArrayParam.size(); i++) {
            JSONObject ciEntityObj = ciEntityObjArrayParam.getJSONObject(i);
            JSONObject entityDataParam = ciEntityObj.getJSONObject("entityData");
            String ciName = ciEntityObj.getString("ciName");
            String ciEntityName = ciEntityObj.getString("name");
            String editMode = EditModeType.PARTIAL.getValue();//默认局部修改
            if (ciEntityObj.containsKey("editMode")) {
                editMode = ciEntityObj.getString("editMode");
            }
            CiVo ciVo = ciMapper.getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
            //建立属性映射
            Map<String, AttrVo> attrMap = new HashMap<>();
            Map<String, RelVo> relMap = new HashMap<>();
            ciVo = ciService.getCiById(ciVo.getId());
            for (AttrVo attr : ciVo.getAttrList()) {
                attrMap.put(attr.getName(), attr);
            }
            for (RelVo rel : ciVo.getRelList()) {
                if (StringUtils.isNotBlank(rel.getFromCiName())) {
                    relMap.put(rel.getFromCiName(), rel);
                }
                if (StringUtils.isNotBlank(rel.getToCiName())) {
                    relMap.put(rel.getToCiName(), rel);
                }
            }
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByName(new CiEntityVo(ciVo.getId(), ciEntityName));
            if (CollectionUtils.isEmpty(ciEntityList)) {
                ciEntityResultArray.add(getCiEntityResultDate(ciVo, entityDataParam, attrMap, relMap, null, ciMap, editMode));
            } else {
                //如果一个entityName找到多个entity，直接给多个数据
                //TODO 还得根据唯一规则过滤出ciEntityList
                for (CiEntityVo ciEntityVo : ciEntityList) {
                    ciEntityResultArray.add(getCiEntityResultDate(ciVo, entityDataParam, attrMap, relMap, ciEntityVo.getId(), ciMap, editMode));
                }
            }
        }
        //调用内部保存配置项接口(transactional)
        MyApiComponent component = (MyApiComponent) PrivateApiComponentFactory.getInstance(BatchSaveCiEntityApi.class.getName());
        component.myDoService(result);
        return null;
    }

    /**
     * @param ciVo            ci对象
     * @param entityDataParam entity属性或关系入参
     * @param attrMap         ci属性map
     * @param relMap          ci关系map
     * @param ciEntityId      entity Id
     * @param ciMap           ci 缓存
     * @param editMode        global：全局，partial：局部
     * @return 转换后的配置项入参
     */
    private JSONObject getCiEntityResultDate(CiVo ciVo, JSONObject entityDataParam, Map<String, AttrVo> attrMap, Map<String, RelVo> relMap, Long ciEntityId, Map<Long, CiVo> ciMap, String editMode) {
        JSONObject ciEntityResult = JSONObject.parseObject(JSONObject.toJSONString(ciVo));
        ciEntityResult.put("ciId", ciVo.getId());
        ciEntityResult.put("id", null);
        JSONObject attrEntityData = new JSONObject();
        if (ciEntityId != null) {
            ciEntityResult.put("id", ciEntityId);
        } else {
            ciEntityResult.put("uuid", UUID.randomUUID().toString().replace("-", ""));
        }
        ciEntityResult.put("editMode", editMode);
        ciEntityResult.put("attrEntityData", attrEntityData);
        JSONObject relEntityData = new JSONObject();
        ciEntityResult.put("relEntityData", relEntityData);
        //遍历入参属性 key value,转换为对应id
        for (Map.Entry<String, Object> attrEntity : entityDataParam.entrySet()) {
            String entityKey = attrEntity.getKey();
            JSONArray entityValueArray = JSONArray.parseArray(JSONArray.toJSONString(attrEntity.getValue()));
            if (attrMap.containsKey(entityKey)) {
                JSONObject ciEntityAttr = new JSONObject();
                AttrVo attrVo = attrMap.get(entityKey);
                ciEntityAttr.put("type", attrVo.getType());
                attrEntityData.put("attr_" + attrVo.getId(), ciEntityAttr);
                JSONArray valueList = new JSONArray();
                JSONArray actualValueList = new JSONArray();
                if (attrVo.getTargetCiId() != null) {
                    for (int j = 0; j < entityValueArray.size(); j++) {
                        String value = entityValueArray.getString(j);
                        if (StringUtils.isNotBlank(value)) {
                            value = value.trim();
                            List<CiEntityVo> targetCiEntityList = getCiEntityBaseInfoByName(attrVo.getTargetCiId(), value, ciMap);
                            if (CollectionUtils.isNotEmpty(targetCiEntityList)) {
                                valueList.addAll(targetCiEntityList.stream().map(CiEntityVo::getId).distinct().collect(Collectors.toList()));
                                actualValueList.addAll(targetCiEntityList.stream().map(CiEntityVo::getName).distinct().collect(Collectors.toList()));

                            } else {
                                throw new CiEntityNotFoundException(value);
                            }
                        }
                    }
                } else {
                    valueList.add(entityValueArray.getString(0));
                }
                ciEntityAttr.put("valueList", valueList);
                ciEntityAttr.put("actualValueList", actualValueList);
            }
            //遍历入参关系 key value,转换为对应id

            if (relMap.containsKey(entityKey)) {
                JSONObject ciEntityRel = new JSONObject();
                RelVo relVo = relMap.get(entityKey);
                relEntityData.put("rel" + relVo.getDirection() + "_" + relVo.getId(), ciEntityRel);
                JSONArray valueList = new JSONArray();
                for (int j = 0; j < entityValueArray.size(); j++) {
                    String value = entityValueArray.getString(j);
                    if (StringUtils.isNotBlank(value)) {
                        value = value.trim();
                        List<CiEntityVo> targetCiEntityList = getCiEntityBaseInfoByName(relVo.getDirection().equals(RelDirectionType.FROM.getValue()) ? relVo.getToCiId() : relVo.getFromCiId(), value, ciMap);
                        if (CollectionUtils.isNotEmpty(targetCiEntityList)) {
                            for (CiEntityVo entity : targetCiEntityList) {
                                JSONObject valueRel = new JSONObject();
                                valueList.add(valueRel);
                                valueRel.put("ciId", entity.getCiId());
                                valueRel.put("ciEntityId", entity.getId());
                                valueRel.put("ciEntityName", entity.getName());
                            }
                        } else {
                            throw new CiEntityNotFoundException(value);
                        }
                    }
                }
                ciEntityRel.put("valueList", valueList);
            }
        }
        return ciEntityResult;
    }

    /**
     * 根据配置项值获取配置项
     *
     * @param ciId  配置模型id
     * @param name  配置项值
     * @param ciMap 缓存
     * @return 配置项列表
     */
    private List<CiEntityVo> getCiEntityBaseInfoByName(Long ciId, String name, Map<Long, CiVo> ciMap) {
        if (!ciMap.containsKey(ciId)) {
            ciMap.put(ciId, ciMapper.getCiById(ciId));
        }
        CiVo ciVo = ciMap.get(ciId);
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(ciId);
        ciEntityVo.setName(name);
        if (ciVo.getIsVirtual().equals(0)) {
            return ciEntityMapper.getCiEntityBaseInfoByName(ciEntityVo);
        } else {
            return ciEntityMapper.getVirtualCiEntityBaseInfoByName(ciEntityVo);
        }
    }

}
