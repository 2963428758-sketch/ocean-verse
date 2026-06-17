package com.oceanverse.common.controller;

import com.oceanverse.common.result.Result;
import com.oceanverse.common.utils.OssUtil;
import com.oceanverse.pojo.dto.UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final OssUtil ossUtil;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    @PostMapping("/image")
    public Result<UploadResult> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return Result.fail("仅支持 jpg/png/gif/webp 格式");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail("文件大小不能超过 10MB");
        }
        String url = ossUtil.upload(file);
        UploadResult data = UploadResult.of(url, file.getOriginalFilename(), file.getSize());
        return Result.success("上传成功", data);
    }
}
