package com.oceanverse.species.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.oceanverse.common.exception.BusinessException;
import com.oceanverse.common.utils.OssUtil;
import com.oceanverse.pojo.entity.Species;
import com.oceanverse.pojo.entity.SpeciesMedia;
import com.oceanverse.species.mapper.SpeciesMapper;
import com.oceanverse.species.mapper.SpeciesMediaMapper;
import com.oceanverse.species.service.SpeciesMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesMediaServiceImpl implements SpeciesMediaService {

    private final SpeciesMediaMapper speciesMediaMapper;
    private final SpeciesMapper speciesMapper;
    private final OssUtil ossUtil;

    @Override
    public List<SpeciesMedia> uploadMedia(Long speciesId, List<MultipartFile> files) {
        Species species = speciesMapper.selectById(speciesId);
        if (species == null) {
            throw BusinessException.notFound("物种");
        }

        // 检查该物种是否已有媒体，以确定 isPrimary
        long existingCount = speciesMediaMapper.selectCount(
                new LambdaQueryWrapper<SpeciesMedia>()
                        .eq(SpeciesMedia::getSpeciesId, speciesId)
        );

        List<SpeciesMedia> mediaList = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException("仅支持图片文件上传");
            }

            // 上传到 OSS
            String fileUrl = ossUtil.upload(file);

            // 保存媒体记录
            SpeciesMedia media = new SpeciesMedia();
            media.setSpeciesId(speciesId);
            media.setMediaType("IMAGE");
            media.setFileName(file.getOriginalFilename());
            media.setFileUrl(fileUrl);
            media.setFileSize(file.getSize());
            media.setStatus(1);
            media.setCreateTime(LocalDateTime.now());
            media.setUpdateTime(LocalDateTime.now());

            // 第一张上传的图片自动设为主图
            if (existingCount == 0 && mediaList.isEmpty()) {
                media.setIsPrimary(1);
            } else {
                media.setIsPrimary(0);
            }

            speciesMediaMapper.insert(media);
            mediaList.add(media);
            log.info("物种图片已上传: speciesId={}, fileName={}, url={}",
                    speciesId, file.getOriginalFilename(), fileUrl);
        }

        return mediaList;
    }

    @Override
    public List<SpeciesMedia> getMediaBySpeciesId(Long speciesId) {
        LambdaQueryWrapper<SpeciesMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpeciesMedia::getSpeciesId, speciesId)
                .eq(SpeciesMedia::getStatus, 1)
                .orderByDesc(SpeciesMedia::getIsPrimary)
                .orderByAsc(SpeciesMedia::getCreateTime);
        return speciesMediaMapper.selectList(wrapper);
    }

    @Override
    public void deleteMedia(Long mediaId) {
        SpeciesMedia media = speciesMediaMapper.selectById(mediaId);
        if (media == null) {
            throw BusinessException.notFound("媒体资源");
        }

        // 删除 OSS 文件
        try {
            String key = ossUtil.extractKey(media.getFileUrl());
            if (key != null) {
                ossUtil.delete(key);
            }
        } catch (Exception e) {
            log.warn("OSS 文件删除失败，但数据库记录将继续删除: {}", media.getFileUrl(), e);
        }

        // 删除数据库记录（逻辑删除）
        speciesMediaMapper.deleteById(mediaId);

        // 如果删除的是主图，将下一张设为主图
        if (media.getIsPrimary() != null && media.getIsPrimary() == 1) {
            LambdaQueryWrapper<SpeciesMedia> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SpeciesMedia::getSpeciesId, media.getSpeciesId())
                    .eq(SpeciesMedia::getStatus, 1)
                    .orderByAsc(SpeciesMedia::getCreateTime)
                    .last("LIMIT 1");
            SpeciesMedia nextMedia = speciesMediaMapper.selectOne(wrapper);
            if (nextMedia != null) {
                nextMedia.setIsPrimary(1);
                nextMedia.setUpdateTime(LocalDateTime.now());
                speciesMediaMapper.updateById(nextMedia);
            }
        }

        log.info("物种媒体已删除: mediaId={}, fileName={}", mediaId, media.getFileName());
    }

    @Override
    public void setPrimaryMedia(Long mediaId) {
        SpeciesMedia media = speciesMediaMapper.selectById(mediaId);
        if (media == null) {
            throw BusinessException.notFound("媒体资源");
        }

        // 取消当前物种的所有主图标记
        LambdaUpdateWrapper<SpeciesMedia> resetWrapper = new LambdaUpdateWrapper<>();
        resetWrapper.eq(SpeciesMedia::getSpeciesId, media.getSpeciesId())
                .set(SpeciesMedia::getIsPrimary, 0)
                .set(SpeciesMedia::getUpdateTime, LocalDateTime.now());
        speciesMediaMapper.update(null, resetWrapper);

        // 设置新的主图
        media.setIsPrimary(1);
        media.setUpdateTime(LocalDateTime.now());
        speciesMediaMapper.updateById(media);

        log.info("主图已更新: mediaId={}, speciesId={}", mediaId, media.getSpeciesId());
    }
}
