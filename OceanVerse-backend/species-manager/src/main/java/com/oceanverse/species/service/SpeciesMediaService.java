package com.oceanverse.species.service;

import com.oceanverse.pojo.entity.SpeciesMedia;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SpeciesMediaService {

    /**
     * 上传物种图片（支持批量）
     */
    List<SpeciesMedia> uploadMedia(Long speciesId, List<MultipartFile> files);

    /**
     * 获取物种的所有媒体资源
     */
    List<SpeciesMedia> getMediaBySpeciesId(Long speciesId);

    /**
     * 删除媒体资源（同时删除 OSS 文件）
     */
    void deleteMedia(Long mediaId);

    /**
     * 设置主图
     */
    void setPrimaryMedia(Long mediaId);
}
