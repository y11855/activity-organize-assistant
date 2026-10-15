package com.aiapp.activityassistant.controller;

import cn.hutool.core.util.IdUtil;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传（本地磁盘存储）
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/files")
public class FileController {

    /** 允许的图片格式 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    /** 单张图片最大 5MB */
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    /** 存储目录（相对应用工作目录 backend/） */
    private static final Path UPLOAD_DIR = Paths.get("uploads");

    @Operation(summary = "上传图片（jpg/png/webp，≤5MB）")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "图片大小不能超过 5MB，当前文件 " + String.format("%.1f", file.getSize() / 1024.0 / 1024.0) + "MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "仅支持 JPG、PNG、WebP 格式的图片");
        }

        String filename = IdUtil.fastSimpleUUID() + "." + ext;
        try {
            Files.createDirectories(UPLOAD_DIR);
            file.transferTo(UPLOAD_DIR.resolve(filename).toAbsolutePath());
        } catch (IOException e) {
            log.error("[文件上传] 保存失败", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片保存失败，请重试");
        }

        // context-path 为 /api，前端经 Vite 代理 /api 访问
        String url = "/api/uploads/" + filename;
        log.info("[文件上传] 成功: {} -> {}", original, url);
        return Result.success(Map.of("url", url));
    }
}
