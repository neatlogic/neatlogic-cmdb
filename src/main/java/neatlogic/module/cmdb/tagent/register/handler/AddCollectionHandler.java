/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.tagent.register.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.result.InsertOneResult;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.tagent.register.core.AfterRegisterBase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/*
添加tagent数据进mongodb
 */
@Service
public class AddCollectionHandler extends AfterRegisterBase {
    private static final Logger logger = LoggerFactory.getLogger(AddCollectionHandler.class);

    @Resource
    private MongoTemplate mongoTemplate;


    /**
     * 注册后会送一条数据进mongodb，如果已存在不在写入，用于tagent注册时即使模型还没配置，也能在后续同步时写入配置库
     *
     * @param tagentVo tagent对象
     */
    @Override
    public void myExecute(TagentVo tagentVo) {
        if(logger.isDebugEnabled()) {
            logger.debug("AddCollectionHandler insert mongodb init! {}", JSON.toJSONString(tagentVo));
        }
        if (StringUtils.isNotBlank(tagentVo.getOsType()) && StringUtils.isNotBlank(tagentVo.getIp())) {
            Criteria criteria = new Criteria();
            criteria.andOperator(Criteria.where("MGMT_IP").is(tagentVo.getIp()));
            Query query = new Query(criteria);
            JSONObject oldData = mongoTemplate.findOne(query, JSONObject.class, "COLLECT_OS");
            Document dataObj = new Document();
            dataObj.put("_OBJ_CATEGORY", "OS");
            dataObj.put("_OBJ_TYPE", AddCiEntityHandler.capitalizeFirst(tagentVo.getOsType()));
            dataObj.put("OS_TYPE", AddCiEntityHandler.capitalizeFirst(tagentVo.getOsType()));
            dataObj.put("MGMT_IP", tagentVo.getIp());
            dataObj.put("CPU_ARCH", tagentVo.getOsbit());
            dataObj.put("HOSTNAME", tagentVo.getName());
            dataObj.put("VERSION", tagentVo.getOsVersion());
            if(logger.isDebugEnabled()) {
                logger.debug("AddCollectionHandler insert mongodb start! {}", JSON.toJSONString(dataObj));
            }
            if (oldData == null) {
                InsertOneResult result = mongoTemplate.getCollection("COLLECT_OS").insertOne(dataObj);
                if (result.getInsertedId() == null) {
                    logger.error("AddCiEntityHandler insert mongodb COLLECT_OS collection failed!");
                }else{
                    logger.debug("AddCiEntityHandler insert mongodb COLLECT_OS collection succeed!");
                }
            }else{
                if(logger.isDebugEnabled()) {
                    logger.debug("AddCollectionHandler COLLECT_OS collection no need to insert! {}", JSON.toJSONString(oldData));
                }
            }
        }
    }
}
