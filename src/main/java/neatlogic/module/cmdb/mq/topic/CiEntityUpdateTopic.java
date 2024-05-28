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

package neatlogic.module.cmdb.mq.topic;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.GlobalAttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.mq.core.TopicBase;
import neatlogic.framework.mq.dto.TopicVo;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
public class CiEntityUpdateTopic extends TopicBase<CiEntityTransactionVo> {
    @Override
    public String getName() {
        return "cmdb/cientity/update";
    }

    @Override
    public String getLabel() {
        return "配置项修改";
    }

    @Resource
    CiEntityService ciEntityService;

    @Resource
    CiMapper ciMapper;

    @Resource
    AttrMapper attrMapper;

    @Resource
    GlobalAttrMapper globalAttrMapper;

    @Resource
    RelMapper relMapper;

    @Override
    public String getDescription() {
        return "配置项修改并生效后触发此主题";
    }

    @Override
    protected JSONObject generateTopicContent(TopicVo topicVo, CiEntityTransactionVo content) {
        if(topicVo == null){
            return null;
        }
        if (topicVo.getConfig() == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(topicVo.getConfig().getJSONArray("ciIdList"))) {
            return null;
        }
        JSONArray ciIdList = topicVo.getConfig().getJSONArray("ciIdList");
        boolean isExists = false;
        for (int i = 0; i < ciIdList.size(); i++) {
            if (ciIdList.getLong(i).equals(content.getCiId())) {
                isExists = true;
                break;
            }
        }
        if (!isExists) {
            return null;
        }
        Long ciEntityId = content.getCiEntityId();
        CiEntityVo ciEntityBaseVo = ciEntityService.getCiEntityBaseInfoById(ciEntityId);
        if (ciEntityBaseVo == null) {
            return null;
        }
        Long ciId = ciEntityBaseVo.getCiId();
        Boolean limitRelEntity = false;
        Boolean limitAttrEntity = false;
        CiVo ciVo = ciMapper.getCiById(ciId);
        CiEntityVo pCiEntityVo = new CiEntityVo();
        pCiEntityVo.setId(ciEntityId);
        pCiEntityVo.setCiId(ciId);
        pCiEntityVo.setLimitRelEntity(limitRelEntity);
        pCiEntityVo.setLimitAttrEntity(limitAttrEntity);
        CiEntityVo entity = ciEntityService.getCiEntityById(pCiEntityVo);
        entity.setIsVirtual(ciVo.getIsVirtual());
        if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(entity.getCiId()).checkCiEntityIsInGroup(entity.getId(), GroupType.READONLY, GroupType.MAINTAIN, GroupType.AUTOEXEC).check()) {
            throw new CiEntityAuthException(entity.getCiLabel(), TransactionActionType.VIEW.getText());
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        List<RelVo> relList = relMapper.getRelByCiId(ciId);
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.getGlobalAttrByCiId(ciId);
        JSONObject entityObj = new JSONObject();
        entityObj.put("id", entity.getId());
        entityObj.put("uuid", entity.getUuid());
        entityObj.put("name", entity.getName());
        entityObj.put("ciId", entity.getCiId());
        entityObj.put("ciName", entity.getCiName());
        entityObj.put("ciIcon", entity.getCiIcon());
        entityObj.put("ciLabel", entity.getCiLabel());
        entityObj.put("type", entity.getTypeId());
        entityObj.put("typeName", entity.getTypeName());

        JSONArray globalAttrObjList = new JSONArray();
        if (CollectionUtils.isNotEmpty(entity.getGlobalAttrEntityList())) {
            for (GlobalAttrEntityVo attrEntityVo : entity.getGlobalAttrEntityList()) {
                JSONObject attrObj = new JSONObject();
                attrObj.put("name", attrEntityVo.getAttrName());
                attrObj.put("label", attrEntityVo.getAttrLabel());
                attrObj.put("value", attrEntityVo.getValueList());
                globalAttrObjList.add((attrObj));
            }
        }
        if (CollectionUtils.isNotEmpty(globalAttrList)) {
            for (GlobalAttrVo attr : globalAttrList) {
                if (entity.getGlobalAttrEntityList().stream().noneMatch(d -> d.getAttrId().equals(attr.getId()))) {
                    JSONObject attrObj = new JSONObject();
                    attrObj.put("name", attr.getName());
                    attrObj.put("label", attr.getLabel());
                    attrObj.put("value", new JSONArray());
                    globalAttrObjList.add((attrObj));
                }
            }
        }
        entityObj.put("globalAttrList", globalAttrObjList);

        JSONArray attrObjList = new JSONArray();
        if (CollectionUtils.isNotEmpty(entity.getAttrEntityList())) {
            for (AttrEntityVo attrEntityVo : entity.getAttrEntityList()) {
                JSONObject attrObj = new JSONObject();
                attrObj.put("id", attrEntityVo.getId());
                attrObj.put("name", attrEntityVo.getAttrName());
                attrObj.put("label", attrEntityVo.getAttrLabel());
                attrObj.put("type", attrEntityVo.getAttrType());
                if (attrEntityVo.getToCiId() != null) {
                    attrObj.put("targetCiId", attrEntityVo.getToCiId());
                }
                attrObj.put("value", attrEntityVo.getActualValueList());
                attrObjList.add((attrObj));
            }
        }
        //补充值为空的属性值
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (AttrVo attr : attrList) {
                if (entity.getAttrEntityList().stream().noneMatch(d -> d.getAttrId().equals(attr.getId()))) {
                    JSONObject attrObj = new JSONObject();
                    attrObj.put("id", attr.getId());
                    attrObj.put("name", attr.getName());
                    attrObj.put("label", attr.getLabel());
                    attrObj.put("type", attr.getType());
                    attrObj.put("targetCiId", attr.getTargetCiId());
                    attrObj.put("value", new JSONArray());
                    attrObjList.add((attrObj));
                }
            }
        }
        entityObj.put("attrList", attrObjList);
        JSONArray relObjList = new JSONArray();
        if (CollectionUtils.isNotEmpty(entity.getRelEntityList())) {
            for (RelEntityVo relEntityVo : entity.getRelEntityList()) {
                Optional<Object> op = relObjList.stream().filter(d -> ((JSONObject) d).getLong("id").equals(relEntityVo.getRelId())).findFirst();
                JSONObject relObj;
                if (op.isPresent()) {
                    relObj = (JSONObject) op.get();
                } else {
                    relObj = new JSONObject();
                    relObj.put("value", new JSONArray());
                    relObjList.add((relObj));
                }
                relObj.put("id", relEntityVo.getRelId());
                relObj.put("name", relEntityVo.getRelName());
                relObj.put("label", relEntityVo.getRelLabel());
                relObj.put("direction", relEntityVo.getDirection());
                if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                    relObj.getJSONArray("value").add(relEntityVo.getToCiEntityId());
                } else {
                    relObj.getJSONArray("value").add(relEntityVo.getFromCiEntityId());
                }

            }
        }
        //补充值为空的属性值
        if (CollectionUtils.isNotEmpty(relList)) {
            for (RelVo rel : relList) {
                if (entity.getRelEntityList().stream().noneMatch(d -> d.getRelId().equals(rel.getId()))) {
                    JSONObject relObj = new JSONObject();
                    relObj.put("id", rel.getId());
                    if (rel.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        relObj.put("name", rel.getToName());
                        relObj.put("label", rel.getToLabel());
                    } else {
                        relObj.put("name", rel.getFromName());
                        relObj.put("label", rel.getFromLabel());
                    }
                    relObj.put("direction", rel.getDirection());
                    relObj.put("value", new JSONArray());
                    relObjList.add((relObj));
                }
            }
        }
        entityObj.put("relList", relObjList);

        return entityObj;
    }
}
