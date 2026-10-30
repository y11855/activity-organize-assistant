package com.aiapp.activityassistant.service;

import com.aiapp.activityassistant.dto.ChunkInitDTO;
import com.aiapp.activityassistant.entity.ActivityFile;
import com.aiapp.activityassistant.vo.ActivityFileVO;
import com.aiapp.activityassistant.vo.ChunkInitVO;
import com.aiapp.activityassistant.vo.ChunkStatusVO;
import com.aiapp.activityassistant.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 活动资料：小文件直传 + 大文件分片上传（含断点续传与分片校验）
 */
public interface FileService {

    /** 小文件直传（带文件名称与说明）；activityId 为空时只落盘不建记录（如问卷封面） */
    FileUploadVO upload(Long activityId, String name, String description, MultipartFile file);

    /** 初始化分片上传会话，返回服务端规定的分片大小与总分片数 */
    ChunkInitVO initChunk(ChunkInitDTO dto);

    /** 查询已上传分片，供断点续传跳过已完成部分 */
    ChunkStatusVO chunkStatus(String uploadId);

    /** 上传单个分片：先校验 SHA-256，校验失败直接返回错误让客户端重传 */
    void uploadChunk(String uploadId, int index, String sha256, MultipartFile chunk);

    /** 合并分片（幂等：已合并时直接返回已有记录） */
    ActivityFileVO merge(String uploadId);

    List<ActivityFileVO> listByActivity(Long activityId);

    /** 打包下载用：直接返回实体，避免额外查询 */
    List<ActivityFile> listEntitiesByActivity(Long activityId);

    ActivityFile requireFile(Long id);

    void delete(Long id);
}
