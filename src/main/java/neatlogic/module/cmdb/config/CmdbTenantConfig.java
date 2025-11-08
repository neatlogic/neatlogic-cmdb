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

package neatlogic.module.cmdb.config;

import neatlogic.framework.config.ITenantConfig;

public enum CmdbTenantConfig implements ITenantConfig {
    SYNC_BATCH_RUNNER_COUNT("sync.batch.runner.count", "5", "同步自动采集数据进CMDB时并发线程数，默认是5"),
    SYNC_MONGODB_CURSOR_MAX_RETRY("sync.mongodb.cursor.max.retry", "5", "同步自动采集数据进CDMB时遇到mongodb游标丢失后，进行自动重连的最大次数，默认是5");

    final String key;
    final String value;
    final String description;

    CmdbTenantConfig(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
