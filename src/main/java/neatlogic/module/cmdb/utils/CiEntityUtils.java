/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
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

package neatlogic.module.cmdb.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.enums.EditModeType;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.exception.cientity.NewCiEntityNotFoundException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CiEntityUtils {
    private static CiEntityMapper ciEntityMapper;

    @Autowired
    public CiEntityUtils(CiEntityMapper _ciEntityMapper) {
        ciEntityMapper = _ciEntityMapper;
    }


    public static List<CiEntityTransactionVo> generateCiEntityTransaction(JSONArray ciEntityObjList) {
        List<CiEntityTransactionVo> ciEntityTransactionList = new ArrayList<>();
        Map<String, CiEntityTransactionVo> ciEntityTransactionMap = new HashMap<>();
        // 先给所有没有id的ciEntity分配新的id
        for (int ciindex = 0; ciindex < ciEntityObjList.size(); ciindex++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(ciindex);
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            if (StringUtils.isNotBlank(uuid)) {
                CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo();
                if (id != null) {
                    ciEntityTransactionVo.setCiEntityId(id);
                } else {
                    CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(uuid);
                    if (uuidCiEntityVo != null) {
                        ciEntityTransactionVo.setCiEntityId(uuidCiEntityVo.getId());
                        ciEntityObj.put("id", uuidCiEntityVo.getId());
                    }
                }
                ciEntityTransactionMap.put(uuid, ciEntityTransactionVo);
            }
        }
        for (int ciindex = 0; ciindex < ciEntityObjList.size(); ciindex++) {
            JSONObject ciEntityObj = ciEntityObjList.getJSONObject(ciindex);
            Long ciId = ciEntityObj.getLong("ciId");
            Long id = ciEntityObj.getLong("id");
            String uuid = ciEntityObj.getString("uuid");
            String description = ciEntityObj.getString("description");
            CiEntityTransactionVo ciEntityTransactionVo;

            if (id != null) {
                ciEntityTransactionVo = new CiEntityTransactionVo();
                ciEntityTransactionVo.setCiEntityId(id);
                ciEntityTransactionVo.setAction(TransactionActionType.UPDATE.getValue());
            } else if (StringUtils.isNotBlank(uuid)) {
                ciEntityTransactionVo = ciEntityTransactionMap.get(uuid);
                ciEntityTransactionVo.setCiEntityUuid(uuid);
                ciEntityTransactionVo.setAction(TransactionActionType.INSERT.getValue());
            } else {
                throw new ParamNotExistsException("id", "uuid");
            }

            if (Objects.equals(ciEntityObj.getString("editMode"), EditModeType.GLOBAL.getValue()) || Objects.equals(ciEntityObj.getString("editMode"), EditModeType.PARTIAL.getValue())) {
                ciEntityTransactionVo.setEditMode(ciEntityObj.getString("editMode"));
            }
            ciEntityTransactionVo.setCiId(ciId);
            ciEntityTransactionVo.setDescription(description);

            // 解析属性数据
            JSONObject attrObj = ciEntityObj.getJSONObject("attrEntityData");
            //修正新配置项的uuid为id
            if (MapUtils.isNotEmpty(attrObj)) {
                for (String key : attrObj.keySet()) {
                    JSONObject obj = attrObj.getJSONObject(key);
                    JSONArray valueList = obj.getJSONArray("valueList");
                    //删除没用的属性
                    obj.remove("actualValueList");
                    if (CollectionUtils.isNotEmpty(valueList)) {
                        //因为可能需要删除某些成员，所以需要倒着循环
                        for (int i = valueList.size() - 1; i >= 0; i--) {
                            if (valueList.get(i) instanceof JSONObject) {
                                JSONObject valueObj = valueList.getJSONObject(i);
                                String attrCiEntityUuid = valueObj.getString("uuid");
                                Long attrCiEntityId = valueObj.getLong("id");
                                if (attrCiEntityId == null && StringUtils.isNotBlank(attrCiEntityUuid)) {
                                    CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(attrCiEntityUuid);
                                    if (tmpVo != null) {
                                        //替换掉原来的ciEntityUuid为新的ciEntityId
                                        valueList.set(i, tmpVo.getCiEntityId());
                                    } else {
                                        //使用uuid寻找配置项
                                        CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(attrCiEntityUuid);
                                        if (uuidCiEntityVo == null) {
                                            throw new NewCiEntityNotFoundException(attrCiEntityUuid);
                                        } else {
                                            valueList.set(i, uuidCiEntityVo.getId());
                                        }
                                    }
                                } else if (attrCiEntityId != null) {
                                    valueList.set(i, attrCiEntityId);
                                } else {
                                    valueList.remove(i);
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setAttrEntityData(attrObj);
            //解析公共属性数据
            JSONObject globalAttrObj = ciEntityObj.getJSONObject("globalAttrEntityData");
            ciEntityTransactionVo.setGlobalAttrEntityData(globalAttrObj);

            // 解析关系数据
            JSONObject relObj = ciEntityObj.getJSONObject("relEntityData");
            //修正新配置项的uuid为id
            if (MapUtils.isNotEmpty(relObj)) {
                for (String key : relObj.keySet()) {
                    JSONObject obj = relObj.getJSONObject(key);
                    JSONArray relDataList = obj.getJSONArray("valueList");
                    if (CollectionUtils.isNotEmpty(relDataList)) {
                        for (int i = 0; i < relDataList.size(); i++) {
                            JSONObject relEntityObj = relDataList.getJSONObject(i);
                            Long ciEntityId = relEntityObj.getLong("ciEntityId");
                            String ciEntityUuid = relEntityObj.getString("ciEntityUuid");
                            if (ciEntityId == null && StringUtils.isNotBlank(ciEntityUuid)) {
                                CiEntityTransactionVo tmpVo = ciEntityTransactionMap.get(ciEntityUuid);
                                if (tmpVo != null) {
                                    relEntityObj.put("ciEntityId", tmpVo.getCiEntityId());
                                } else {
                                    CiEntityVo uuidCiEntityVo = ciEntityMapper.getCiEntityBaseInfoByUuid(ciEntityUuid);
                                    if (uuidCiEntityVo == null) {
                                        throw new NewCiEntityNotFoundException(relEntityObj.getString("ciEntityUuid"));
                                    } else {
                                        relEntityObj.put("ciEntityId", uuidCiEntityVo.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ciEntityTransactionVo.setRelEntityData(relObj);

            if (!ciEntityTransactionList.contains(ciEntityTransactionVo)) {
                ciEntityTransactionList.add(ciEntityTransactionVo);
            }
        }
        return ciEntityTransactionList;
    }
}
