package com.aiapp.activityassistant.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.aiapp.activityassistant.dto.ChunkInitDTO;
import com.aiapp.activityassistant.entity.Activity;
import com.aiapp.activityassistant.entity.ActivityFile;
import com.aiapp.activityassistant.entity.User;
import com.aiapp.activityassistant.mapper.ActivityFileMapper;
import com.aiapp.activityassistant.service.ActivityService;
import com.aiapp.activityassistant.service.FileService;
import com.aiapp.activityassistant.service.MessageService;
import com.aiapp.activityassistant.service.UserService;
import com.aiapp.activityassistant.vo.ActivityFileVO;
import com.aiapp.activityassistant.vo.ChunkInitVO;
import com.aiapp.activityassistant.vo.ChunkStatusVO;
import com.aiapp.activityassistant.vo.FileUploadVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 活动资料文件服务。
 *
 * <p>存储布局（都相对 app.upload-dir）：</p>
 * <pre>
 * activity-files/{yyyyMM}/{uuid}.{ext}   合并/直传后的最终文件
 * chunks/{uploadId}/meta.json            分片上传会话元数据
 * chunks/{uploadId}/{index}.part         单个分片
 * </pre>
 *
 * <p>分片上传流程：init 建会话 → 客户端查 status 跳过已传分片 → 逐片上传（服务端校验 SHA-256）
 * → merge 按序合并并整体校验。任一环节失败都只影响单个分片，客户端重传该片即可。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /** 文件类型白名单：可执行脚本等一律拒绝 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "ppt", "pptx", "doc", "docx", "xls", "xlsx", "csv", "pdf", "txt", "md", "zip",
            "jpg", "jpeg", "png", "webp", "gif", "mp4", "mov");

    private static final String FILE_DIR = "activity-files";
    private static final String CHUNK_DIR = "chunks";
    private static final String META_FILE = "meta.json";
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final ActivityFileMapper fileMapper;
    private final UserService userService;
    private final ActivityService activityService;
    private final MessageService messageService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.chunk-size:2097152}")
    private long chunkSize;

    @Value("${app.direct-max-size:5242880}")
    private long directMaxSize;

    @Value("${app.max-file-size:1073741824}")
    private long maxFileSize;

    @Override
    public FileUploadVO upload(Long activityId, String name, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的文件");
        }
        if (file.getSize() > directMaxSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "单个文件超过 " + readableSize(directMaxSize) + "，请使用分片上传");
        }
        if (activityId != null) {
            activityService.checkAccess(activityId);
        }
        // 文件名称可自定义；未填写时沿用原始文件名
        String fileOriginName = sanitizeName(file.getOriginalFilename());
        String displayName = (name == null || name.isBlank()) ? fileOriginName : sanitizeName(name);
        String ext = extOf(fileOriginName);
        checkExtension(ext);

        StoredFile stored;
        try (InputStream in = file.getInputStream()) {
            stored = store(ext, in);
        } catch (IOException e) {
            log.error("[资料上传] 保存失败", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败，请重试");
        }

        ActivityFile record = null;
        if (activityId != null) {
            record = new ActivityFile();
            record.setActivityId(activityId);
            record.setUploaderId(UserContext.getCurrentUserId());
            record.setName(displayName);
            record.setDescription(trimDescription(description));
            record.setExt(ext);
            record.setSize(stored.size);
            record.setSha256(stored.sha256);
            record.setStoragePath(stored.relativePath);
            fileMapper.insert(record);
            notifyMembers(record);
        }
        log.info("[资料上传] {} 直传成功，{}", displayName, stored.size);
        return new FileUploadVO(record == null ? null : record.getId(), displayName, stored.size,
                urlOf(stored.relativePath));
    }

    @Override
    public ChunkInitVO initChunk(ChunkInitDTO dto) {
        activityService.checkAccess(dto.getActivityId());
        String name = sanitizeName(dto.getFileName());
        String ext = extOf(name);
        checkExtension(ext);
        if (dto.getFileSize() > maxFileSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "文件超过 " + readableSize(maxFileSize) + "，无法上传");
        }

        long size = chunkSize;
        int totalChunks = (int) ((dto.getFileSize() + size - 1) / size);
        String uploadId = IdUtil.fastSimpleUUID();
        Path dir = chunkDir(uploadId);
        try {
            Files.createDirectories(dir);
            JSONObject meta = new JSONObject()
                    .set("activityId", dto.getActivityId())
                    .set("uploaderId", UserContext.getCurrentUserId())
                    .set("fileName", name)
                    .set("description", trimDescription(dto.getDescription()))
                    .set("fileSize", dto.getFileSize())
                    .set("chunkSize", size)
                    .set("totalChunks", totalChunks);
            writeMeta(uploadId, meta);
        } catch (IOException e) {
            log.error("[分片上传] 初始化失败", e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "初始化上传失败");
        }
        log.info("[分片上传] 创建会话 {}，共 {} 片，每片 {} 字节", uploadId, totalChunks, size);
        return new ChunkInitVO(uploadId, size, totalChunks);
    }

    @Override
    public ChunkStatusVO chunkStatus(String uploadId) {
        JSONObject meta = readMeta(uploadId);
        ChunkStatusVO vo = new ChunkStatusVO();
        vo.setUploadId(uploadId);
        vo.setFileName(meta.getStr("fileName"));
        vo.setFileSize(meta.getLong("fileSize"));
        vo.setChunkSize(meta.getLong("chunkSize"));
        vo.setTotalChunks(meta.getInt("totalChunks"));
        vo.setUploadedChunks(uploadedChunks(uploadId, meta.getInt("totalChunks")));
        vo.setMerged(meta.getLong("fileId") != null);
        return vo;
    }

    @Override
    public void uploadChunk(String uploadId, int index, String sha256, MultipartFile chunk) {
        JSONObject meta = readMeta(uploadId);
        requireSessionOwner(meta);
        long size = meta.getLong("chunkSize");
        int totalChunks = meta.getInt("totalChunks");
        if (index < 0 || index >= totalChunks) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分片序号越界");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分片内容为空");
        }
        long expectedSize = Math.min(size, meta.getLong("fileSize") - (long) index * size);
        byte[] bytes;
        try {
            bytes = chunk.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "读取分片失败");
        }
        if (bytes.length != expectedSize) {
            throw new BusinessException(ResultCode.CHUNK_CHECKSUM_MISMATCH,
                    "第 " + (index + 1) + " 片大小不符，需要重传");
        }
        // 分块损坏检测：服务端重新计算 SHA-256，与客户端声明的值比对
        if (sha256 == null || !sha256.equalsIgnoreCase(sha256Hex(bytes))) {
            throw new BusinessException(ResultCode.CHUNK_CHECKSUM_MISMATCH,
                    "第 " + (index + 1) + " 片校验失败，需要重传");
        }
        Path target = chunkDir(uploadId).resolve(index + ".part");
        try {
            Files.write(target, bytes);
        } catch (IOException e) {
            log.error("[分片上传] 写入分片失败 {}", index, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "分片保存失败，请重传该片");
        }
    }

    @Override
    public ActivityFileVO merge(String uploadId) {
        JSONObject meta = readMeta(uploadId);
        requireSessionOwner(meta);
        Long fileId = meta.getLong("fileId");
        if (fileId != null) {
            ActivityFile existing = fileMapper.selectById(fileId);
            if (existing != null) {
                return toVO(existing, uploaderName(existing.getUploaderId()), creatorIdOf(existing.getActivityId()));
            }
        }
        activityService.checkAccess(meta.getLong("activityId"));

        int totalChunks = meta.getInt("totalChunks");
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!Files.exists(chunkDir(uploadId).resolve(i + ".part"))) {
                missing.add(i + 1);
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ResultCode.CHUNK_NOT_COMPLETE,
                    "缺少分片 " + missing + "，请补传后再合并");
        }

        String name = meta.getStr("fileName");
        String ext = extOf(name);
        Path target = finalPath(ext);
        MessageDigest digest = sha256Digest();
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = new DigestOutputStream(Files.newOutputStream(target), digest)) {
                for (int i = 0; i < totalChunks; i++) {
                    try (InputStream in = Files.newInputStream(chunkDir(uploadId).resolve(i + ".part"))) {
                        in.transferTo(out);
                    }
                }
            }
        } catch (IOException e) {
            log.error("[分片上传] 合并失败 {}", uploadId, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "合并分片失败，请重试");
        }

        ActivityFile record = new ActivityFile();
        record.setActivityId(meta.getLong("activityId"));
        record.setUploaderId(meta.getLong("uploaderId"));
        record.setName(name);
        record.setDescription(trimDescription(meta.getStr("description")));
        record.setExt(ext);
        record.setSize(meta.getLong("fileSize"));
        record.setSha256(HexFormat.of().formatHex(digest.digest()));
        record.setStoragePath(relativeOf(target).toString());
        fileMapper.insert(record);
        notifyMembers(record);

        // 记录已合并：保留 meta（其中的 fileId 让重复合并幂等、让续传查询知道已完成），只清理分片文件
        meta.set("fileId", record.getId());
        try {
            writeMeta(uploadId, meta);
            deleteChunkParts(uploadId);
        } catch (IOException e) {
            log.warn("[分片上传] 清理分片文件失败 {}", uploadId, e);
        }
        log.info("[分片上传] 合并完成 {}，{}，SHA-256 {}", name, record.getSize(), record.getSha256());
        return toVO(record, uploaderName(record.getUploaderId()), creatorIdOf(record.getActivityId()));
    }

    @Override
    public List<ActivityFileVO> listByActivity(Long activityId) {
        activityService.checkAccess(activityId);
        List<ActivityFile> files = listEntitiesByActivity(activityId);
        if (files.isEmpty()) {
            return List.of();
        }
        Long creatorId = creatorIdOf(activityId);
        Map<Long, String> nameMap = files.stream().map(ActivityFile::getUploaderId).distinct()
                .collect(Collectors.toMap(id -> id, this::uploaderName));
        return files.stream().map(f -> toVO(f, nameMap.getOrDefault(f.getUploaderId(), ""), creatorId)).toList();
    }

    @Override
    public List<ActivityFile> listEntitiesByActivity(Long activityId) {
        activityService.checkAccess(activityId);
        return fileMapper.selectList(new LambdaQueryWrapper<ActivityFile>()
                .eq(ActivityFile::getActivityId, activityId)
                .orderByDesc(ActivityFile::getCreateTime));
    }

    @Override
    public ActivityFile requireFile(Long id) {
        ActivityFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        // 下载前校验：仅活动创建人或参与成员可访问
        activityService.checkAccess(file.getActivityId());
        return file;
    }

    @Override
    public void delete(Long id) {
        ActivityFile file = requireFile(id);
        Long currentUserId = UserContext.getCurrentUserId();
        Long creatorId = creatorIdOf(file.getActivityId());
        // 权限边界：创建者可删任意文件，普通成员只能删自己上传的
        if (!currentUserId.equals(creatorId) && !currentUserId.equals(file.getUploaderId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "普通成员只能删除自己上传的文件");
        }
        try {
            Files.deleteIfExists(root().resolve(file.getStoragePath()));
        } catch (IOException e) {
            log.warn("[资料删除] 物理文件删除失败 {}", file.getStoragePath(), e);
        }
        fileMapper.deleteById(id);
        log.info("[资料删除] fileId={}, 操作人={}", id, currentUserId);
    }

    /** 活动创建人 ID，用于计算文件删除权限 */
    private Long creatorIdOf(Long activityId) {
        Activity activity = activityService.getById(activityId);
        return activity == null ? null : activity.getCreatorId();
    }

    /** 分片会话只能由发起上传的本人操作，避免拿到 uploadId 后越权续传 */
    private void requireSessionOwner(JSONObject meta) {
        Long uploaderId = meta.getLong("uploaderId");
        if (uploaderId != null && !uploaderId.equals(UserContext.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该上传会话");
        }
    }

    /** 文件说明去除首尾空白并限制长度 */
    private String trimDescription(String description) {
        if (description == null) {
            return null;
        }
        String value = description.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    /** 上传后同步通知活动创建者与其他参与成员（上传人自己不重复通知） */
    private void notifyMembers(ActivityFile record) {
        Activity activity = activityService.getById(record.getActivityId());
        if (activity == null) {
            return;
        }
        Set<Long> targets = new HashSet<>();
        if (activity.getCreatorId() != null) {
            targets.add(activity.getCreatorId());
        }
        if (activity.getMemberIds() != null && !activity.getMemberIds().isBlank()) {
            try {
                targets.addAll(JSONUtil.toList(activity.getMemberIds(), Long.class));
            } catch (Exception e) {
                log.warn("[资料上传] 解析活动成员失败 activityId={}", activity.getId());
            }
        }
        targets.remove(record.getUploaderId());
        if (targets.isEmpty()) {
            return;
        }
        String uploader = uploaderName(record.getUploaderId());
        if (uploader.isEmpty()) {
            uploader = "参与成员";
        }
        String content = uploader + " 上传了活动文件《" + record.getName() + "》"
                + (record.getDescription() == null ? "" : "\n文件说明：" + record.getDescription())
                + "\n请前往活动详情「活动文件」查看或下载。";
        for (Long userId : targets) {
            try {
                messageService.send(userId, "活动文件上传提醒：" + activity.getTitle(), content, activity.getId());
            } catch (Exception e) {
                log.warn("[资料上传] 通知成员失败 userId={}", userId, e);
            }
        }
    }

    /* ===================== 存储与元数据 ===================== */

    private record StoredFile(String relativePath, long size, String sha256) {
    }

    /** 落盘并计算整体 SHA-256，返回相对路径/大小/摘要 */
    private StoredFile store(String ext, InputStream in) throws IOException {
        Path target = finalPath(ext);
        Files.createDirectories(target.getParent());
        MessageDigest digest = sha256Digest();
        try (OutputStream out = new DigestOutputStream(Files.newOutputStream(target), digest)) {
            in.transferTo(out);
        }
        return new StoredFile(relativeOf(target).toString(), Files.size(target),
                HexFormat.of().formatHex(digest.digest()));
    }

    private Path finalPath(String ext) {
        String fileName = IdUtil.fastSimpleUUID() + (ext.isEmpty() ? "" : "." + ext);
        return root().resolve(FILE_DIR).resolve(LocalDateTime.now().format(MONTH_FORMAT)).resolve(fileName);
    }

    private Path relativeOf(Path target) {
        return root().relativize(target);
    }

    private Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path chunkDir(String uploadId) {
        return root().resolve(CHUNK_DIR).resolve(uploadId);
    }

    private JSONObject readMeta(String uploadId) {
        Path meta = chunkDir(uploadId).resolve(META_FILE);
        if (!Files.exists(meta)) {
            throw new BusinessException(ResultCode.UPLOAD_SESSION_NOT_FOUND);
        }
        try {
            return JSONUtil.parseObj(Files.readString(meta, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.UPLOAD_SESSION_NOT_FOUND);
        }
    }

    private void writeMeta(String uploadId, JSONObject meta) throws IOException {
        Path dir = chunkDir(uploadId);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(META_FILE), JSONUtil.toJsonStr(meta), StandardCharsets.UTF_8);
    }

    private List<Integer> uploadedChunks(String uploadId, int totalChunks) {
        Path dir = chunkDir(uploadId);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".part"))
                    .map(n -> Integer.parseInt(n.substring(0, n.length() - 5)))
                    .filter(i -> i >= 0 && i < totalChunks)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** 合并完成后只删分片正文，保留 meta.json */
    private void deleteChunkParts(String uploadId) throws IOException {
        Path dir = chunkDir(uploadId);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path part : stream.filter(p -> p.getFileName().toString().endsWith(".part")).toList()) {
                Files.deleteIfExists(part);
            }
        }
    }

    /* ===================== 工具方法 ===================== */

    private ActivityFileVO toVO(ActivityFile file, String uploaderName, Long creatorId) {
        ActivityFileVO vo = new ActivityFileVO();
        BeanUtils.copyProperties(file, vo);
        vo.setUploaderName(uploaderName);
        vo.setUrl(urlOf(file.getStoragePath()));
        Long currentUserId = UserContext.getCurrentUserId();
        vo.setCanDelete(currentUserId.equals(creatorId) || currentUserId.equals(file.getUploaderId()));
        return vo;
    }

    /** 走 /uploads 静态映射，图片/PDF 可直接预览 */
    private String urlOf(String relativePath) {
        return "/api/uploads/" + relativePath.replace('\\', '/');
    }

    private String uploaderName(Long uploaderId) {
        if (uploaderId == null) {
            return "";
        }
        User user = userService.getById(uploaderId);
        return user == null || user.getNickname() == null ? "" : user.getNickname();
    }

    /** 去掉客户端可能带上的目录，避免路径穿越 */
    private String sanitizeName(String original) {
        if (original == null || original.isBlank()) {
            return "未命名文件";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private void checkExtension(String ext) {
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "不支持的文件类型：" + (ext.isEmpty() ? "无扩展名" : ext));
        }
    }

    private String readableSize(long bytes) {
        return bytes >= 1024 * 1024 * 1024
                ? String.format("%.0fGB", bytes / 1024.0 / 1024.0 / 1024.0)
                : String.format("%.0fMB", bytes / 1024.0 / 1024.0);
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 缺少 SHA-256 实现", e);
        }
    }

    private String sha256Hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256Digest().digest(bytes));
    }
}
