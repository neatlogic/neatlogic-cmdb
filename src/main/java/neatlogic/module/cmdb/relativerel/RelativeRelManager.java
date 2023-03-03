/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.relativerel;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.cmdb.dto.ci.RelativeRelItemVo;
import neatlogic.framework.cmdb.dto.ci.RelativeRelVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 更新级联关系
 */
@Service
public class RelativeRelManager {
    private static RelEntityMapper relEntityMapper;

    private static CiEntityService ciEntityService;

    private static RelMapper relMapper;


    @Autowired
    public RelativeRelManager(RelEntityMapper _relEntityMapper, RelMapper _relMapper, CiEntityService _ciEntityService) {
        relEntityMapper = _relEntityMapper;
        relMapper = _relMapper;
        ciEntityService = _ciEntityService;
    }


    /**
     * 删除级联关系，由于级联关系产生的数据不是很可控，暂时先不补充事务
     *
     * @param sourceRelEntityVo 原关系数据
     */
    public static void delete(RelEntityVo sourceRelEntityVo) {
        if (sourceRelEntityVo != null) {
            AfterTransactionJob<RelEntityVo> job = new AfterTransactionJob<>("RELATIVE-RELENTITY-DELETER");
            job.execute(sourceRelEntityVo, relEntityVo -> {
                List<RelEntityVo> relativeRelList = relEntityMapper.getRelentityBySourceRelEntityId(relEntityVo.getId());
                relEntityMapper.deleteRelEntityBySourceRelEntityId(relEntityVo.getId());
                rebuildRelEntityIndex(relativeRelList);
            });
        }
    }

    private static void rebuildRelEntityIndex(List<RelEntityVo> relEntityList) {
        if (CollectionUtils.isNotEmpty(relEntityList)) {
            Set<String> fromSet = relEntityList.stream().map(rel -> rel.getRelId() + "_" + rel.getFromCiEntityId()).collect(Collectors.toSet());
            Set<String> toSet = relEntityList.stream().map(rel -> rel.getRelId() + "_" + rel.getToCiEntityId()).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(fromSet)) {
                for (String relId : fromSet) {
                    ciEntityService.rebuildRelEntityIndex(RelDirectionType.FROM, Long.parseLong(relId.split("_")[0]), Long.parseLong(relId.split("_")[1]));
                }
            }
            if (CollectionUtils.isNotEmpty(toSet)) {
                for (String relId : toSet) {
                    ciEntityService.rebuildRelEntityIndex(RelDirectionType.TO, Long.parseLong(relId.split("_")[0]), Long.parseLong(relId.split("_")[1]));
                }
            }
        }
    }

    private static final Pattern p = Pattern.compile("(([<|>])([^<>]+))");


    public static void rebuild(Long relId) {
        CachedThreadPool.execute(new NeatLogicThread("RELATIVE-RELENTITY-REBUILD-" + relId, true) {
            @Override
            protected void execute() {
                List<RelativeRelVo> relativeRelList = relMapper.getRelativeRelByRelId(relId);
                if (CollectionUtils.isNotEmpty(relativeRelList)) {
                    RelEntityVo pRelEntityVo = new RelEntityVo();
                    pRelEntityVo.setRelId(relId);
                    pRelEntityVo.setPageSize(100);
                    pRelEntityVo.setCurrentPage(1);
                    List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByRelId(pRelEntityVo);
                    while (CollectionUtils.isNotEmpty(relEntityList)) {
                        for (RelEntityVo relEntityVo : relEntityList) {
                            buildSingleRelEntity(relativeRelList, relEntityVo);
                        }
                        pRelEntityVo.setCurrentPage(pRelEntityVo.getCurrentPage() + 1);
                        relEntityList = relEntityMapper.getRelEntityByRelId(pRelEntityVo);
                    }
                }
            }
        });

    }

    private static void buildSingleRelEntity(List<RelativeRelVo> relativeRelList, RelEntityVo relEntityVo) {
        for (RelativeRelVo relativeRelVo : relativeRelList) {
            List<Long> fromCiEntityIdList = new ArrayList<>();
            if (StringUtils.isNotBlank(relativeRelVo.getFromPath())) {
                Matcher fromMatch = p.matcher(relativeRelVo.getFromPath());
                List<RelativeRelItemVo> fromItemList = new ArrayList<>();
                while (fromMatch.find()) {
                    RelativeRelItemVo relativeRelItemVo = new RelativeRelItemVo();
                    String direction = fromMatch.group(2).equals(">") ? "to" : "from";
                    relativeRelItemVo.setRelId(Long.parseLong(fromMatch.group(3)));
                    relativeRelItemVo.setDirection(direction);
                    fromItemList.add(relativeRelItemVo);
                }
                fromCiEntityIdList = relEntityMapper.getCiEntityIdByRelativeRelPath(fromItemList, relEntityVo.getId(), "from");
            } else {
                fromCiEntityIdList.add(relEntityVo.getFromCiEntityId());
            }

            List<RelEntityVo> newRelEntityList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fromCiEntityIdList)) {
                List<Long> toCiEntityIdList = new ArrayList<>();
                if (StringUtils.isNotBlank(relativeRelVo.getToPath())) {
                    Matcher toMatch = p.matcher(relativeRelVo.getToPath());
                    List<RelativeRelItemVo> toItemList = new ArrayList<>();
                    while (toMatch.find()) {
                        RelativeRelItemVo relativeRelItemVo = new RelativeRelItemVo();
                        String direction = toMatch.group(2).equals(">") ? "to" : "from";
                        relativeRelItemVo.setRelId(Long.parseLong(toMatch.group(3)));
                        relativeRelItemVo.setDirection(direction);
                        toItemList.add(relativeRelItemVo);
                    }
                    toCiEntityIdList = relEntityMapper.getCiEntityIdByRelativeRelPath(toItemList, relEntityVo.getId(), "to");
                } else {
                    toCiEntityIdList.add(relEntityVo.getToCiEntityId());
                }
                if (CollectionUtils.isNotEmpty(toCiEntityIdList)) {
                    for (Long fromCiEntityId : fromCiEntityIdList) {
                        for (Long toCiEntityId : toCiEntityIdList) {
                            if (relEntityMapper.getRelEntityByFromCiEntityIdAndToCiEntityIdAndRelId(fromCiEntityId, toCiEntityId, relativeRelVo.getRelativeRelId()) == null) {
                                RelEntityVo newRelEntityVo = new RelEntityVo();
                                newRelEntityVo.setFromCiEntityId(fromCiEntityId);
                                newRelEntityVo.setToCiEntityId(toCiEntityId);
                                newRelEntityVo.setRelId(relativeRelVo.getRelativeRelId());
                                newRelEntityVo.setSourceRelEntityId(relEntityVo.getId());
                                newRelEntityVo.setRelativeRelHash(relativeRelVo.getHash());
                                newRelEntityList.add(newRelEntityVo);
                                relEntityMapper.insertRelEntity(newRelEntityVo);
                            }
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(newRelEntityList)) {
                rebuildRelEntityIndex(newRelEntityList);
            }
        }

    }

    public static void insert(RelEntityVo sourceRelEntityVo) {
        if (sourceRelEntityVo != null) {
            AfterTransactionJob<RelEntityVo> job = new AfterTransactionJob<>("RELATIVE-RELENTITY-APPENDER");
            job.execute(sourceRelEntityVo, relEntityVo -> {
                List<RelativeRelVo> relativeRelList = relMapper.getRelativeRelByRelId(relEntityVo.getRelId());
                if (CollectionUtils.isNotEmpty(relativeRelList)) {
                    buildSingleRelEntity(relativeRelList, relEntityVo);
                }
            });
        }
    }
}
