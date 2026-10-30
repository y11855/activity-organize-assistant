package com.aiapp.activityassistant.controller;

import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.ChunkInitDTO;
import com.aiapp.activityassistant.entity.ActivityFile;
import com.aiapp.activityassistant.service.FileDownloadService;
import com.aiapp.activityassistant.service.FileService;
import com.aiapp.activityassistant.vo.ActivityFileVO;
import com.aiapp.activityassistant.vo.ChunkInitVO;
import com.aiapp.activityassistant.vo.ChunkStatusVO;
import com.aiapp.activityassistant.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件传输：小文件直传 / 大文件分片上传（含断点续传、分片校验）/ Range 下载 / 打包下载
 */
@Tag(name = "文件传输")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('activity:view')")
public class FileController {

    private final FileService fileService;
    private final FileDownloadService fileDownloadService;

    @Operation(summary = "上传单个小文件（≤5MB）；传活动资料时带 activityId，问卷封面等临时图片可不带")
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam(required = false) Long activityId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(activityId, name, description, file));
    }

    @Operation(summary = "初始化分片上传，返回 uploadId / 分片大小 / 总分片数")
    @PostMapping("/chunks")
    public Result<ChunkInitVO> initChunk(@Valid @RequestBody ChunkInitDTO dto) {
        return Result.success(fileService.initChunk(dto));
    }

    @Operation(summary = "查询已上传分片（断点续传：客户端只需补传缺失分片）")
    @GetMapping("/chunks/{uploadId}")
    public Result<ChunkStatusVO> chunkStatus(@PathVariable String uploadId) {
        return Result.success(fileService.chunkStatus(uploadId));
    }

    @Operation(summary = "上传单个分片（服务端校验 SHA-256，不通过则要求重传该片）")
    @PutMapping("/chunks/{uploadId}/{index}")
    public Result<Void> uploadChunk(@PathVariable String uploadId, @PathVariable int index,
            @RequestParam("sha256") String sha256,
            @RequestParam("chunk") MultipartFile chunk) {
        fileService.uploadChunk(uploadId, index, sha256, chunk);
        return Result.success();
    }

    @Operation(summary = "合并分片（服务端校验完整性并生成文件记录）")
    @PostMapping("/chunks/{uploadId}/merge")
    public Result<ActivityFileVO> merge(@PathVariable String uploadId) {
        return Result.success(fileService.merge(uploadId));
    }

    @Operation(summary = "活动资料列表")
    @GetMapping
    public Result<List<ActivityFileVO>> list(@RequestParam Long activityId) {
        return Result.success(fileService.listByActivity(activityId));
    }

    @Operation(summary = "下载文件（支持 Range 断点续传）")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        fileDownloadService.writeFile(fileService.requireFile(id), request, response);
    }

    @Operation(summary = "一键打包下载活动资料（边压缩边输出）")
    @GetMapping("/package")
    public void downloadPackage(@RequestParam Long activityId, HttpServletResponse response) throws IOException {
        List<ActivityFile> files = fileService.listEntitiesByActivity(activityId);
        if (files.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "暂无可打包的资料");
        }
        fileDownloadService.writeZip("活动资料.zip", files, response);
    }

    @Operation(summary = "删除资料文件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.success();
    }
}
