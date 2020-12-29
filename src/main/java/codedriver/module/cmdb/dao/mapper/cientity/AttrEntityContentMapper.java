package codedriver.module.cmdb.dao.mapper.cientity;

import org.apache.ibatis.annotations.Param;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 */
public interface AttrEntityContentMapper {
    public int checkAttrEntityHashIsExists(String hash);

    public String getAttrEntityContentByHash(String hash);

    public int insertAttrEntityContent(@Param("hash") String hash, @Param("valueHash") String valueHash,
        @Param("content") String content);

}
