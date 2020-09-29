package codedriver.module.cmdb.dao.mapper.cientity;

import org.apache.ibatis.annotations.Param;

/**
 * 
 * @Author:chenqiwei
 * @Time:2020年9月27日
 * @ClassName: CiEntitySnapshotMapper
 * @Description: 快照
 */
public interface CiEntitySnapshotMapper {
    public String getSnapshotContentByHash(String hash);

    public int replaceSnapshotContent(@Param("hash") String hash, @Param("content") String content);
}
