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

package neatlogic.module.cmdb.service.transaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.crossover.ITransactionCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.GlobalAttrEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionDetailVo;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService, ITransactionCrossoverService {
    @Resource
    private TransactionMapper transactionMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    public List<TransactionVo> searchTransaction(TransactionVo transactionVo) {
        int rowNum = transactionMapper.searchTransactionCount(transactionVo);
        if (rowNum > 0) {
            transactionVo.setRowNum(rowNum);
            List<Long> transactionIdList = transactionMapper.searchTransactionId(transactionVo);
            if (CollectionUtils.isNotEmpty(transactionIdList)) {
                return transactionMapper.getTransactionByIdList(transactionIdList);
            }
        }
        return new ArrayList<>();
    }

    public List<TransactionDetailVo> getTransactionDetailList(List<TransactionVo> transactionList) {
        List<TransactionDetailVo> transactionDetailList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(transactionList)) {
            for (TransactionVo transactionVo : transactionList) {
                CiEntityTransactionVo ciEntityTransactionVo = transactionMapper.getCiEntityTransactionByTransactionId(transactionVo.getId());
                transactionDetailList.add(getTransactionDetail(transactionVo, ciEntityTransactionVo));
            }
        }
        return transactionDetailList;
    }

    public TransactionDetailVo getTransactionDetail(TransactionVo transactionVo, CiEntityTransactionVo ciEntityTransactionVo) {
        JSONArray dataList = new JSONArray();
        JSONObject oldCiEntityObj = null;
        boolean allowRecover = true;
        if (StringUtils.isNotBlank(ciEntityTransactionVo.getSnapshot())) {
            oldCiEntityObj = JSON.parseObject(ciEntityTransactionVo.getSnapshot());
        }
        if (!ciEntityTransactionVo.getAction().equals(TransactionActionType.DELETE.getValue())) {
            if (MapUtils.isNotEmpty(ciEntityTransactionVo.getAttrEntityData())) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(transactionVo.getCiId());

                JSONObject oldAttrEntityData = null;
                if (MapUtils.isNotEmpty(oldCiEntityObj)) {
                    oldAttrEntityData = oldCiEntityObj.getJSONObject("attrEntityData");
                }

                for (String key : ciEntityTransactionVo.getAttrEntityData().keySet()) {
                    JSONObject dataObj = new JSONObject();
                    JSONObject attrObj = ciEntityTransactionVo.getAttrEntityData().getJSONObject(key);
                    Long attrId = Long.parseLong(key.replace("attr_", ""));

                    Optional<AttrVo> filterAttr = attrList.stream().filter(attr -> attr.getId().equals(attrId)).findFirst();
                    if (filterAttr.isPresent()) {
                        AttrVo attrVo = filterAttr.get();
                        dataObj.put("newValue", buildAttrObj(attrVo, attrObj.getJSONArray("valueList")));
                        if (MapUtils.isNotEmpty(oldAttrEntityData) && oldAttrEntityData.containsKey(key)) {
                            dataObj.put("oldValue", buildAttrObj(attrVo, oldAttrEntityData.getJSONObject(key).getJSONArray("valueList")));
                        }
                        //如果整个newValueList都不存在表示原来使用的属性已经删除，这时候就不需要再显示新旧值了
                    } else {
                        //如果属性已删除，尝试使用snapshot数据还原原来的值
                        allowRecover = false;
                        if (MapUtils.isNotEmpty(oldAttrEntityData) && oldAttrEntityData.containsKey(key)) {
                            AttrVo attrVo = JSON.toJavaObject(oldAttrEntityData.getJSONObject(key), AttrVo.class);
                            dataObj.put("oldValue", buildAttrObj(attrVo, oldAttrEntityData.getJSONObject(key).getJSONArray("valueList")));
                        }
                        dataObj.put("action", "delattr");
                    }
                    dataObj.put("id", attrId);
                    dataObj.put("name", attrObj.getString("name"));
                    dataObj.put("label", attrObj.getString("label"));
                    dataObj.put("type", "attr");
                    dataList.add(dataObj);
                }
            }
            //处理全局属性
            if (MapUtils.isNotEmpty(ciEntityTransactionVo.getGlobalAttrEntityData())) {
                List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(new GlobalAttrVo());
                JSONObject oldGlobalAttrEntityData = null;
                if (MapUtils.isNotEmpty(oldCiEntityObj)) {
                    oldGlobalAttrEntityData = oldCiEntityObj.getJSONObject("globalAttrEntityData");
                }

                for (String key : ciEntityTransactionVo.getGlobalAttrEntityData().keySet()) {
                    JSONObject dataObj = new JSONObject();
                    JSONObject attrObj = ciEntityTransactionVo.getGlobalAttrEntityData().getJSONObject(key);
                    Long attrId = Long.parseLong(key.replace("global_", ""));

                    Optional<GlobalAttrVo> filterAttr = globalAttrList.stream().filter(attr -> attr.getId().equals(attrId)).findFirst();
                    if (filterAttr.isPresent()) {
                        GlobalAttrVo attr = filterAttr.get();
                        dataObj.put("name", attr.getName());
                        dataObj.put("label", attr.getLabel());
                        dataObj.put("newValue", attrObj.getJSONArray("valueList"));
                        if (MapUtils.isNotEmpty(oldGlobalAttrEntityData) && oldGlobalAttrEntityData.containsKey(key)) {
                            dataObj.put("oldValue", oldGlobalAttrEntityData.getJSONObject(key).getJSONArray("valueList"));
                        }
                    } else {
                        allowRecover = false;
                        if (MapUtils.isNotEmpty(oldGlobalAttrEntityData) && oldGlobalAttrEntityData.containsKey(key)) {
                            JSONObject globalAttrObj = oldGlobalAttrEntityData.getJSONObject(key);
                            dataObj.put("name", globalAttrObj.getString("name"));
                            dataObj.put("label", globalAttrObj.getString("label"));
                            dataObj.put("oldValue", globalAttrObj.getJSONArray("valueList"));
                        }
                        dataObj.put("action", "delglobalattr");
                    }
                    dataObj.put("id", attrId);
                    dataObj.put("type", "globalattr");
                    dataList.add(dataObj);
                }
            }

            if (MapUtils.isNotEmpty(ciEntityTransactionVo.getRelEntityData())) {
                List<RelVo> relList = relMapper.getRelByCiId(transactionVo.getCiId());
                JSONObject oldRelEntityData = null;
                if (MapUtils.isNotEmpty(oldCiEntityObj)) {
                    oldRelEntityData = oldCiEntityObj.getJSONObject("relEntityData");
                }
                for (String key : ciEntityTransactionVo.getRelEntityData().keySet()) {
                    JSONObject dataObj = new JSONObject();
                    JSONObject relObj = ciEntityTransactionVo.getRelEntityData().getJSONObject(key);
                    Long relId = Long.parseLong(key.split("_")[1]);
                    if (relList.stream().noneMatch(d -> d.getId().equals(relId))) {
                        allowRecover = false;
                    }
                    dataObj.put("id", relId);
                    dataObj.put("name", relObj.getString("name"));
                    dataObj.put("label", relObj.getString("label"));
                    dataObj.put("direction", relObj.getString("direction"));
                    dataObj.put("type", "rel");
                    JSONArray newValueList = new JSONArray();
                    //因为关系的修改只有insert和delete两种，显示对比时需要去掉删除的目标
                    for (int i = 0; i < relObj.getJSONArray("valueList").size(); i++) {
                        JSONObject valueObj = relObj.getJSONArray("valueList").getJSONObject(i);
                        if (!valueObj.containsKey("action") || !valueObj.getString("action").equals("delete")) {
                            //补充ciEntityName
                            CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(valueObj.getLong("ciEntityId"));
                            if (ciEntityVo != null) {
                                valueObj.put("ciEntityName", ciEntityVo.getName());
                            }
                            valueObj.put("action", "insert");
                        }
                        newValueList.add(valueObj);
                    }

                    if (MapUtils.isNotEmpty(oldRelEntityData) && oldRelEntityData.containsKey(key)) {
                        //补充ciEntityName
                        JSONArray oldValueList = oldRelEntityData.getJSONObject(key).getJSONArray("valueList");
                        for (int i = 0; i < oldValueList.size(); i++) {
                            JSONObject valueObj = oldValueList.getJSONObject(i);
                            /*CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(valueObj.getLong("ciEntityId"));
                            if (ciEntityVo != null) {
                                valueObj.put("ciEntityName", ciEntityVo.getName());
                            }*/
                            //补充原来的值
                            boolean isExists = false;
                            for (int j = 0; j < newValueList.size(); j++) {
                                JSONObject newV = newValueList.getJSONObject(j);
                                if (newV.getLong("ciEntityId").equals(valueObj.getLong("ciEntityId"))) {
                                    isExists = true;
                                    break;
                                }
                            }
                            if (!isExists) {
                                newValueList.add(valueObj);
                            }
                        }
                        dataObj.put("oldValue", oldValueList);
                    }
                    //清除删除信息
                    for (int j = newValueList.size() - 1; j >= 0; j--) {
                        JSONObject newV = newValueList.getJSONObject(j);
                        if (newV.containsKey("action") && newV.getString("action").equals("delete")) {
                            newValueList.remove(j);
                        }
                    }
                    dataObj.put("newValue", newValueList);
                    dataList.add(dataObj);
                }
            }
        } else {
            if (MapUtils.isNotEmpty(oldCiEntityObj)) {
                JSONObject attrData = oldCiEntityObj.getJSONObject("attrEntityData");
                if (MapUtils.isNotEmpty(attrData)) {
                    List<AttrVo> attrList = attrMapper.getAttrByCiId(transactionVo.getCiId());
                    for (String key : attrData.keySet()) {
                        JSONObject dataObj = new JSONObject();
                        //如果属性已删除，尝试使用snapshot数据还原原来的值
                        //JSONObject oldAttrEntityData = attrData.getJSONObject(key);
                        AttrVo attrVo = JSON.toJavaObject(attrData.getJSONObject(key), AttrVo.class);
                        if (attrList.stream().noneMatch(d -> d.getId().equals(attrVo.getId()))) {
                            allowRecover = false;
                        }
                        dataObj.put("oldValue", buildAttrObj(attrVo, attrData.getJSONObject(key).getJSONArray("valueList")));
                        dataObj.put("action", "delattr");
                        dataObj.put("id", attrVo.getId());
                        dataObj.put("name", attrVo.getName());
                        dataObj.put("label", attrVo.getLabel());
                        dataObj.put("type", "attr");
                        dataList.add(dataObj);
                    }
                }
                JSONObject globalAttrData = oldCiEntityObj.getJSONObject("globalAttrEntityData");
                if (MapUtils.isNotEmpty(globalAttrData)) {
                    List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(new GlobalAttrVo());

                    for (String key : globalAttrData.keySet()) {
                        JSONObject dataObj = new JSONObject();
                        GlobalAttrEntityVo attrVo = JSON.toJavaObject(globalAttrData.getJSONObject(key), GlobalAttrEntityVo.class);
                        if (globalAttrList.stream().noneMatch(d -> d.getId().equals(attrVo.getAttrId()))) {
                            allowRecover = false;
                        }
                        dataObj.put("oldValue", attrVo.getValueObjList());
                        dataObj.put("action", "delglobalattr");
                        dataObj.put("id", attrVo.getAttrId());
                        dataObj.put("name", attrVo.getAttrName());
                        dataObj.put("label", attrVo.getAttrLabel());
                        dataObj.put("type", "globalattr");
                        dataList.add(dataObj);
                    }
                }
                JSONObject relData = oldCiEntityObj.getJSONObject("relEntityData");
                if (MapUtils.isNotEmpty(relData)) {
                    List<RelVo> relList = relMapper.getRelByCiId(transactionVo.getCiId());
                    for (String key : relData.keySet()) {
                        JSONObject dataObj = new JSONObject();
                        JSONObject oldRelEntityData = relData.getJSONObject(key);
                        if (relList.stream().noneMatch(d -> d.getId().equals(oldRelEntityData.getLong("relId")))) {
                            allowRecover = false;
                        }
                        dataObj.put("oldValue", oldRelEntityData.getJSONArray("valueList"));
                        dataObj.put("id", oldRelEntityData.getLong("relId"));
                        dataObj.put("name", oldRelEntityData.getString("name"));
                        dataObj.put("label", oldRelEntityData.getString("label"));
                        dataObj.put("direction", oldRelEntityData.getString("direction"));
                        dataObj.put("type", "rel");
                        dataList.add(dataObj);
                    }
                }
            }

        }
        return new TransactionDetailVo(transactionVo, dataList, allowRecover);
    }

    private JSONObject buildAttrObj(AttrVo attrVo, JSONArray valueList) {
        JSONObject attrObj = new JSONObject();
        attrObj.put("type", attrVo.getType());
        attrObj.put("name", attrVo.getName());
        attrObj.put("label", attrVo.getLabel());
        attrObj.put("config", attrVo.getConfig(true));//克隆一个config对象，避免json序列化出错
        attrObj.put("targetCiId", attrVo.getTargetCiId());
        attrObj.put("valueList", valueList);
        attrObj.put("attrId", attrVo.getId());
        attrObj.put("actualValueList", AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList));
        return attrObj;
    }
}
